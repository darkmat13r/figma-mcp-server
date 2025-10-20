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
 * Set Text Alignment Tool
 *
 * ## Purpose
 * Controls text alignment both horizontally and vertically within a text node.
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - textAlignHorizontal: enum (optional) - Horizontal alignment
 * - textAlignVertical: enum (optional) - Vertical alignment
 */
class SetTextAlignmentTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_TEXT_ALIGNMENT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Sets text alignment on a text node in Figma. " +
                    "Controls both horizontal (left, center, right, justified) and " +
                    "vertical (top, center, bottom) alignment.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to align (required)"
                    ),
                    ParamNames.TEXT_ALIGN_HORIZONTAL to mapOf(
                        "type" to "string",
                        "description" to "Horizontal text alignment (optional)",
                        "enum" to listOf("LEFT", "CENTER", "RIGHT", "JUSTIFIED")
                    ),
                    ParamNames.TEXT_ALIGN_VERTICAL to mapOf(
                        "type" to "string",
                        "description" to "Vertical text alignment (optional)",
                        "enum" to listOf("TOP", "CENTER", "BOTTOM")
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_TEXT_ALIGNMENT
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))

            // Optional horizontal alignment
            params.getStringOrNull(ParamNames.TEXT_ALIGN_HORIZONTAL)?.let {
                put(ParamNames.TEXT_ALIGN_HORIZONTAL, it)
            }

            // Optional vertical alignment
            params.getStringOrNull(ParamNames.TEXT_ALIGN_VERTICAL)?.let {
                put(ParamNames.TEXT_ALIGN_VERTICAL, it)
            }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set text alignment for node: $nodeId"
    }
}
