package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Paint Style Tool
 *
 * ## Purpose
 * Creates a new local paint (color/fill) style in the current Figma document.
 * Paint styles can be used for fills, strokes, and backgrounds.
 *
 * ## Parameters
 * - name: string (optional) - The name for the new style (default: "Paint Style")
 * - description: string (optional) - Description for the style
 * - paints: Paint[] (optional) - Initial paint values for the style
 *
 * ## Returns
 * - styleId: The ID of the newly created style
 * - name: The name of the style
 */
class CreatePaintStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_create_paint_style") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a new local paint (color/fill) style in the current Figma document. " +
                    "Paint styles can include solid colors, gradients, or images. " +
                    "The newly created style is local to the current document. " +
                    "Useful for creating reusable color palettes and fill patterns.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "Name for the new paint style (optional, default: 'Paint Style')"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "Description for the style (optional)"
                    ),
                    "paints" to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects to set as initial value (optional)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "createPaintStyle"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull("name")?.let { put("name", it) }
            params.getStringOrNull("description")?.let { put("description", it) }
            params.getArrayOrNull("paints")?.let { put("paints", it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styleId = pluginResponse?.jsonObject?.get("styleId")?.jsonPrimitive?.contentOrNull
        val styleName = pluginResponse?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: params["name"]?.jsonPrimitive?.contentOrNull
            ?: "Paint Style"

        return if (styleId != null) {
            "Successfully created paint style '$styleName' (ID: $styleId)"
        } else {
            "Successfully created paint style '$styleName'"
        }
    }
}
