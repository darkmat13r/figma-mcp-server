package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.Defaults
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
 * Create Rectangle Tool
 *
 * ## Purpose
 * Creates rectangle shapes with optional fills, strokes, and corner radius.
 * Rectangles are the most common shape in UI design.
 *
 * ## Parameters
 * - width: number (required) - Rectangle width
 * - height: number (required) - Rectangle height
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 * - fills: Paint[] (optional) - Fill colors
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - cornerRadius: number (optional) - Corner radius for rounded corners
 * - strokes: Paint[] (optional) - Stroke colors
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - strokeWeight: number (optional) - Stroke thickness
 * - effectStyleId: string (optional) - Effect style ID to apply
 */
class CreateRectangleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_RECTANGLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a rectangle shape in Figma. " +
                    "Rectangles support fills, strokes, corner radius, and are the foundation " +
                    "for most UI elements like buttons, cards, and containers.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Width in pixels (required)"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Height in pixels (required)"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    ),
                    ParamNames.FILLS to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for fills (optional)"
                    ),
                    ParamNames.FILL_COLOR to mapOf(
                        "type" to "string",
                        "description" to "Fill color as hex code (e.g., '#FF0000') - shorthand for solid fill (optional)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this rectangle (optional)"
                    ),
                    ParamNames.CORNER_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Corner radius for rounded corners (optional)"
                    ),
                    ParamNames.STROKES to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for strokes (optional)"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this rectangle (optional)"
                    ),
                    ParamNames.STROKE_WEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Stroke thickness in pixels (optional)"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this rectangle (optional)"
                    )
                ),
                required = listOf(ParamNames.WIDTH, ParamNames.HEIGHT)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.RECTANGLE)
            put(ParamNames.WIDTH, params.getRequiredDouble(ParamNames.WIDTH))
            put(ParamNames.HEIGHT, params.getRequiredDouble(ParamNames.HEIGHT))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))

            // Optional styling parameters
            params.getArrayOrNull(ParamNames.FILLS)?.let { put(ParamNames.FILLS, it) }
            params.getStringOrNull(ParamNames.FILL_COLOR)?.let { put(ParamNames.FILL_COLOR, it) }
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getDoubleOrNull(ParamNames.CORNER_RADIUS)?.let { put(ParamNames.CORNER_RADIUS, it) }
            params.getArrayOrNull(ParamNames.STROKES)?.let { put(ParamNames.STROKES, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getDoubleOrNull(ParamNames.STROKE_WEIGHT)?.let { put(ParamNames.STROKE_WEIGHT, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }
}
