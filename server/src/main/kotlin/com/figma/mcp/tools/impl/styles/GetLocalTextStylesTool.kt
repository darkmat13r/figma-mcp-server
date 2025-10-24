package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get Local Text Styles Tool
 *
 * ## Purpose
 * Retrieves all local text styles from the current Figma document.
 * Does not include styles from team libraries - only local styles.
 *
 * ## Parameters
 * None required
 *
 * ## Returns
 * Array of text styles with properties:
 * - id: Style ID
 * - name: Style name
 * - description: Style description
 * - fontSize: Font size
 * - fontName: Font family and style
 * - textAlignHorizontal: Horizontal alignment
 * - Other text properties
 */
class GetLocalTextStylesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_get_local_text_styles") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Retrieves all local text styles from the current Figma document. " +
                    "Returns an array of text styles with their IDs, names, and typography properties. " +
                    "Only includes local styles, not styles from team libraries. " +
                    "Text styles include font family, size, weight, line height, letter spacing, and more.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "getLocalTextStyles"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            // No parameters needed for this tool
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styles = pluginResponse?.jsonObject?.get("styles")?.jsonArray
        val count = styles?.size ?: 0

        return "Successfully retrieved $count local text style${if (count != 1) "s" else ""}"
    }
}
