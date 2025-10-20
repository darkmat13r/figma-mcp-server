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
 * Apply Effect Style Tool
 *
 * ## Purpose
 * Applies an existing effect style (shadows, blurs) to a node.
 * Ensures consistent effects across the design system.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - styleId: string (required) - ID of the existing effect style
 */
class ApplyEffectStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.APPLY_EFFECT_STYLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Applies an existing effect style (shadows, blurs) to a Figma node. " +
                    "Use this to maintain consistency in visual effects across your design system. " +
                    "The effect style must already exist in the document.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply effect style to (required)"
                    ),
                    ParamNames.STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the existing effect style to apply (required)"
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
            put(ParamNames.STYLE_TYPE, "EFFECT")
            put(ParamNames.STYLE_ID, params.getRequiredString(ParamNames.STYLE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully applied effect style to node: $nodeId"
    }
}
