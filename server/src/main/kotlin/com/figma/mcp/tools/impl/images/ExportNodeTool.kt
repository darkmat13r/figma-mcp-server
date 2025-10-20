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
 * Export Node Tool
 *
 * ## Purpose
 * Exports a node as an image in the specified format.
 * Returns base64 encoded image data.
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
 * {
 *   imageData: string - Base64 encoded image data
 *   format: string - The export format used
 * }
 */
class ExportNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.EXPORT_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Export a node as an image in PNG, JPG, SVG, or PDF format. " +
                    "Returns base64 encoded image data. " +
                    "Supports custom scale and size constraints.",
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
        // Extract the base64 image data from the response
        val responseObj = pluginResponse?.jsonObject ?: buildJsonObject {}
        val imageData = responseObj["imageData"]?.jsonPrimitive?.contentOrNull ?: ""
        val format = params[ParamNames.FORMAT]?.jsonPrimitive?.contentOrNull ?: "PNG"

        val resultData = buildJsonObject {
            put("imageData", imageData)
            put("format", format)
        }

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = resultData.toString())
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
        val imageData = pluginResponse?.jsonObject?.get("imageData")?.jsonPrimitive?.contentOrNull
        val dataLength = imageData?.length ?: 0
        return "Successfully exported node $nodeId as $format (${dataLength} bytes base64 data)"
    }
}
