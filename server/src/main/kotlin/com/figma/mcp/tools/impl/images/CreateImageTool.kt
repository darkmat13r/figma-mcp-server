package com.figma.mcp.tools.impl.images

import com.figma.mcp.config.FigmaConstants.Defaults
import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Image Tool
 *
 * ## Purpose
 * Creates an image node from a URL or base64 encoded data.
 * The plugin will automatically detect whether the imageData is a URL or base64 string.
 *
 * ## Parameters
 * - imageData: string (required) - URL or base64 encoded image data
 * - width: number (optional) - Image width in pixels
 * - height: number (optional) - Image height in pixels
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 *
 * ## Returns
 * {
 *   imageNodeId: string - ID of the created image node
 * }
 */
class CreateImageTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_IMAGE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Create an image node from a URL or base64 encoded data. " +
                    "The plugin automatically detects whether the imageData is a URL or base64 string. " +
                    "Supports optional positioning and sizing.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.IMAGE_DATA to mapOf(
                        "type" to "string",
                        "description" to "URL or base64 encoded image data (required)"
                    ),
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Image width in pixels (optional)"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Image height in pixels (optional)"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    )
                ),
                required = listOf(ParamNames.IMAGE_DATA)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_IMAGE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.IMAGE_DATA, params.getRequiredString(ParamNames.IMAGE_DATA))
            params.getDoubleOrNull(ParamNames.WIDTH)?.let { put(ParamNames.WIDTH, it) }
            params.getDoubleOrNull(ParamNames.HEIGHT)?.let { put(ParamNames.HEIGHT, it) }
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val imageNodeId = pluginResponse?.jsonObject?.get("imageNodeId")?.jsonPrimitive?.contentOrNull
            ?: pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        val responseData = buildJsonObject {
            imageNodeId?.let { put("imageNodeId", it) }
        }

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = responseData.toJsonString())
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val imageNodeId = pluginResponse?.jsonObject?.get("imageNodeId")?.jsonPrimitive?.contentOrNull
            ?: pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull
        return if (imageNodeId != null) {
            "Successfully created image node with ID: $imageNodeId"
        } else {
            "Successfully created image node"
        }
    }
}
