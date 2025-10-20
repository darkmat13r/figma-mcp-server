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
 * Create Ellipse Tool
 *
 * ## Purpose
 * Creates circular or oval shapes.
 * Set width=height for perfect circles.
 *
 * ## Parameters
 * - width: number (required) - Ellipse width
 * - height: number (required) - Ellipse height
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 * - fills: Paint[] (optional) - Fill colors
 */
class CreateEllipseTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_ELLIPSE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a circular or oval shape in Figma. " +
                    "Use equal width and height to create perfect circles. " +
                    "Useful for avatars, icons, decorative elements, and illustrations.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Width in pixels (required, use same as height for circles)"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Height in pixels (required, use same as width for circles)"
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
                    )
                ),
                required = listOf(ParamNames.WIDTH, ParamNames.HEIGHT)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.ELLIPSE)
            put(ParamNames.WIDTH, params.getRequiredDouble(ParamNames.WIDTH))
            put(ParamNames.HEIGHT, params.getRequiredDouble(ParamNames.HEIGHT))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))
            params.getArrayOrNull(ParamNames.FILLS)?.let { put(ParamNames.FILLS, it) }
        }
    }
}
