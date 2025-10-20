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
 * Set Text Style Tool
 *
 * ## Purpose
 * Sets text formatting properties including font family, style, size, weight,
 * line height, letter spacing, text case, and text decoration.
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - fontFamily: string (optional) - Font family name
 * - fontStyle: string (optional) - Font style (e.g., "Regular", "Bold", "Italic")
 * - fontSize: number (optional) - Font size in pixels
 * - fontWeight: number (optional) - Font weight (100-900)
 * - lineHeight: object (optional) - Line height {value: number, unit: "AUTO"|"PIXELS"|"PERCENT"}
 * - letterSpacing: object (optional) - Letter spacing {value: number, unit: "PIXELS"|"PERCENT"}
 * - textCase: enum (optional) - Text case transformation
 * - textDecoration: enum (optional) - Text decoration (underline, strikethrough)
 */
class SetTextStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_TEXT_STYLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Sets text formatting properties on a text node in Figma. " +
                    "Controls font family, style, size, weight, line height, letter spacing, " +
                    "text case, and text decoration. All parameters except nodeId are optional.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to style (required)"
                    ),
                    ParamNames.FONT_FAMILY to mapOf(
                        "type" to "string",
                        "description" to "Font family name (optional, e.g., 'Inter', 'Roboto')"
                    ),
                    ParamNames.FONT_STYLE to mapOf(
                        "type" to "string",
                        "description" to "Font style (optional, e.g., 'Regular', 'Bold', 'Italic', 'Medium')"
                    ),
                    ParamNames.FONT_SIZE to mapOf(
                        "type" to "number",
                        "description" to "Font size in pixels (optional)"
                    ),
                    ParamNames.FONT_WEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Font weight (optional, 100-900). Maps to font styles: " +
                                "100=Thin, 200=ExtraLight, 300=Light, 400=Regular, 500=Medium, " +
                                "600=SemiBold, 700=Bold, 800=ExtraBold, 900=Black"
                    ),
                    ParamNames.LINE_HEIGHT to mapOf(
                        "type" to "object",
                        "description" to "Line height configuration (optional). " +
                                "Object with 'unit' and 'value' properties. " +
                                "unit: 'AUTO' | 'PIXELS' | 'PERCENT'. " +
                                "value: number (required for PIXELS and PERCENT). " +
                                "Can also be a simple number (treated as PIXELS)."
                    ),
                    ParamNames.LETTER_SPACING to mapOf(
                        "type" to "object",
                        "description" to "Letter spacing configuration (optional). " +
                                "Object with 'unit' and 'value' properties. " +
                                "unit: 'PIXELS' | 'PERCENT'. " +
                                "value: number. " +
                                "Can also be a simple number (treated as PERCENT)."
                    ),
                    ParamNames.TEXT_CASE to mapOf(
                        "type" to "string",
                        "description" to "Text case transformation (optional). " +
                                "Values: 'ORIGINAL' | 'UPPER' | 'LOWER' | 'TITLE'",
                        "enum" to listOf("ORIGINAL", "UPPER", "LOWER", "TITLE")
                    ),
                    ParamNames.TEXT_DECORATION to mapOf(
                        "type" to "string",
                        "description" to "Text decoration (optional). " +
                                "Values: 'NONE' | 'UNDERLINE' | 'STRIKETHROUGH'",
                        "enum" to listOf("NONE", "UNDERLINE", "STRIKETHROUGH")
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_TEXT_STYLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))

            // Optional font family
            params.getStringOrNull(ParamNames.FONT_FAMILY)?.let {
                put(ParamNames.FONT_FAMILY, it)
            }

            // Optional font style
            params.getStringOrNull(ParamNames.FONT_STYLE)?.let {
                put(ParamNames.FONT_STYLE, it)
            }

            // Optional font size
            params.getDoubleOrNull(ParamNames.FONT_SIZE)?.let {
                put(ParamNames.FONT_SIZE, it)
            }

            // Optional font weight
            params[ParamNames.FONT_WEIGHT]?.jsonPrimitive?.intOrNull?.let {
                put(ParamNames.FONT_WEIGHT, it)
            }

            // Optional line height (can be object or number)
            params[ParamNames.LINE_HEIGHT]?.let { lineHeight ->
                put(ParamNames.LINE_HEIGHT, lineHeight)
            }

            // Optional letter spacing (can be object or number)
            params[ParamNames.LETTER_SPACING]?.let { letterSpacing ->
                put(ParamNames.LETTER_SPACING, letterSpacing)
            }

            // Optional text case
            params.getStringOrNull(ParamNames.TEXT_CASE)?.let {
                put(ParamNames.TEXT_CASE, it)
            }

            // Optional text decoration
            params.getStringOrNull(ParamNames.TEXT_DECORATION)?.let {
                put(ParamNames.TEXT_DECORATION, it)
            }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set text style for node: $nodeId"
    }
}
