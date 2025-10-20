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
 * Set Fills Tool
 *
 * ## Purpose
 * Sets fill colors and gradients on nodes.
 * Supports solid colors, linear gradients, radial gradients, and image fills.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - fills: Paint[] (required) - Array of fill definitions
 *   Each fill can be:
 *   - type: "SOLID" | "GRADIENT_LINEAR" | "GRADIENT_RADIAL" | "IMAGE"
 *   - color: {r, g, b, a} (for SOLID fills)
 *   - gradientStops: Array<{color, position}> (for gradients)
 *   - imageRef: string (for IMAGE fills)
 *   - opacity: number (0-1, optional)
 *   - blendMode: string (optional)
 */
class SetFillsTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_FILLS) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Sets fill colors, gradients, or images on a Figma node. " +
                    "Supports solid colors, linear/radial gradients, and image fills. " +
                    "Use this to change the background or fill appearance of any node.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply fills to (required)"
                    ),
                    ParamNames.FILLS to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects defining fills (required). " +
                                "Each paint must have 'type' field: SOLID, GRADIENT_LINEAR, GRADIENT_RADIAL, or IMAGE. " +
                                "SOLID requires 'color' {r, g, b, a}. Gradients require 'gradientStops'. " +
                                "IMAGE requires 'imageRef'. Optional: 'opacity' (0-1), 'blendMode'."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.FILLS)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.FILLS, params.getRequiredArray(ParamNames.FILLS))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully applied fills to node: $nodeId"
    }
}
