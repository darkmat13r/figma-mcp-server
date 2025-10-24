package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Text Style Tool
 *
 * ## Purpose
 * Creates a new local text style in the current Figma document.
 * Text styles define typography properties like font, size, and spacing.
 * By default, the style has Figma default properties (Inter Regular, 12px).
 *
 * ## Parameters
 * - name: string (optional) - The name for the new style (default: "Text Style")
 * - description: string (optional) - Description for the style
 * - fontSize: number (optional) - Font size in pixels
 * - fontFamily: string (optional) - Font family name
 * - fontWeight: string (optional) - Font weight (e.g., "Regular", "Bold")
 * - lineHeight: object (optional) - Line height configuration
 * - letterSpacing: object (optional) - Letter spacing configuration
 * - textAlignHorizontal: string (optional) - Horizontal text alignment
 *
 * ## Returns
 * - styleId: The ID of the newly created style
 * - name: The name of the style
 */
class CreateTextStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_create_text_style") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a new local text style in the current Figma document. " +
                    "Text styles define typography properties including font family, size, weight, " +
                    "line height, letter spacing, and text alignment. " +
                    "By default, created with Figma defaults (Inter Regular, 12px). " +
                    "The newly created style is local to the current document.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "Name for the new text style (optional, default: 'Text Style')"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "Description for the style (optional)"
                    ),
                    "fontSize" to mapOf(
                        "type" to "number",
                        "description" to "Font size in pixels (optional)"
                    ),
                    "fontFamily" to mapOf(
                        "type" to "string",
                        "description" to "Font family name (optional)"
                    ),
                    "fontWeight" to mapOf(
                        "type" to "string",
                        "description" to "Font weight like 'Regular', 'Bold', 'Light' (optional)"
                    ),
                    "lineHeight" to mapOf(
                        "type" to "object",
                        "description" to "Line height configuration (optional)"
                    ),
                    "letterSpacing" to mapOf(
                        "type" to "object",
                        "description" to "Letter spacing configuration (optional)"
                    ),
                    "textAlignHorizontal" to mapOf(
                        "type" to "string",
                        "description" to "Horizontal alignment: 'LEFT', 'CENTER', 'RIGHT', 'JUSTIFIED' (optional)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "createTextStyle"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull("name")?.let { put("name", it) }
            params.getStringOrNull("description")?.let { put("description", it) }
            params.getDoubleOrNull("fontSize")?.let { put("fontSize", it) }
            params.getStringOrNull("fontFamily")?.let { put("fontFamily", it) }
            params.getStringOrNull("fontWeight")?.let { put("fontWeight", it) }
            params.getObjectOrNull("lineHeight")?.let { put("lineHeight", it) }
            params.getObjectOrNull("letterSpacing")?.let { put("letterSpacing", it) }
            params.getStringOrNull("textAlignHorizontal")?.let { put("textAlignHorizontal", it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styleId = pluginResponse?.jsonObject?.get("styleId")?.jsonPrimitive?.contentOrNull
        val styleName = pluginResponse?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: params["name"]?.jsonPrimitive?.contentOrNull
            ?: "Text Style"

        return if (styleId != null) {
            "Successfully created text style '$styleName' (ID: $styleId)"
        } else {
            "Successfully created text style '$styleName'"
        }
    }
}
