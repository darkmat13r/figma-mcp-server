package com.figma.mcp.transport

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.tools.FigmaToolRegistry
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Figma MCP Server using Official Kotlin SDK
 *
 * ## Purpose
 * This class wraps the official Model Context Protocol Kotlin SDK to create
 * a Figma-specific MCP server that dynamically exposes all registered Figma tools.
 *
 * ## Architecture
 * ```
 * Claude Code (MCP Client)
 *     ↓ MCP Protocol (stdio/SSE)
 * McpServer (this class - using official SDK)
 *     ↓ Delegates to
 * FigmaToolRegistry (Strategy + Registry Pattern)
 *     ↓ Routes to individual tool implementations
 * IFigmaTool implementations (CreateFrameTool, CreateRectangleTool, etc.)
 *     ↓ WebSocket
 * Figma Plugin
 *     ↓ Figma Plugin API
 * Figma Document
 * ```
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Only responsible for MCP protocol integration and tool registration
 * - Tool execution logic delegated to FigmaToolRegistry
 * - Individual tool implementations in separate classes
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: Add new tools by registering them in FigmaToolRegistry
 * - Closed for modification: This class doesn't change when adding new tools
 * - No hardcoded tool list - all tools come from registry
 *
 * ### Dependency Inversion Principle (DIP)
 * - Depends on FigmaToolRegistry abstraction
 * - Individual tools injected via registry, not directly instantiated
 *
 * ## Design Improvements (vs Legacy Version)
 *
 * ### Before (Legacy - 5 hardcoded tools)
 * - 370 lines of code
 * - Hardcoded tool registration (87 lines per tool)
 * - Hardcoded tool handlers (switch statement)
 * - Violates Open-Closed Principle
 * - Adding tool = modify 3 sections of code
 *
 * ### After (New - Dynamic registry)
 * - ~150 lines of code (60% reduction)
 * - Dynamic tool registration from registry
 * - Single generic handler for all tools
 * - Follows Open-Closed Principle
 * - Adding tool = 0 lines in this file (register in DI module only)
 *
 * ## Usage Example
 * ```kotlin
 * // In DI configuration
 * val toolRegistry = FigmaToolRegistry(logger)
 * toolRegistry.registerAll(
 *     CreateFrameTool(...),
 *     CreateRectangleTool(...),
 *     // ... 10 more Category 1 tools
 * )
 * val mcpServer = McpServer(logger, toolRegistry)
 * mcpServer.start()
 * ```
 *
 * ## Tools Provided (Dynamic - from Registry)
 * The server exposes all tools registered in FigmaToolRegistry.
 * As of now (Category 1 complete):
 * 1. figma_create_frame - Create container frames
 * 2. figma_create_component - Create reusable components
 * 3. figma_create_instance - Create component instances
 * 4. figma_create_rectangle - Create rectangle shapes
 * 5. figma_create_ellipse - Create ellipse/circle shapes
 * 6. figma_create_text - Create text nodes
 * 7. figma_create_polygon - Create polygon shapes
 * 8. figma_create_star - Create star shapes
 * 9. figma_create_line - Create line shapes
 * 10. figma_create_group - Group nodes together
 * 11. figma_create_section - Create organizational sections
 * 12. figma_create_boolean_operation - Boolean operations (union, subtract, etc.)
 *
 * Plus 3 legacy tools (to be migrated to registry):
 * - figma_get_selection - Get current selection
 * - figma_set_properties - Set node properties
 * - figma_get_node_info - Get node information
 */
