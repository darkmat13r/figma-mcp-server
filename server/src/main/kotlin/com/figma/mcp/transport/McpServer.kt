package com.figma.mcp.transport

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaToolExecutor
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Figma MCP Server using Official Kotlin SDK
 *
 * ## Purpose
 * This class wraps the official Model Context Protocol Kotlin SDK to create
 * a Figma-specific MCP server that exposes 5 core tools for Figma manipulation.
 *
 * ## Architecture
 * ```
 * Claude Code (MCP Client)
 *     ↓ MCP Protocol (stdio)
 * McpServer (this class - using official SDK)
 *     ↓ Delegates to
 * FigmaToolExecutor
 *     ↓ WebSocket
 * Figma Plugin
 *     ↓ Figma Plugin API
 * Figma Document
 * ```
 *
 * ## Tools Provided
 * 1. figma_create_rectangle - Create rectangle nodes
 * 2. figma_create_text - Create text nodes
 * 3. figma_get_selection - Get current selection
 * 4. figma_set_properties - Set node properties
 * 5. figma_get_node_info - Get node information
 */
class McpServer(
    private val logger: ILogger,
    private val figmaToolExecutor: FigmaToolExecutor
) {
    private val server: Server
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Create the MCP server using the official SDK
        server = Server(
            serverInfo = Implementation(
                name = "figma-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(
                        listChanged = true
                    )
                )
            )
        )

        // Register all Figma tools
        registerTools()

        logger.info("McpServer initialized with official Kotlin SDK")
    }

    /**
     * Register all Figma tools with the MCP server
     */
    private fun registerTools() {
        // Tool 1: Create Rectangle
        server.addTool(
            name = "figma_create_rectangle",
            description = "Create a rectangle node in Figma with specified dimensions, position, and optional fill color",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("width", buildJsonObject {
                        put("type", "number")
                        put("description", "Width of the rectangle in pixels")
                    })
                    put("height", buildJsonObject {
                        put("type", "number")
                        put("description", "Height of the rectangle in pixels")
                    })
                    put("x", buildJsonObject {
                        put("type", "number")
                        put("description", "X position of the rectangle (defaults to 0)")
                    })
                    put("y", buildJsonObject {
                        put("type", "number")
                        put("description", "Y position of the rectangle (defaults to 0)")
                    })
                    put("fillColor", buildJsonObject {
                        put("type", "string")
                        put("description", "Fill color as hex code (e.g., '#FF0000')")
                    })
                },
                required = listOf("width", "height")
            )
        ) { request ->
            handleCreateRectangle(request)
        }

        // Tool 2: Create Text
        server.addTool(
            name = "figma_create_text",
            description = "Create a text node in Figma with specified content, font size, font family, and color",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("text", buildJsonObject {
                        put("type", "string")
                        put("description", "The text content to display")
                    })
                    put("fontSize", buildJsonObject {
                        put("type", "number")
                        put("description", "Font size in pixels (defaults to 16)")
                    })
                    put("fontFamily", buildJsonObject {
                        put("type", "string")
                        put("description", "Font family name (e.g., 'Inter', 'Roboto')")
                    })
                    put("color", buildJsonObject {
                        put("type", "string")
                        put("description", "Text color as hex code (e.g., '#000000')")
                    })
                },
                required = listOf("text")
            )
        ) { request ->
            handleCreateText(request)
        }

        // Tool 3: Get Selection
        server.addTool(
            name = "figma_get_selection",
            description = "Get information about the currently selected nodes in Figma",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    // No parameters needed
                }
            )
        ) { request ->
            handleGetSelection(request)
        }

        // Tool 4: Set Properties
        server.addTool(
            name = "figma_set_properties",
            description = "Set properties on a specific Figma node (e.g., size, position, color, name)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("nodeId", buildJsonObject {
                        put("type", "string")
                        put("description", "The ID of the node to modify")
                    })
                    put("properties", buildJsonObject {
                        put("type", "object")
                        put(
                            "description",
                            "Object containing properties to set (e.g., {\"width\": 100, \"name\": \"My Node\"})"
                        )
                    })
                },
                required = listOf("nodeId", "properties")
            )
        ) { request ->
            handleSetProperties(request)
        }

        // Tool 5: Get Node Info
        server.addTool(
            name = "figma_get_node_info",
            description = "Get detailed information about a specific Figma node by its ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("nodeId", buildJsonObject {
                        put("type", "string")
                        put("description", "The ID of the node to retrieve information for")
                    })
                },
                required = listOf("nodeId")
            )
        ) { request ->
            handleGetNodeInfo(request)
        }

        logger.info("Registered 5 Figma tools with MCP server")
    }

    /**
     * Start the MCP server with stdio transport
     */
    suspend fun start() {
        try {
            logger.info("Starting MCP server with stdio transport...")

            // Create stdio transport with kotlinx.io Source and Sink
            val transport = StdioServerTransport(
                inputStream = System.`in`.asSource().buffered(),
                outputStream = System.out.asSink().buffered()
            )

            // Connect the server to the transport
            server.connect(transport)

            logger.info("MCP server started successfully on stdio")
        } catch (e: Exception) {
            logger.error("Failed to start MCP server", e)
            throw e
        }
    }

    /**
     * Stop the MCP server
     */
    fun stop() {
        logger.info("Stopping MCP server...")
        // The SDK handles cleanup when the transport is closed
    }

    // ========================================
    // Tool Handlers
    // ========================================

    private suspend fun handleCreateRectangle(request: CallToolRequest): CallToolResult {
        return try {
            // Extract arguments from the request
            // According to SDK examples, arguments is a direct property
            val argsElement = request.arguments

            // Convert SDK CallToolResult to our custom one
            val result = figmaToolExecutor.createRectangle(argsElement)

            // Map to SDK's CallToolResult format
            CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }

                        else -> TextContent(text = "Unsupported content type")
                    }
                },
                isError = result.isError
            )
        } catch (e: Exception) {
            logger.error("Error in handleCreateRectangle", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error: ${e.message}")),
                isError = true
            )
        }
    }

    private suspend fun handleCreateText(request: CallToolRequest): CallToolResult {
        return try {
            val argsElement = request.arguments
            val result = figmaToolExecutor.createText(argsElement)

            CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }

                        else -> TextContent(text = "Unsupported content type")
                    }
                },
                isError = result.isError
            )
        } catch (e: Exception) {
            logger.error("Error in handleCreateText", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error: ${e.message}")),
                isError = true
            )
        }
    }

    private suspend fun handleGetSelection(request: CallToolRequest): CallToolResult {
        return try {
            val result = figmaToolExecutor.getSelection()

            CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }

                        else -> TextContent(text = "Unsupported content type")
                    }
                },
                isError = result.isError
            )
        } catch (e: Exception) {
            logger.error("Error in handleGetSelection", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error: ${e.message}")),
                isError = true
            )
        }
    }

    private suspend fun handleSetProperties(request: CallToolRequest): CallToolResult {
        return try {
            val argsElement = request.arguments
            val result = figmaToolExecutor.setProperties(argsElement)

            CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }

                        else -> TextContent(text = "Unsupported content type")
                    }
                },
                isError = result.isError
            )
        } catch (e: Exception) {
            logger.error("Error in handleSetProperties", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error: ${e.message}")),
                isError = true
            )
        }
    }

    private suspend fun handleGetNodeInfo(request: CallToolRequest): CallToolResult {
        return try {
            val argsElement = request.arguments
            val result = figmaToolExecutor.getNodeInfo(argsElement)

            CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }

                        else -> TextContent(text = "Unsupported content type")
                    }
                },
                isError = result.isError
            )
        } catch (e: Exception) {
            logger.error("Error in handleGetNodeInfo", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error: ${e.message}")),
                isError = true
            )
        }
    }
}