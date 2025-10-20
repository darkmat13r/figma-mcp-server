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
 * Set Strokes Tool
 *
 * ## Purpose
 * Sets stroke/border properties on nodes.
 * Controls stroke colors, weight, alignment, caps, and joins.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - strokes: Paint[] (required) - Array of stroke paint definitions
 * - strokeWeight: number (optional) - Stroke thickness in pixels
 * - strokeAlign: "INSIDE" | "CENTER" | "OUTSIDE" (optional) - Stroke alignment
 * - strokeCap: "NONE" | "ROUND" | "SQUARE" | "ARROW_LINES" | "ARROW_EQUILATERAL" (optional)
 * - strokeJoin: "MITER" | "BEVEL" | "ROUND" (optional)
 */
class SetStrokesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_STROKES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Sets stroke (border) properties on a Figma node. " +
                    "Controls stroke colors, thickness, alignment (inside/center/outside), " +
                    "line caps, and line joins. Essential for creating borders and outlines.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply strokes to (required)"
                    ),
                    ParamNames.STROKES to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for stroke colors (required)"
                    ),
                    ParamNames.STROKE_WEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Stroke thickness in pixels (optional)"
                    ),
                    ParamNames.STROKE_ALIGN to mapOf(
                        "type" to "string",
                        "enum" to listOf("INSIDE", "CENTER", "OUTSIDE"),
                        "description" to "Stroke alignment relative to node bounds (optional)"
                    ),
                    ParamNames.STROKE_CAP to mapOf(
                        "type" to "string",
                        "enum" to listOf("NONE", "ROUND", "SQUARE", "ARROW_LINES", "ARROW_EQUILATERAL"),
                        "description" to "Line cap style (optional)"
                    ),
                    ParamNames.STROKE_JOIN to mapOf(
                        "type" to "string",
                        "enum" to listOf("MITER", "BEVEL", "ROUND"),
                        "description" to "Line join style (optional)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.STROKES)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.STROKES, params.getRequiredArray(ParamNames.STROKES))

            // Optional parameters
            params.getDoubleOrNull(ParamNames.STROKE_WEIGHT)?.let { put(ParamNames.STROKE_WEIGHT, it) }
            params.getStringOrNull(ParamNames.STROKE_ALIGN)?.let { put(ParamNames.STROKE_ALIGN, it) }
            params.getStringOrNull(ParamNames.STROKE_CAP)?.let { put(ParamNames.STROKE_CAP, it) }
            params.getStringOrNull(ParamNames.STROKE_JOIN)?.let { put(ParamNames.STROKE_JOIN, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully applied strokes to node: $nodeId"
    }
}