class McpServer(
    private val logger: ILogger,
    private val toolRegistry: FigmaToolRegistry
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
                    ),
                    prompts = ServerCapabilities.Prompts(
                        listChanged = true
                    )
                )
            )
        )

        // Register all Figma tools from the registry
        registerToolsFromRegistry()

        // Register prompts
        registerPrompts()

        logger.info(
            "McpServer initialized with official Kotlin SDK",
            "toolCount" to toolRegistry.getToolCount()
        )
    }

    /**
     * Register all Figma tools from the registry with the MCP server
     *
     * This method dynamically registers all tools from FigmaToolRegistry.
     * It follows the Open-Closed Principle - adding new tools requires
     * no modification to this code.
     *
     * ### How it works:
     * 1. Get all tool definitions from registry
     * 2. For each tool, register it with the MCP SDK
     * 3. Attach a generic handler that delegates to the registry
     *
     * ### Benefits:
     * - No hardcoded tool list
     * - No switch/if-else statements
     * - Adding tool = 0 lines changed in this file
     * - Single source of truth (FigmaToolRegistry)
     */
    private fun registerToolsFromRegistry() {
        val tools = toolRegistry.getAllTools()

        if (tools.isEmpty()) {
            logger.warn("No Figma tools registered in FigmaToolRegistry")
            return
        }

        tools.forEach { toolDefinition ->
            // Pass the complete input schema directly to the SDK
            // The schema already includes type, properties, and required fields
            val inputSchema = Tool.Input(
                properties = toolDefinition.inputSchema["properties"] as? kotlinx.serialization.json.JsonObject
                    ?: kotlinx.serialization.json.buildJsonObject { },
                required = (toolDefinition.inputSchema["required"] as? kotlinx.serialization.json.JsonArray)
                    ?.map { element ->
                        element.toString().removeSurrounding("\"")
                    }
                    ?: emptyList()
            )

            // Register the tool with MCP SDK
            server.addTool(
                name = toolDefinition.name,
                description = toolDefinition.description,
                inputSchema = inputSchema
            ) { request ->
                // Generic handler that delegates to the registry
                handleToolExecution(toolDefinition.name, request)
            }

            logger.debug(
                "Registered Figma tool with MCP SDK",
                "toolName" to toolDefinition.name
            )
        }

        logger.info(
            "Successfully registered all Figma tools from registry",
            "count" to tools.size,
            "toolNames" to toolRegistry.getAllToolNames().joinToString(", ")
        )
    }

    /**
     * Register system prompts with the MCP server
     *
     * Prompts are templates for common interactions that guide the AI's behavior.
     * This method registers a UI/UX professional designer prompt that instructs
     * the AI on best practices for Figma design work.
     */
    private fun registerPrompts() {
        try {
            // Register the UI/UX Professional Designer prompt
            server.addPrompt(
                name = "figma-designer",
                description = "Professional UI/UX designer persona for creating Figma designs following best practices"
            ) {
                GetPromptResult(
                    description = "You are a professional UI/UX designer working in Figma",
                    messages = listOf(
                        PromptMessage(
                            role = Role.user,
                            content = TextContent(
                                text = """You are a professional UI/UX designer working in Figma. Follow these principles:

## Design System First
- Before creating any design elements, analyze existing design pages and components
- Check if a design system exists; if not, create one following industry standards
- Always use design tokens and variables instead of static values
- Maintain consistency with existing design patterns and styles

## Layout & Organization
- Analyze existing pages to find the appropriate location for new features
- Calculate positions carefully to ensure components don't overlap
- Always use auto layout for responsive and maintainable designs
- Group related elements logically and name layers descriptively

## Assets & Resources
- Use Unsplash for placeholder images when needed
- Optimize image sizes and formats for performance
- Maintain a consistent visual language across all designs

## Workflow
1. **Analyze**: Review existing design pages and components
2. **Plan**: Identify the proper location and structure for new elements
3. **Reference**: Look at similar designs for inspiration and consistency
4. **Design**: Create components using variables, auto layout, and design system tokens
5. **Verify**: Ensure no overlaps, proper spacing, and adherence to design system

Remember: Quality, consistency, and attention to detail are paramount in professional UI/UX design."""
                            )
                        )
                    )
                )
            }

            logger.info("Successfully registered Figma designer prompt")
        } catch (e: Exception) {
            logger.error("Failed to register prompts", e)
        }
    }

    /**
     * Generic tool execution handler
     *
     * This single handler replaces 5+ hardcoded handlers in the legacy version.
     * It delegates execution to the FigmaToolRegistry which routes to the
     * appropriate IFigmaTool implementation.
     *
     * ### Benefits:
     * - DRY: No code duplication across handlers
     * - Extensible: Works for all current and future tools
     * - Consistent: Same error handling for all tools
     * - Maintainable: Single place to update handler logic
     *
     * @param toolName The name of the tool to execute
     * @param request The MCP SDK CallToolRequest
     * @return CallToolResult in SDK format
     */
    private suspend fun handleToolExecution(
        toolName: String,
        request: CallToolRequest
    ): CallToolResult {
        return try {
            logger.info(
                "→ MCP handleToolExecution() STARTED",
                "toolName" to toolName,
                "hasArguments" to (request.arguments.toString() != "{}")
            )

            // Execute the tool via registry
            logger.info("  Calling toolRegistry.executeTool()...", "toolName" to toolName)
            val result = toolRegistry.executeTool(
                toolName = toolName,
                arguments = request.arguments,
                validateArgs = true
            )
            logger.info(
                "  ✓ toolRegistry.executeTool() returned",
                "toolName" to toolName,
                "isError" to result.isError,
                "contentCount" to result.content.size
            )

            // Convert our CallToolResult to SDK's CallToolResult format
            logger.info("  Converting result to SDK format...", "toolName" to toolName)
            val sdkResult = CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }
                        is ToolContent.ImageContent -> {
                            // SDK supports images too, but we'll use text for now
                            TextContent(text = "[Image: ${content.mimeType}]")
                        }
                        is ToolContent.EmbeddedResource -> {
                            TextContent(text = "[Resource: ${content.resource.uri}]")
                        }
                    }
                },
                isError = result.isError
            )
            logger.info(
                "  ✓ Conversion complete, returning result",
                "toolName" to toolName,
                "sdkContentCount" to sdkResult.content.size
            )

            logger.info("← MCP handleToolExecution() RETURNING", "toolName" to toolName)
            sdkResult
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error(
                "✗ ERROR in handleToolExecution",
                e,
                "toolName" to toolName,
                "errorType" to e.javaClass.simpleName
            )
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Tool execution failed: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }

    /**
     * Get the MCP Server instance for HTTP/SSE transport
     * This allows the Ktor routing to handle MCP requests via SSE
     */
    fun getServer(): Server {
        return server
    }

    /**
     * Start the MCP server with SSE transport (HTTP-based)
     *
     * When running as an HTTP server with Ktor, the MCP protocol
     * is exposed via Server-Sent Events (SSE) endpoints.
     * The actual connection handling is done by Ktor routing.
     */
    suspend fun start() {
        try {
            logger.info(
                "MCP server initialized and ready for SSE transport",
                "registeredTools" to toolRegistry.getToolCount()
            )
            logger.info("Server will accept connections via HTTP/SSE endpoints")
            // When using SSE transport, the connection is handled by Ktor routing
            // No need to start stdio transport
        } catch (e: Exception) {
            logger.error("Failed to initialize MCP server", e)
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

    /**
     * Get statistics about registered tools (for monitoring/debugging)
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "toolCount" to toolRegistry.getToolCount(),
            "toolNames" to toolRegistry.getAllToolNames(),
            "serverVersion" to "1.0.0",
            "sdkVersion" to "official-kotlin-sdk"
        )
    }
}
