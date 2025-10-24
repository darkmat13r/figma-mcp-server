package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get Local Paint Styles Tool
 *
 * ## Purpose
 * Retrieves all local paint (color/fill) styles from the current Figma document.
 * Does not include styles from team libraries - only local styles.
 *
 * ## Parameters
 * None required
 *
 * ## Returns
 * Array of paint styles with properties:
 * - id: Style ID
 * - name: Style name
 * - description: Style description
 * - paints: Array of paint properties
 */
class GetLocalPaintStylesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_get_local_paint_styles") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Retrieves all local paint (color/fill) styles from the current Figma document. " +
                    "Returns an array of paint styles with their IDs, names, and paint properties. " +
                    "Only includes local styles, not styles from team libraries. " +
                    "Paint styles can include solid colors, gradients, and images.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "getLocalPaintStyles"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            // No parameters needed for this tool
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styles = pluginResponse?.jsonObject?.get("styles")?.jsonArray
        val count = styles?.size ?: 0

        return "Successfully retrieved $count local paint style${if (count != 1) "s" else ""}"
    }
}
