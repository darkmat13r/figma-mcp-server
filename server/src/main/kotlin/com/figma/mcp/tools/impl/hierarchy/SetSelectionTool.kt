package com.figma.mcp.tools.impl.hierarchy

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
 * Set Selection Tool
 *
 * ## Purpose
 * Changes the current selection to the specified nodes.
 *
 * ## Parameters
 * - nodeIds: string[] (required) - Array of node IDs to select
 */
class SetSelectionTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_SELECTION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Change selected nodes in Figma. " +
                    "Takes an array of node IDs and sets them as the current selection.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_IDS to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "Array of node IDs to select (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_IDS)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_SELECTION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            putJsonArray(ParamNames.NODE_IDS) {
                params.getRequiredStringArray(ParamNames.NODE_IDS).forEach { add(it) }
            }
        }
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeCount = params[ParamNames.NODE_IDS]?.jsonArray?.size ?: 0
        return "Successfully selected $nodeCount node(s)"
    }
}
