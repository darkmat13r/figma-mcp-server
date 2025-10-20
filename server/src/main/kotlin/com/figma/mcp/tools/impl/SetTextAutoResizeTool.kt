package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Set Text Auto Resize Tool
 *
 * ## Purpose
 * Controls how text boxes automatically resize based on content.
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - textAutoResize: enum (required) - Auto-resize mode
 * - maxLines: number (optional) - Maximum number of lines (when truncation enabled)
 */
class SetTextAutoResizeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_TEXT_AUTO_RESIZE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Controls text box auto-sizing behavior in Figma. " +
                    "NONE: Fixed size (no auto-resize). " +
                    "HEIGHT: Auto-resize height to fit content, fixed width. " +
                    "WIDTH_AND_HEIGHT: Auto-resize both dimensions to fit content.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to configure (required)"
                    ),
                    ParamNames.TEXT_AUTO_RESIZE to mapOf(
                        "type" to "string",
                        "description" to "Auto-resize mode (required)",
                        "enum" to listOf("NONE", "HEIGHT", "WIDTH_AND_HEIGHT")
                    ),
                    ParamNames.MAX_LINES to mapOf(
                        "type" to "number",
                        "description" to "Maximum number of lines (optional, requires truncation to be enabled)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.TEXT_AUTO_RESIZE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_TEXT_AUTO_RESIZE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.TEXT_AUTO_RESIZE, params.getRequiredString(ParamNames.TEXT_AUTO_RESIZE))

            // Optional max lines
            params[ParamNames.MAX_LINES]?.jsonPrimitive?.intOrNull?.let {
                put(ParamNames.MAX_LINES, it)
            }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set text auto-resize for node: $nodeId"
    }
}
