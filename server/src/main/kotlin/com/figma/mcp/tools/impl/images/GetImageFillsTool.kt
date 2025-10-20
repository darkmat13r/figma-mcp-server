package com.figma.mcp.tools.impl.images

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
 * Get Image Fills Tool
 *
 * ## Purpose
 * Retrieves all image fills applied to a node.
 * Returns information about image references, scale modes, and transformations.
 *
 * ## Parameters
 * - nodeId: string (required) - Node to get image fills from
 *
 * ## Returns
 * {
 *   images: ImagePaint[] - Array of image fill information
 * }
 *
 * Each ImagePaint contains:
 * - type: "IMAGE"
 * - scaleMode: string (FILL, FIT, CROP, TILE)
 * - imageRef: string (image reference/hash)
 * - imageTransform: matrix (optional)
 * - opacity: number (optional)
 * - blendMode: string (optional)
 */
class GetImageFillsTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_IMAGE_FILLS) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get image references and fill information from a node. " +
                    "Returns all image fills with their properties including scale mode, " +
                    "image reference, transformations, opacity, and blend mode.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to get image fills from (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_IMAGE_FILLS
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the images array data as JSON
        val imagesData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("images", buildJsonArray {})
        }

        val imageCount = imagesData["images"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Image fills (${imageCount}): ${imagesData.toString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val imageCount = pluginResponse?.jsonObject?.get("images")?.jsonArray?.size ?: 0
        return "Successfully retrieved $imageCount image fill(s) from node: $nodeId"
    }
}
