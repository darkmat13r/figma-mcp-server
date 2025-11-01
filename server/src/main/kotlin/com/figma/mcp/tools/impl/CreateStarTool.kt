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
import kotlinx.serialization.json.*

/**
 * Create Star Tool
 *
 * ## Purpose
 * Creates star shapes with customizable number of points.
 * Useful for ratings, decorations, and icons.
 *
 * ## Parameters
 * - points: number (required) - Number of star points (3-100)
 * - radius: number (required) - Outer radius
 * - innerRadius: number (optional) - Inner radius (controls star sharpness)
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 * - fills: Paint[] (optional) - Fill colors
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
 */
class CreateStarTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_STAR) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a star shape in Figma with customizable number of points. " +
                    "Perfect for rating systems (5-point stars), decorative elements, badges, and icons. " +
                    "The innerRadius parameter controls how sharp or rounded the star appears.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.POINTS to mapOf(
                        "type" to "integer",
                        "description" to "Number of star points (required, 3-100). Common: 5 for ratings, 6 for hexagrams",
                        "minimum" to Defaults.MIN_STAR_POINTS,
                        "maximum" to Defaults.MAX_STAR_POINTS
                    ),
                    ParamNames.RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Outer radius from center to point tip in pixels (required)"
                    ),
                    ParamNames.INNER_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Inner radius from center to point valley (optional, controls star sharpness)"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position of center (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position of center (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    ),
                    ParamNames.FILLS to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for fills (optional)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    )
                ),
                required = listOf(ParamNames.POINTS, ParamNames.RADIUS)
            )
        )
    }

    override fun validate(arguments: JsonObject): String? {
        // Validate points is within range
        val points = arguments[ParamNames.POINTS]?.jsonPrimitive?.intOrNull
            ?: return "Missing or invalid parameter: points"

        if (points < Defaults.MIN_STAR_POINTS || points > Defaults.MAX_STAR_POINTS) {
            return "Parameter 'points' must be between ${Defaults.MIN_STAR_POINTS} and ${Defaults.MAX_STAR_POINTS}"
        }

        return null // Valid
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val points = params.getRequiredInt(ParamNames.POINTS)
        validateRange(points, Defaults.MIN_STAR_POINTS, Defaults.MAX_STAR_POINTS, ParamNames.POINTS)

        return buildJsonObject {
            put("type", NodeTypes.STAR)
            put(ParamNames.POINTS, points)
            put(ParamNames.RADIUS, params.getRequiredDouble(ParamNames.RADIUS))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))

            // Optional parameters
            params.getDoubleOrNull(ParamNames.INNER_RADIUS)?.let { put(ParamNames.INNER_RADIUS, it) }
            params.getArrayOrNull(ParamNames.FILLS)?.let { put(ParamNames.FILLS, it) }
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val points = params[ParamNames.POINTS]?.jsonPrimitive?.int ?: 0
        val nodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        return if (nodeId != null) {
            "Successfully created $points-point star (ID: $nodeId)"
        } else {
            "Successfully created $points-point star"
        }
    }
}
