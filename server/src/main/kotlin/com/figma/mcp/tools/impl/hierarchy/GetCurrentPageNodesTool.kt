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
 * Get Current Page Nodes Tool
 *
 * ## Purpose
 * Gets all top-level nodes on the current page.
 *
 * ## Parameters
 * None - operates on the current page
 *
 * ## Returns
 * {
 *   pageId: string,
 *   pageName: string,
 *   nodes: NodeInfo[] - Array of top-level nodes on the page
 * }
 */
class GetCurrentPageNodesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_CURRENT_PAGE_NODES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get all top-level nodes on the current page. " +
                    "Returns page information along with an array of all top-level nodes.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_CURRENT_PAGE_NODES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            // No parameters needed
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val pageData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("nodes", buildJsonArray {})
        }

        val nodeCount = pageData["nodes"]?.jsonArray?.size ?: 0
        val pageName = pageData["pageName"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(
                    text = "Current page: $pageName with ${nodeCount} top-level node(s)\n${pageData.toString()}"
                )
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeCount = pluginResponse?.jsonObject?.get("nodes")?.jsonArray?.size ?: 0
        val pageName = pluginResponse?.jsonObject?.get("pageName")?.jsonPrimitive?.contentOrNull ?: "Unknown"
        return "Successfully retrieved $nodeCount top-level node(s) from page: $pageName"
    }
}
