package com.figma.mcp.tools.impl.images

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
 * Set Image Fill Tool
 *
 * ## Purpose
 * Applies an image as a fill to an existing node.
 * Supports various scale modes and transformations.
 *
 * ## Parameters
 * - nodeId: string (required) - Target node ID
 * - imageData: string (required) - URL or base64 encoded image data
 * - scaleMode: string (optional) - How to scale the image: FILL | FIT | CROP | TILE
 * - rotation: number (optional) - Rotation angle in degrees
 * - opacity: number (optional) - Opacity from 0 to 1
 *
 * ## Scale Modes
 * - FILL: Stretch to fill the entire node
 * - FIT: Scale to fit inside the node while maintaining aspect ratio
 * - CROP: Scale to cover the entire node while maintaining aspect ratio
 * - TILE: Repeat the image as a pattern
 */
class SetImageFillTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_IMAGE_FILL) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Apply an image as a fill to an existing node. " +
                    "Supports URL or base64 data with various scale modes (FILL, FIT, CROP, TILE). " +
                    "Optional rotation and opacity controls.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to apply image fill to (required)"
                    ),
                    ParamNames.IMAGE_DATA to mapOf(
                        "type" to "string",
                        "description" to "URL or base64 encoded image data (required)"
                    ),
                    ParamNames.SCALE_MODE to mapOf(
                        "type" to "string",
                        "description" to "How to scale the image (optional): FILL (stretch), FIT (contain), CROP (cover), TILE (repeat)",
                        "enum" to listOf("FILL", "FIT", "CROP", "TILE")
                    ),
                    ParamNames.ROTATION to mapOf(
                        "type" to "number",
                        "description" to "Rotation angle in degrees (optional)"
                    ),
                    ParamNames.OPACITY to mapOf(
                        "type" to "number",
                        "description" to "Opacity from 0 to 1 (optional)",
                        "minimum" to 0,
                        "maximum" to 1
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.IMAGE_DATA)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_IMAGE_FILL
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.IMAGE_DATA, params.getRequiredString(ParamNames.IMAGE_DATA))
            params.getStringOrNull(ParamNames.SCALE_MODE)?.let { put(ParamNames.SCALE_MODE, it) }
            params.getDoubleOrNull(ParamNames.ROTATION)?.let { put(ParamNames.ROTATION, it) }
            params.getDoubleOrNull(ParamNames.OPACITY)?.let { put(ParamNames.OPACITY, it) }
        }
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val scaleMode = params[ParamNames.SCALE_MODE]?.jsonPrimitive?.contentOrNull ?: "default"
        return "Successfully applied image fill to node: $nodeId (scale mode: $scaleMode)"
    }
}
