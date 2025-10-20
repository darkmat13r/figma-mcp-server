package com.figma.mcp.tools.impl.hierarchy

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
 * Get Selection Tool
 *
 * ## Purpose
 * Gets the currently selected nodes in the Figma canvas.
 *
 * ## Parameters
 * None - operates on the current selection
 *
 * ## Returns
 * {
 *   nodes: NodeInfo[] - Array of selected node information
 * }
 */
class GetSelectionTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_SELECTION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get currently selected nodes in Figma. " +
                    "Returns an array of node information for all selected nodes.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_SELECTION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            // No parameters needed for get selection
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the selection data as JSON
        val selectionData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("nodes", buildJsonArray {})
        }

        val nodeCount = selectionData["nodes"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Selected nodes (${nodeCount}): ${selectionData.toString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeCount = pluginResponse?.jsonObject?.get("nodes")?.jsonArray?.size ?: 0
        return "Successfully retrieved $nodeCount selected node(s)"
    }
}
