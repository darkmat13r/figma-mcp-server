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
 * Apply Text Style Tool
 *
 * ## Purpose
 * Applies an existing text style to a text node.
 * Ensures consistent typography across the design system.
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - styleId: string (required) - ID of the existing text style
 */
class ApplyTextStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.APPLY_TEXT_STYLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Applies an existing text style to a text node in Figma. " +
                    "Use this to maintain consistent typography across your design system. " +
                    "The text style must already exist in the document and the target must be a text node.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to apply style to (required)"
                    ),
                    ParamNames.STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the existing text style to apply (required). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\"). Use figma_get_local_text_styles to get available style IDs."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.STYLE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.APPLY_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.STYLE_TYPE, "TEXT")
            put(ParamNames.STYLE_ID, params.getRequiredString(ParamNames.STYLE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully applied text style to node: $nodeId"
    }
}
