package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get Local Grid Styles Tool
 *
 * ## Purpose
 * Retrieves all local grid/layout styles from the current Figma document.
 * Does not include styles from team libraries - only local styles.
 *
 * ## Parameters
 * None required
 *
 * ## Returns
 * Array of grid styles with properties:
 * - id: Style ID
 * - name: Style name
 * - description: Style description
 * - layoutGrids: Array of grid configurations
 */
class GetLocalGridStylesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_get_local_grid_styles") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Retrieves all local grid/layout styles from the current Figma document. " +
                    "Returns an array of grid styles with their IDs, names, and grid properties. " +
                    "Only includes local styles, not styles from team libraries. " +
                    "Grid styles define layout grids, columns, and rows for design systems.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "getLocalGridStyles"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            // No parameters needed for this tool
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the styles array data as JSON
        val stylesData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("styles", buildJsonArray {})
        }

        val styleCount = stylesData["styles"]?.jsonArray?.size ?: 0
        val jsonString = stylesData.toJsonString()

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Grid Styles ($styleCount): $jsonString")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styles = pluginResponse?.jsonObject?.get("styles")?.jsonArray
        val count = styles?.size ?: 0

        return "Successfully retrieved $count local grid style${if (count != 1) "s" else ""}"
    }
}
