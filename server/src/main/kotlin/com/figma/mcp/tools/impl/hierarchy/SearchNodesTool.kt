package com.figma.mcp.tools.impl.hierarchy

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
 * Search Nodes Tool
 *
 * ## Purpose
 * Advanced search for nodes by text content, node ID, or component instance.
 * Can search in current page only or across the entire document.
 *
 * ## Parameters
 * - searchText: string (optional) - Text to search for in node names and text content
 * - nodeId: string (optional) - Specific node ID to find
 * - componentId: string (optional) - Find all instances of a component
 * - searchInCurrentPageOnly: boolean (optional) - Limit search to current page (default: false)
 *
 * ## Returns
 * {
 *   nodes: NodeInfo[] - Array of matching nodes
 *   totalFound: number - Total number of matches
 * }
 */
class SearchNodesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SEARCH_NODES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Advanced search by text, ID, or component instance. " +
                    "Search in node names and text content. " +
                    "Optionally limit to current page or search entire document.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.SEARCH_TEXT to mapOf(
                        "type" to "string",
                        "description" to "Text to search for in node names and text content (optional)"
                    ),
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "Specific node ID to find (optional)"
                    ),
                    ParamNames.COMPONENT_ID to mapOf(
                        "type" to "string",
                        "description" to "Find all instances of a component (optional)"
                    ),
                    ParamNames.SEARCH_IN_CURRENT_PAGE_ONLY to mapOf(
                        "type" to "boolean",
                        "description" to "Limit search to current page only (optional, default: false)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SEARCH_NODES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull(ParamNames.SEARCH_TEXT)?.let { put(ParamNames.SEARCH_TEXT, it) }
            params.getStringOrNull(ParamNames.NODE_ID)?.let { put(ParamNames.NODE_ID, it) }
            params.getStringOrNull(ParamNames.COMPONENT_ID)?.let { put(ParamNames.COMPONENT_ID, it) }
            params.getBooleanOrNull(ParamNames.SEARCH_IN_CURRENT_PAGE_ONLY)?.let {
                put(ParamNames.SEARCH_IN_CURRENT_PAGE_ONLY, it)
            }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val searchData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("nodes", buildJsonArray {})
            put("totalFound", 0)
        }

        val totalFound = searchData["totalFound"]?.jsonPrimitive?.intOrNull ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Search results (${totalFound} found): ${searchData.toString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val totalFound = pluginResponse?.jsonObject?.get("totalFound")?.jsonPrimitive?.intOrNull ?: 0
        return "Successfully completed search. Found $totalFound matching node(s)"
    }
}
