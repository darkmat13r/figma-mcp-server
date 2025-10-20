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
 * Set Corner Radius Tool
 *
 * ## Purpose
 * Rounds corners on rectangles and frames.
 * Supports uniform or individual corner radii.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - cornerRadius: number (optional) - Apply same radius to all corners
 * - topLeftRadius: number (optional) - Top-left corner radius
 * - topRightRadius: number (optional) - Top-right corner radius
 * - bottomLeftRadius: number (optional) - Bottom-left corner radius
 * - bottomRightRadius: number (optional) - Bottom-right corner radius
 * - cornerSmoothing: number (optional, 0-1) - Corner smoothing factor
 */
class SetCornerRadiusTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_CORNER_RADIUS) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Rounds corners on rectangles and frames in Figma. " +
                    "Can set all corners uniformly with 'cornerRadius', or control each corner " +
                    "individually. Supports corner smoothing for iOS-style continuous curves.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to round corners on (required)"
                    ),
                    ParamNames.CORNER_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Radius to apply to all corners (optional, overrides individual corners)"
                    ),
                    ParamNames.TOP_LEFT_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Top-left corner radius in pixels (optional)"
                    ),
                    ParamNames.TOP_RIGHT_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Top-right corner radius in pixels (optional)"
                    ),
                    ParamNames.BOTTOM_LEFT_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Bottom-left corner radius in pixels (optional)"
                    ),
                    ParamNames.BOTTOM_RIGHT_RADIUS to mapOf(
                        "type" to "number",
                        "description" to "Bottom-right corner radius in pixels (optional)"
                    ),
                    ParamNames.CORNER_SMOOTHING to mapOf(
                        "type" to "number",
                        "description" to "Corner smoothing factor 0-1 for iOS-style curves (optional)",
                        "minimum" to 0.0,
                        "maximum" to 1.0
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))

            // Uniform or individual corner radii
            params.getDoubleOrNull(ParamNames.CORNER_RADIUS)?.let { put(ParamNames.CORNER_RADIUS, it) }
            params.getDoubleOrNull(ParamNames.TOP_LEFT_RADIUS)?.let { put(ParamNames.TOP_LEFT_RADIUS, it) }
            params.getDoubleOrNull(ParamNames.TOP_RIGHT_RADIUS)?.let { put(ParamNames.TOP_RIGHT_RADIUS, it) }
            params.getDoubleOrNull(ParamNames.BOTTOM_LEFT_RADIUS)?.let { put(ParamNames.BOTTOM_LEFT_RADIUS, it) }
            params.getDoubleOrNull(ParamNames.BOTTOM_RIGHT_RADIUS)?.let { put(ParamNames.BOTTOM_RIGHT_RADIUS, it) }
            params.getDoubleOrNull(ParamNames.CORNER_SMOOTHING)?.let { put(ParamNames.CORNER_SMOOTHING, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set corner radius for node: $nodeId"
    }
}
