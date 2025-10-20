package com.figma.mcp.tools.impl

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
 * Load Font Tool
 *
 * ## Purpose
 * Loads a font in Figma before use. Fonts must be loaded before they can be applied to text nodes.
 * Returns information about whether the font is available.
 *
 * ## Parameters
 * - fontFamily: string (required) - Font family name
 * - fontStyle: string (required) - Font style (e.g., "Regular", "Bold", "Italic")
 *
 * ## Response
 * Returns an object with:
 * - available: boolean - Whether the font is available
 * - message: string - Success or error message
 */
class LoadFontTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.LOAD_FONT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Loads a font in Figma before use. " +
                    "Fonts must be loaded before applying them to text nodes. " +
                    "Returns availability status and message. " +
                    "Common styles: 'Regular', 'Bold', 'Italic', 'Medium', 'Light', etc.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.FONT_FAMILY to mapOf(
                        "type" to "string",
                        "description" to "Font family name (required, e.g., 'Inter', 'Roboto', 'Arial')"
                    ),
                    ParamNames.FONT_STYLE to mapOf(
                        "type" to "string",
                        "description" to "Font style (required, e.g., 'Regular', 'Bold', 'Italic', 'Medium')"
                    )
                ),
                required = listOf(ParamNames.FONT_FAMILY, ParamNames.FONT_STYLE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.LOAD_FONT
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.FONT_FAMILY, params.getRequiredString(ParamNames.FONT_FAMILY))
            put(ParamNames.FONT_STYLE, params.getRequiredString(ParamNames.FONT_STYLE))
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Extract response data from plugin
        val available = pluginResponse?.jsonObject?.get("available")?.jsonPrimitive?.booleanOrNull ?: false
        val message = pluginResponse?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: "Font load operation completed"

        // Return the response with isError based on availability
        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = !available
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        // This method is overridden by formatSuccessResponse, but provide a fallback
        val fontFamily = params[ParamNames.FONT_FAMILY]?.jsonPrimitive?.contentOrNull
        val fontStyle = params[ParamNames.FONT_STYLE]?.jsonPrimitive?.contentOrNull
        return "Font load completed for: $fontFamily $fontStyle"
    }
}
