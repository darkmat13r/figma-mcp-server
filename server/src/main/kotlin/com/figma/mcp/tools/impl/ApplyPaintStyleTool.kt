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
 * Apply Paint Style Tool
 *
 * ## Purpose
 * Applies an existing fill or stroke style to a node.
 * Enables consistent styling across design systems.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - styleType: "FILL" | "STROKE" (required) - Type of paint style to apply
 * - styleId: string (required) - ID of the existing style
 */
class ApplyPaintStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.APPLY_PAINT_STYLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Applies an existing fill or stroke style to a Figma node. " +
                    "Use this to maintain consistency across your design system by reusing " +
                    "predefined color styles. Requires the style to already exist in the document.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply style to (required)"
                    ),
                    ParamNames.STYLE_TYPE to mapOf(
                        "type" to "string",
                        "enum" to listOf("FILL", "STROKE"),
                        "description" to "Type of paint style to apply: FILL or STROKE (required)"
                    ),
                    ParamNames.STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the existing paint style to apply (required). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\"). Use figma_get_local_paint_styles to get available style IDs."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.STYLE_TYPE, ParamNames.STYLE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.APPLY_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.STYLE_TYPE, params.getRequiredString(ParamNames.STYLE_TYPE))
            put(ParamNames.STYLE_ID, params.getRequiredString(ParamNames.STYLE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val styleType = params[ParamNames.STYLE_TYPE]?.jsonPrimitive?.contentOrNull
        return "Successfully applied $styleType style to node: $nodeId"
    }
}
