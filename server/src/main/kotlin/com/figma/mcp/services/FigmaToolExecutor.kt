package com.figma.mcp.services

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.ToolContent
import kotlinx.serialization.json.*

/**
 * Figma Tool Executor Service
 *
 * ## Purpose
 * This service executes Figma-specific tools by communicating with the
 * Figma plugin running in the Figma desktop app.
 *
 * ## Architecture
 * ```
 * Claude Code (MCP Client)
 *     ↓ MCP Protocol (stdio/WebSocket)
 * Ktor MCP Server (this code)
 *     ↓ WebSocket
 * Figma Plugin (running in Figma app)
 *     ↓ Plugin API
 * Figma Document
 * ```
 *
 * ## How it Works
 * 1. Tool call comes in from Claude (e.g., "create rectangle")
 * 2. We extract and validate the arguments
 * 3. We send a command to the connected Figma plugin via WebSocket
 * 4. The plugin executes the command in Figma
 * 5. The plugin returns the result
 * 6. We format it as MCP tool result and return to Claude
 *
 * ## Implementation Note
 * For now, this is a simplified implementation. In production, you would:
 * - Maintain WebSocket connections to Figma plugins
 * - Queue requests and match responses
 * - Handle timeouts and errors
 * - Support multiple concurrent Figma documents
 */
class FigmaToolExecutor(
    private val logger: ILogger,
    private val figmaConnectionManager: FigmaConnectionManager
) {

    /**
     * Create a rectangle node in Figma
     *
     * Arguments:
     * - width: number (required)
     * - height: number (required)
     * - x: number (optional, defaults to 0)
     * - y: number (optional, defaults to 0)
     * - fillColor: string (optional, hex color)
     */
    suspend fun createRectangle(arguments: JsonObject): CallToolResult {
        return try {
            // Extract and validate arguments
            val width = arguments["width"]?.jsonPrimitive?.doubleOrNull
                ?: return errorResult("Missing required parameter: width")

            val height = arguments["height"]?.jsonPrimitive?.doubleOrNull
                ?: return errorResult("Missing required parameter: height")

            val x = arguments["x"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val y = arguments["y"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val fillColor = arguments["fillColor"]?.jsonPrimitive?.contentOrNull

            logger.info(
                "Creating rectangle",
                "width" to width,
                "height" to height,
                "x" to x,
                "y" to y,
                "fillColor" to fillColor
            )

            // Send command to Figma plugin
            val result = figmaConnectionManager.sendCommand(
                method = "createNode",
                params = buildJsonObject {
                    put("type", "RECTANGLE")
                    put("width", width)
                    put("height", height)
                    put("x", x)
                    put("y", y)
                    if (fillColor != null) {
                        put("fillColor", fillColor)
                    }
                }
            )

            // Return success result
            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        text = "Successfully created rectangle (${width}x${height}) at position ($x, $y)" +
                                if (fillColor != null) " with fill color $fillColor" else ""
                    )
                ),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("Failed to create rectangle", e)
            errorResult("Failed to create rectangle: ${e.message}")
        }
    }

    /**
     * Create a text node in Figma
     *
     * Arguments:
     * - text: string (required)
     * - fontSize: number (optional, defaults to 16)
     * - fontFamily: string (optional)
     * - color: string (optional, hex color)
     */
    suspend fun createText(arguments: JsonObject): CallToolResult {
        return try {
            val text = arguments["text"]?.jsonPrimitive?.contentOrNull
                ?: return errorResult("Missing required parameter: text")

            val fontSize = arguments["fontSize"]?.jsonPrimitive?.doubleOrNull ?: 16.0
            val fontFamily = arguments["fontFamily"]?.jsonPrimitive?.contentOrNull
            val color = arguments["color"]?.jsonPrimitive?.contentOrNull

            logger.info(
                "Creating text node",
                "text" to text,
                "fontSize" to fontSize
            )

            val result = figmaConnectionManager.sendCommand(
                method = "createNode",
                params = buildJsonObject {
                    put("type", "TEXT")
                    put("text", text)
                    put("fontSize", fontSize)
                    if (fontFamily != null) put("fontFamily", fontFamily)
                    if (color != null) put("color", color)
                }
            )

            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        text = "Successfully created text node with content: \"$text\""
                    )
                ),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("Failed to create text", e)
            errorResult("Failed to create text: ${e.message}")
        }
    }

    /**
     * Get information about the current selection in Figma
     */
    suspend fun getSelection(): CallToolResult {
        return try {
            logger.info("Getting current selection")

            val result = figmaConnectionManager.sendCommand(
                method = "getInfo",
                params = buildJsonObject {
                    put("type", "selection")
                }
            )

            // Parse the selection info from the result
            val selectionInfo = result?.toString() ?: "No selection information available"

            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        text = "Current selection:\n$selectionInfo"
                    )
                ),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("Failed to get selection", e)
            errorResult("Failed to get selection: ${e.message}")
        }
    }

    /**
     * Set properties on a Figma node
     *
     * Arguments:
     * - nodeId: string (required)
     * - properties: object (required) - properties to set
     */
    suspend fun setProperties(arguments: JsonObject): CallToolResult {
        return try {
            val nodeId = arguments["nodeId"]?.jsonPrimitive?.contentOrNull
                ?: return errorResult("Missing required parameter: nodeId")

            val properties = arguments["properties"]?.jsonObject
                ?: return errorResult("Missing required parameter: properties")

            logger.info(
                "Setting node properties",
                "nodeId" to nodeId,
                "properties" to properties.toString()
            )

            val result = figmaConnectionManager.sendCommand(
                method = "setProperties",
                params = buildJsonObject {
                    put("nodeId", nodeId)
                    put("properties", properties)
                }
            )

            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        text = "Successfully updated properties for node: $nodeId"
                    )
                ),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("Failed to set properties", e)
            errorResult("Failed to set properties: ${e.message}")
        }
    }

    /**
     * Get information about a specific Figma node
     *
     * Arguments:
     * - nodeId: string (required)
     */
    suspend fun getNodeInfo(arguments: JsonObject): CallToolResult {
        return try {
            val nodeId = arguments["nodeId"]?.jsonPrimitive?.contentOrNull
                ?: return errorResult("Missing required parameter: nodeId")

            logger.info("Getting node info", "nodeId" to nodeId)

            val result = figmaConnectionManager.sendCommand(
                method = "getInfo",
                params = buildJsonObject {
                    put("nodeId", nodeId)
                }
            )

            val nodeInfo = result?.toString() ?: "No information available for node: $nodeId"

            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        text = "Node information:\n$nodeInfo"
                    )
                ),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("Failed to get node info", e)
            errorResult("Failed to get node info: ${e.message}")
        }
    }

    /**
     * Helper function to create an error result
     */
    private fun errorResult(message: String): CallToolResult {
        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = true
        )
    }
}