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
 * Get All Pages Tool
 *
 * ## Purpose
 * Gets a list of all pages in the current Figma document.
 *
 * ## Parameters
 * None
 *
 * ## Returns
 * {
 *   pages: Array<{
 *     id: string,
 *     name: string,
 *     isCurrent: boolean
 *   }>
 * }
 */
class GetAllPagesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_ALL_PAGES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get list of all pages in the current Figma document. " +
                    "Returns page IDs, names, and indicates which page is currently active.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_ALL_PAGES
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
        val pagesData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("pages", buildJsonArray {})
        }

        val pageCount = pagesData["pages"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Document pages (${pageCount}): ${pagesData.toJsonString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val pageCount = pluginResponse?.jsonObject?.get("pages")?.jsonArray?.size ?: 0
        return "Successfully retrieved $pageCount page(s)"
    }
}
