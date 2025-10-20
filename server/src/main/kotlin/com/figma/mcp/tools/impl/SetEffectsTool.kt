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
 * Set Effects Tool
 *
 * ## Purpose
 * Applies visual effects to nodes (shadows, blurs, etc.).
 * Essential for adding depth and visual hierarchy to designs.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - effects: Effect[] (required) - Array of effect definitions
 *   Each effect:
 *   - type: "DROP_SHADOW" | "INNER_SHADOW" | "LAYER_BLUR" | "BACKGROUND_BLUR"
 *   - color: {r, g, b, a} (for shadows)
 *   - offset: {x, y} (for shadows)
 *   - radius: number (blur radius)
 *   - spread: number (for shadows)
 *   - visible: boolean (default: true)
 *   - blendMode: string (optional)
 */
class SetEffectsTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_EFFECTS) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Applies visual effects like shadows and blurs to a Figma node. " +
                    "Supports drop shadows, inner shadows, layer blur, and background blur. " +
                    "Use this to add depth, elevation, and visual polish to your designs.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply effects to (required)"
                    ),
                    ParamNames.EFFECTS to mapOf(
                        "type" to "array",
                        "description" to "Array of Effect objects (required). " +
                                "Each effect must have 'type': DROP_SHADOW, INNER_SHADOW, LAYER_BLUR, or BACKGROUND_BLUR. " +
                                "Shadows require 'color' {r,g,b,a}, 'offset' {x,y}, 'radius', and optional 'spread'. " +
                                "Blurs require 'radius'. Optional: 'visible' (boolean), 'blendMode'."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.EFFECTS)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.EFFECTS, params.getRequiredArray(ParamNames.EFFECTS))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val effectsCount = params[ParamNames.EFFECTS]?.jsonArray?.size ?: 0
        return "Successfully applied $effectsCount effect(s) to node: $nodeId"
    }
}
