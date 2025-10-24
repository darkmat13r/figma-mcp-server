package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.Defaults
import com.figma.mcp.config.FigmaConstants.NodeTypes
import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Create Text Tool
 *
 * ## Purpose
 * Creates text nodes with customizable typography.
 *
 * ## Parameters
 * - text: string (required) - The text content
 * - fontFamily: string (optional) - Font family name
 * - fontStyle: string (optional) - Font style (e.g., "Regular", "Bold")
 * - fontSize: number (optional) - Font size in pixels
 * - textAlignHorizontal: string (optional) - Text alignment
 * - fills: Paint[] (optional) - Text color
 * - textStyleId: string (optional) - Text style ID to apply
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 */
class CreateTextTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_TEXT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a text node in Figma with customizable typography. " +
                    "Supports font family, size, style, alignment, and color. " +
                    "Essential for all text content in designs including headings, body text, labels, and buttons.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.TEXT to mapOf(
                        "type" to "string",
                        "description" to "The text content to display (required)"
                    ),
                    ParamNames.FONT_FAMILY to mapOf(
                        "type" to "string",
                        "description" to "Font family name (optional, default: '${Defaults.DEFAULT_FONT_FAMILY}')"
                    ),
                    ParamNames.FONT_STYLE to mapOf(
                        "type" to "string",
                        "description" to "Font style like 'Regular', 'Bold', 'Italic' (optional, default: '${Defaults.DEFAULT_FONT_STYLE}')"
                    ),
                    ParamNames.FONT_SIZE to mapOf(
                        "type" to "number",
                        "description" to "Font size in pixels (optional, default: ${Defaults.DEFAULT_FONT_SIZE})"
                    ),
                    ParamNames.TEXT_ALIGN_HORIZONTAL to mapOf(
                        "type" to "string",
                        "enum" to listOf("LEFT", "CENTER", "RIGHT", "JUSTIFIED"),
                        "description" to "Horizontal text alignment (optional)"
                    ),
                    ParamNames.FILLS to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for text color (optional)"
                    ),
                    ParamNames.TEXT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Text style ID to apply to this text node (optional)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this text node (optional)"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this text node (optional)"
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
                required = listOf(ParamNames.TEXT)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.TEXT)
            put(ParamNames.TEXT, params.getRequiredString(ParamNames.TEXT))
            put(ParamNames.FONT_FAMILY, params.getStringOrDefault(ParamNames.FONT_FAMILY, Defaults.DEFAULT_FONT_FAMILY))
            put(ParamNames.FONT_STYLE, params.getStringOrDefault(ParamNames.FONT_STYLE, Defaults.DEFAULT_FONT_STYLE))
            put(ParamNames.FONT_SIZE, params.getDoubleOrDefault(ParamNames.FONT_SIZE, Defaults.DEFAULT_FONT_SIZE))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))

            // Optional parameters
            params.getStringOrNull(ParamNames.TEXT_ALIGN_HORIZONTAL)?.let { put(ParamNames.TEXT_ALIGN_HORIZONTAL, it) }
            params.getArrayOrNull(ParamNames.FILLS)?.let { put(ParamNames.FILLS, it) }
            params.getStringOrNull(ParamNames.TEXT_STYLE_ID)?.let { put(ParamNames.TEXT_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }
}
