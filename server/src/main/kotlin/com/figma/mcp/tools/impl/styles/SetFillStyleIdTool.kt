package com.figma.mcp.tools.impl.styles

import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Set Fill Style ID Tool
 *
 * ## Purpose
 * Applies a paint style to a node's fill by setting the fillStyleId.
 * This allows nodes to use existing paint styles from the document.
 *
 * ## Parameters
 * - nodeId: string (required) - The ID of the node to apply the style to
 * - styleId: string (required) - The ID of the paint style to apply
 *
 * ## Returns
 * Success confirmation with node and style IDs
 */
class SetFillStyleIdTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_set_fill_style_id") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Applies a paint style to a node's fill by setting the fillStyleId. " +
                    "This links the node to an existing paint style, allowing centralized color management. " +
                    "Works with any node that supports fills (frames, shapes, text, etc.).",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "The ID of the node to apply the style to (required)"
                    ),
                    ParamNames.STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "The ID of the paint style to apply (required). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\"). Use figma_get_local_paint_styles to get available style IDs."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.STYLE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return "setFillStyleId"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val nodeId = params.getRequiredString(ParamNames.NODE_ID)
        val styleId = params.getRequiredString(ParamNames.STYLE_ID)

        return buildJsonObject {
            put(ParamNames.NODE_ID, nodeId)
            put(ParamNames.STYLE_ID, styleId)
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val styleId = params[ParamNames.STYLE_ID]?.jsonPrimitive?.contentOrNull ?: "unknown"

        return "Successfully applied fill style '$styleId' to node '$nodeId'"
    }
}
