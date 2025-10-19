package com.figma.mcp.commands.mcp

import com.figma.mcp.core.*
import com.figma.mcp.protocol.*
import kotlinx.serialization.json.Json

/**
 * MCP List Tools Command Handler
 *
 * ## Purpose
 * Handles the `tools/list` method which returns all available tools
 * that Claude Code can call.
 *
 * ## What are MCP Tools?
 * Tools are functions that the AI assistant (Claude) can invoke.
 * Each tool has:
 * - A unique name
 * - A description (tells Claude what the tool does)
 * - An input schema (JSON Schema defining parameters)
 *
 * ## Example Tool Definition
 * ```json
 * {
 *   "name": "create_rectangle",
 *   "description": "Creates a rectangle node in Figma",
 *   "inputSchema": {
 *     "type": "object",
 *     "properties": {
 *       "width": {"type": "number", "description": "Width in pixels"},
 *       "height": {"type": "number", "description": "Height in pixels"},
 *       "color": {"type": "string", "description": "Fill color (hex)"}
 *     },
 *     "required": ["width", "height"]
 *   }
 * }
 * ```
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/server/tools/
 */
class ListToolsCommand(
    private val logger: ILogger,
    private val json: Json
) : ICommandHandler {

    override val commandName: String = "tools/list"

    /**
     * Return the list of available Figma tools
     */
    override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            logger.info("Listing MCP tools", "clientId" to context.clientId)

            // Define all available Figma tools
            val tools = listOf(
                // Tool 1: Create a rectangle node
                Tool(
                    name = "figma_create_rectangle",
                    description = "Creates a rectangle node in the current Figma selection. " +
                            "You can specify dimensions and fill color.",
                    inputSchema = JSONSchema.createObjectSchema(
                        properties = mapOf(
                            "width" to mapOf(
                                "type" to "number",
                                "description" to "Width of the rectangle in pixels"
                            ),
                            "height" to mapOf(
                                "type" to "number",
                                "description" to "Height of the rectangle in pixels"
                            ),
                            "x" to mapOf(
                                "type" to "number",
                                "description" to "X position (optional, defaults to 0)"
                            ),
                            "y" to mapOf(
                                "type" to "number",
                                "description" to "Y position (optional, defaults to 0)"
                            ),
                            "fillColor" to mapOf(
                                "type" to "string",
                                "description" to "Fill color as hex code (e.g., '#FF0000') or CSS color name"
                            )
                        ),
                        required = listOf("width", "height")
                    )
                ),

                // Tool 2: Create a text node
                Tool(
                    name = "figma_create_text",
                    description = "Creates a text node in Figma with specified content and styling.",
                    inputSchema = JSONSchema.createObjectSchema(
                        properties = mapOf(
                            "text" to mapOf(
                                "type" to "string",
                                "description" to "The text content"
                            ),
                            "fontSize" to mapOf(
                                "type" to "number",
                                "description" to "Font size in pixels (optional, defaults to 16)"
                            ),
                            "fontFamily" to mapOf(
                                "type" to "string",
                                "description" to "Font family name (optional)"
                            ),
                            "color" to mapOf(
                                "type" to "string",
                                "description" to "Text color as hex code (optional)"
                            )
                        ),
                        required = listOf("text")
                    )
                ),

                // Tool 3: Get current selection info
                Tool(
                    name = "figma_get_selection",
                    description = "Gets information about the currently selected nodes in Figma. " +
                            "Returns node IDs, types, names, and properties.",
                    inputSchema = JSONSchema.createObjectSchema(
                        properties = emptyMap(), // No parameters needed
                        required = emptyList()
                    )
                ),

                // Tool 4: Set node properties
                Tool(
                    name = "figma_set_properties",
                    description = "Sets properties on a Figma node (size, position, color, etc.)",
                    inputSchema = JSONSchema.createObjectSchema(
                        properties = mapOf(
                            "nodeId" to mapOf(
                                "type" to "string",
                                "description" to "The ID of the node to modify"
                            ),
                            "properties" to mapOf(
                                "type" to "object",
                                "description" to "Object containing properties to set (e.g., {width: 100, height: 200})"
                            )
                        ),
                        required = listOf("nodeId", "properties")
                    )
                ),

                // Tool 5: Get node information
                Tool(
                    name = "figma_get_node_info",
                    description = "Gets detailed information about a specific Figma node by ID.",
                    inputSchema = JSONSchema.createObjectSchema(
                        properties = mapOf(
                            "nodeId" to mapOf(
                                "type" to "string",
                                "description" to "The ID of the node to query"
                            )
                        ),
                        required = listOf("nodeId")
                    )
                )
            )

            val result = ListToolsResult(
                tools = tools,
                nextCursor = null // We return all tools at once (no pagination)
            )

            logger.info("Returned ${tools.size} tools")

            CommandResult(
                success = true,
                data = json.encodeToJsonElement(ListToolsResult.serializer(), result)
            )
        } catch (e: Exception) {
            logger.error("Failed to list tools", e)
            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Failed to list tools: ${e.message}"
                )
            )
        }
    }

    override fun validate(params: kotlinx.serialization.json.JsonElement?): String? {
        // tools/list doesn't require parameters
        return null
    }
}
