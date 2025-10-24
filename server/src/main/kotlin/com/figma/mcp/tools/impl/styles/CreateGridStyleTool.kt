package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Grid Style Tool
 *
 * ## Purpose
 * Creates a new local grid/layout style in the current Figma document.
 * Grid styles define layout grids for consistent spacing and alignment.
 *
 * ## Parameters
 * - name: string (optional) - The name for the new style (default: "Grid Style")
 * - description: string (optional) - Description for the style
 * - layoutGrids: LayoutGrid[] (optional) - Initial grid configurations
 *
 * ## Returns
 * - styleId: The ID of the newly created style
 * - name: The name of the style
 */
class CreateGridStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_create_grid_style") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a new local grid/layout style in the current Figma document. " +
                    "Grid styles define layout grids including rows, columns, and alignment grids. " +
                    "The newly created style is local to the current document. " +
                    "Useful for creating reusable layout systems and maintaining consistent spacing.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "Name for the new grid style (optional, default: 'Grid Style')"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "Description for the style (optional)"
                    ),
                    "layoutGrids" to mapOf(
                        "type" to "array",
                        "description" to "Array of LayoutGrid objects to set as initial value (optional)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "createGridStyle"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull("name")?.let { put("name", it) }
            params.getStringOrNull("description")?.let { put("description", it) }
            params.getArrayOrNull("layoutGrids")?.let { put("layoutGrids", it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styleId = pluginResponse?.jsonObject?.get("styleId")?.jsonPrimitive?.contentOrNull
        val styleName = pluginResponse?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: params["name"]?.jsonPrimitive?.contentOrNull
            ?: "Grid Style"

        return if (styleId != null) {
            "Successfully created grid style '$styleName' (ID: $styleId)"
        } else {
            "Successfully created grid style '$styleName'"
        }
    }
}
