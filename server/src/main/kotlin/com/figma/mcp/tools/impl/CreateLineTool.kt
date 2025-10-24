package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.NodeTypes
import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Create Line Tool
 *
 * ## Purpose
 * Creates straight line shapes from point A to point B.
 * Useful for dividers, connectors, and diagrams.
 *
 * ## Parameters
 * - x1: number (required) - Start X coordinate
 * - y1: number (required) - Start Y coordinate
 * - x2: number (required) - End X coordinate
 * - y2: number (required) - End Y coordinate
 * - strokes: Paint[] (optional) - Line color
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - strokeWeight: number (optional) - Line thickness
 * - effectStyleId: string (optional) - Effect style ID to apply
 */
class CreateLineTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_LINE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a straight line in Figma from point A to point B. " +
                    "Useful for dividers, separators, connectors, underlines, borders, " +
                    "and diagram connections. Supports custom stroke color and weight.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.X1 to mapOf(
                        "type" to "number",
                        "description" to "X coordinate of line start point (required)"
                    ),
                    ParamNames.Y1 to mapOf(
                        "type" to "number",
                        "description" to "Y coordinate of line start point (required)"
                    ),
                    ParamNames.X2 to mapOf(
                        "type" to "number",
                        "description" to "X coordinate of line end point (required)"
                    ),
                    ParamNames.Y2 to mapOf(
                        "type" to "number",
                        "description" to "Y coordinate of line end point (required)"
                    ),
                    ParamNames.STROKES to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for line color (optional)"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this line (optional)"
                    ),
                    ParamNames.STROKE_WEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Line thickness in pixels (optional, default: 1)"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this line (optional)"
                    )
                ),
                required = listOf(ParamNames.X1, ParamNames.Y1, ParamNames.X2, ParamNames.Y2)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.LINE)
            put(ParamNames.X1, params.getRequiredDouble(ParamNames.X1))
            put(ParamNames.Y1, params.getRequiredDouble(ParamNames.Y1))
            put(ParamNames.X2, params.getRequiredDouble(ParamNames.X2))
            put(ParamNames.Y2, params.getRequiredDouble(ParamNames.Y2))

            // Optional styling
            params.getArrayOrNull(ParamNames.STROKES)?.let { put(ParamNames.STROKES, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getDoubleOrNull(ParamNames.STROKE_WEIGHT)?.let { put(ParamNames.STROKE_WEIGHT, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }
}
