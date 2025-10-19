package com.figma.mcp.commands.mcp

import com.figma.mcp.core.*
import com.figma.mcp.protocol.*
import com.figma.mcp.services.FigmaToolExecutor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * MCP Call Tool Command Handler
 *
 * ## Purpose
 * Handles the `tools/call` method which executes a specific tool.
 * This is called when Claude Code wants to use one of our Figma tools.
 *
 * ## Flow
 * 1. Claude decides to use a tool (e.g., "figma_create_rectangle")
 * 2. Claude sends tools/call with tool name and arguments
 * 3. We validate the tool exists and arguments are correct
 * 4. We execute the tool (send command to Figma plugin)
 * 5. We return the result to Claude
 *
 * ## Example Request
 * ```json
 * {
 *   "method": "tools/call",
 *   "params": {
 *     "name": "figma_create_rectangle",
 *     "arguments": {
 *       "width": 200,
 *       "height": 100,
 *       "fillColor": "#FF0000"
 *     }
 *   }
 * }
 * ```
 *
 * ## Example Response
 * ```json
 * {
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "Successfully created rectangle with dimensions 200x100"
 *     }
 *   ],
 *   "isError": false
 * }
 * ```
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/server/tools/
 */
class CallToolCommand(
    private val logger: ILogger,
    private val json: Json,
    private val figmaToolExecutor: FigmaToolExecutor
) : ICommandHandler {

    override val commandName: String = "tools/call"

    /**
     * Execute a Figma tool
     */
    override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            // Parse the call tool parameters
            val params = context.params?.let {
                json.decodeFromJsonElement(CallToolParams.serializer(), it)
            } ?: return CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INVALID_PARAMS,
                    message = "Missing tool call parameters"
                )
            )

            logger.info(
                "Executing tool",
                "tool" to params.name,
                "clientId" to context.clientId,
                "hasArguments" to (params.arguments != null)
            )

            // Route to the appropriate tool executor
            val result = when (params.name) {
                "figma_create_rectangle" -> figmaToolExecutor.createRectangle(params.arguments ?: JsonObject(emptyMap()))
                "figma_create_text" -> figmaToolExecutor.createText(params.arguments ?: JsonObject(emptyMap()))
                "figma_get_selection" -> figmaToolExecutor.getSelection()
                "figma_set_properties" -> figmaToolExecutor.setProperties(params.arguments ?: JsonObject(emptyMap()))
                "figma_get_node_info" -> figmaToolExecutor.getNodeInfo(params.arguments ?: JsonObject(emptyMap()))
                else -> CallToolResult(
                    content = listOf(
                        ToolContent.TextContent(
                            text = "Unknown tool: ${params.name}"
                        )
                    ),
                    isError = true
                )
            }

            logger.info(
                "Tool execution completed",
                "tool" to params.name,
                "isError" to result.isError
            )

            CommandResult(
                success = true,
                data = json.encodeToJsonElement(CallToolResult.serializer(), result)
            )
        } catch (e: Exception) {
            logger.error("Tool execution failed", e)
            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Tool execution failed: ${e.message}"
                )
            )
        }
    }

    override fun validate(params: kotlinx.serialization.json.JsonElement?): String? {
        if (params == null) {
            return "Missing parameters"
        }

        try {
            val callParams = json.decodeFromJsonElement(CallToolParams.serializer(), params)
            if (callParams.name.isBlank()) {
                return "Tool name is required"
            }
        } catch (e: Exception) {
            return "Invalid parameters: ${e.message}"
        }

        return null
    }
}
