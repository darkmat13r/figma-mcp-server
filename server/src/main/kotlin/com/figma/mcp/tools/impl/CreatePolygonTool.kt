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
 * Create Polygon Tool
 *
 * ## Purpose
 * Creates polygon shapes with customizable number of sides.
 * Useful for geometric designs and icons.
 *
 * ## Parameters
 * - sides: number (required) - Number of sides (3-100)
 * - radius: number (required) - Radius from center to vertex
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 * - fills: Paint[] (optional) - Fill colors
 */
class CreatePolygonTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_POLYGON) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a polygon shape in Figma with a customizable number of sides. " +
                    "Useful for creating triangles (3 sides), hexagons (6 sides), octagons (8 sides), " +
                    "and other geometric shapes for icons, backgrounds, and decorative elements.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.SIDES to mapOf(
                        "type" to "integer",
                        "description" to "Number of sides (required, 3-100). Examples: 3=triangle, 6=hexagon, 8=octagon",
                        "minimum" to Defaults.MIN_POLYGON_SIDES,
                        "maximum" to Defaults.MAX_POLYGON_SIDES
                    ),
                    ParamNames.RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Radius from center to vertex in pixels (required)"
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
                    )
                ),
                required = listOf(ParamNames.SIDES, ParamNames.RADIUS)
            )
        )
    }

    override fun validate(arguments: JsonObject): String? {
        // Validate sides is within range
        val sides = arguments[ParamNames.SIDES]?.jsonPrimitive?.intOrNull
            ?: return "Missing or invalid parameter: sides"

        if (sides < Defaults.MIN_POLYGON_SIDES || sides > Defaults.MAX_POLYGON_SIDES) {
            return "Parameter 'sides' must be between ${Defaults.MIN_POLYGON_SIDES} and ${Defaults.MAX_POLYGON_SIDES}"
        }

        return null // Valid
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val sides = params.getRequiredInt(ParamNames.SIDES)
        validateRange(sides, Defaults.MIN_POLYGON_SIDES, Defaults.MAX_POLYGON_SIDES, ParamNames.SIDES)

        return buildJsonObject {
            put("type", NodeTypes.POLYGON)
            put(ParamNames.SIDES, sides)
            put(ParamNames.RADIUS, params.getRequiredDouble(ParamNames.RADIUS))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))
            params.getArrayOrNull(ParamNames.FILLS)?.let { put(ParamNames.FILLS, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val sides = params[ParamNames.SIDES]?.jsonPrimitive?.int ?: 0
        val nodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        return if (nodeId != null) {
            "Successfully created polygon with $sides sides (ID: $nodeId)"
        } else {
            "Successfully created polygon with $sides sides"
        }
    }
}
