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
import java.util.Base64

/**
 * Export Node Tool
 *
 * ## Purpose
 * Exports a node as an image in the specified format.
 * Returns the image as ImageContent (binary blob) to avoid token consumption.
 *
 * ## Parameters
 * - nodeId: string (required) - Node to export
 * - format: string (required) - Export format: PNG | JPG | SVG | PDF
 * - scale: number (optional) - Scale factor from 1 to 4
 * - constraint: object (optional) - Size constraint
 *   - type: SCALE | WIDTH | HEIGHT
 *   - value: number
 *
 * ## Returns
 * ImageContent with base64 encoded image data and proper MIME type
 * Plus a text summary with dimensions and format
 */
class ExportNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.EXPORT_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Export a node as an image in PNG, JPG, SVG, or PDF format. " +
                    "Returns the image as ImageContent (binary blob) which doesn't consume tokens. " +
                    "Supports custom scale and size constraints. " +
                    "Perfect for saving images to files or analyzing visual content.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to export (required)"
                    ),
                    ParamNames.FORMAT to mapOf(
                        "type" to "string",
                        "description" to "Export format (required): PNG, JPG, SVG, or PDF",
                        "enum" to listOf("PNG", "JPG", "SVG", "PDF")
                    ),
                    ParamNames.SCALE to mapOf(
                        "type" to "number",
                        "description" to "Scale factor from 1 to 4 (optional, default: 1)",
                        "minimum" to 1,
                        "maximum" to 4
                    ),
                    ParamNames.CONSTRAINT to mapOf(
                        "type" to "object",
                        "description" to "Size constraint (optional). Object with 'type' (SCALE|WIDTH|HEIGHT) and 'value' (number)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.FORMAT)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.EXPORT_NODE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.FORMAT, params.getRequiredString(ParamNames.FORMAT))
            params.getDoubleOrNull(ParamNames.SCALE)?.let {
                // Validate scale is between 1 and 4
                val scale = it.coerceIn(1.0, 4.0)
                put(ParamNames.SCALE, scale)
            }
            params.getObjectOrNull(ParamNames.CONSTRAINT)?.let { put(ParamNames.CONSTRAINT, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Extract the binary image data from the response (comes as array of bytes)
        val responseObj = pluginResponse?.jsonObject ?: buildJsonObject {}
        val imageDataArray = responseObj["imageData"]?.jsonArray
        val format = params[ParamNames.FORMAT]?.jsonPrimitive?.contentOrNull ?: "PNG"
        val width = responseObj["width"]?.jsonPrimitive?.intOrNull ?: 0
        val height = responseObj["height"]?.jsonPrimitive?.intOrNull ?: 0

        // Convert array of bytes to base64
        val base64ImageData = if (imageDataArray != null) {
            val byteArray = ByteArray(imageDataArray.size) { i ->
                imageDataArray[i].jsonPrimitive.int.toByte()
            }
            Base64.getEncoder().encodeToString(byteArray)
        } else {
            ""
        }

        // Determine MIME type based on format
        val mimeType = when (format.uppercase()) {
            "PNG" -> "image/png"
            "JPG", "JPEG" -> "image/jpeg"
            "SVG" -> "image/svg+xml"
            "PDF" -> "application/pdf"
            else -> "image/png"
        }

        // Return as ImageContent to avoid token consumption when Claude views it
        // The base64 data is still accessible in the ImageContent.data field for programmatic use
        // Also include a small JSON with metadata for easier programmatic access
        val metadata = buildJsonObject {
            put("width", width)
            put("height", height)
            put("format", format)
            put("mimeType", mimeType)
            put("sizeBytes", imageDataArray?.size ?: 0)
        }

        return CallToolResult(
            content = listOf(
                ToolContent.ImageContent(
                    data = base64ImageData,
                    mimeType = mimeType
                ),
                ToolContent.TextContent(
                    text = "Image exported successfully. Metadata: ${metadata.toString()}"
                )
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val format = params[ParamNames.FORMAT]?.jsonPrimitive?.contentOrNull
        val imageDataArray = pluginResponse?.jsonObject?.get("imageData")?.jsonArray
        val dataLength = imageDataArray?.size ?: 0
        return "Successfully exported node $nodeId as $format ($dataLength bytes binary data)"
    }
}
