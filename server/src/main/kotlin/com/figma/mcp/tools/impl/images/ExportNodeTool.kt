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
import com.figma.mcp.services.ExportedImageResourceManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*
import java.util.Base64

/**
 * Export Node Tool
 *
 * ## Purpose
 * Exports a node as an image in the specified format and saves it to a temporary file.
 *
 * ## Workflow
 * 1. Export node from Figma as image bytes
 * 2. Save to temporary file via ExportedImageResourceManager
 * 3. Register as MCP resource (accessible via resources/read)
 * 4. Return file path, metadata, and image content
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
 * - Text with file path and metadata (dimensions, format, size)
 * - ImageContent with the exported image
 * - Resource URI for MCP resource access
 *
 * The exported file can be used for any purpose:
 * - View with Read tool
 * - Copy to project directories
 * - Use in documentation
 * - Share with team
 */
class ExportNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager,
    private val resourceManager: ExportedImageResourceManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.EXPORT_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Export a node as an image in PNG, JPG, SVG, or PDF format. " +
                    "Saves the image to a temporary file and returns the file path, metadata, and image content. " +
                    "The exported file can be viewed with the Read tool or copied to your project.",
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
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val format = params[ParamNames.FORMAT]?.jsonPrimitive?.contentOrNull ?: "PNG"
        val width = responseObj["width"]?.jsonPrimitive?.intOrNull ?: 0
        val height = responseObj["height"]?.jsonPrimitive?.intOrNull ?: 0

        // Convert array of bytes to ByteArray
        val imageBytes = if (imageDataArray != null) {
            ByteArray(imageDataArray.size) { i ->
                imageDataArray[i].jsonPrimitive.int.toByte()
            }
        } else {
            ByteArray(0)
        }

        // Determine MIME type based on format
        val mimeType = when (format.uppercase()) {
            "PNG" -> "image/png"
            "JPG", "JPEG" -> "image/jpeg"
            "SVG" -> "image/svg+xml"
            "PDF" -> "application/pdf"
            else -> "image/png"
        }

        // Save to file via resource manager
        val exportedImage = resourceManager.saveExport(
            nodeId = nodeId,
            imageData = imageBytes,
            format = format,
            mimeType = mimeType,
            width = width,
            height = height
        )

        // Convert to base64 for ImageContent
        val base64ImageData = Base64.getEncoder().encodeToString(imageBytes)

        // Return file path, metadata, and image content
        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(
                    text = """
                        Export successful!

                        File path: ${exportedImage.filePath.toAbsolutePath()}
                        Resource URI: ${exportedImage.uri}
                        Dimensions: ${width}x${height}
                        Format: $format
                        Size: ${String.format("%.2f", imageBytes.size / 1024.0)} KB

                        The exported file is saved to the path above. You can:
                        - Use Read tool to view the image
                        - Copy to your project using Bash tool
                        - Access via MCP resource URI
                    """.trimIndent()
                ),
                ToolContent.ImageContent(
                    data = base64ImageData,
                    mimeType = mimeType
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
