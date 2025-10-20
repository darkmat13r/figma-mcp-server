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
 * Set Opacity Tool
 *
 * ## Purpose
 * Controls node opacity and blend mode.
 * Used for transparency and layer blending effects.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - opacity: number (required, 0-1) - Opacity value (0=transparent, 1=opaque)
 * - blendMode: string (optional) - Blend mode for layer compositing
 */
class SetOpacityTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_OPACITY) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Sets the opacity (transparency) and blend mode of a Figma node. " +
                    "Opacity ranges from 0 (fully transparent) to 1 (fully opaque). " +
                    "Blend mode controls how the layer composites with layers below.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to set opacity for (required)"
                    ),
                    ParamNames.OPACITY to mapOf(
                        "type" to "number",
                        "description" to "Opacity value from 0 (transparent) to 1 (opaque) (required)",
                        "minimum" to 0.0,
                        "maximum" to 1.0
                    ),
                    ParamNames.BLEND_MODE to mapOf(
                        "type" to "string",
                        "description" to "Blend mode for compositing (optional). " +
                                "Common values: NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN, etc."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.OPACITY)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.OPACITY, params.getRequiredDouble(ParamNames.OPACITY))

            // Optional blend mode
            params.getStringOrNull(ParamNames.BLEND_MODE)?.let { put(ParamNames.BLEND_MODE, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val opacity = params[ParamNames.OPACITY]?.jsonPrimitive?.doubleOrNull
        return "Successfully set opacity to $opacity for node: $nodeId"
    }
}
