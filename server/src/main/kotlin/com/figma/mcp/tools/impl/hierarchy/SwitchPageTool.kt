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
 * Switch Page Tool
 *
 * ## Purpose
 * Switches to a different page in the Figma document.
 *
 * ## Parameters
 * - pageId: string (optional) - Page ID to switch to
 * - pageName: string (optional) - Page name to switch to
 *
 * Note: Either pageId OR pageName must be provided (not both)
 *
 * ## Returns
 * {
 *   pageId: string,
 *   pageName: string,
 *   message: string
 * }
 */
class SwitchPageTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SWITCH_PAGE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Switch to a different page in the Figma document. " +
                    "Provide either pageId or pageName to identify the target page.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.PAGE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the page to switch to (optional, use either pageId or pageName)"
                    ),
                    ParamNames.PAGE_NAME to mapOf(
                        "type" to "string",
                        "description" to "Name of the page to switch to (optional, use either pageId or pageName)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SWITCH_PAGE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull(ParamNames.PAGE_ID)?.let { put(ParamNames.PAGE_ID, it) }
            params.getStringOrNull(ParamNames.PAGE_NAME)?.let { put(ParamNames.PAGE_NAME, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val pageData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("message", "Page switched successfully")
        }

        val pageName = pageData["pageName"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val pageId = pageData["pageId"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val message = pageData["message"]?.jsonPrimitive?.contentOrNull
            ?: "Switched to page: $pageName (ID: $pageId)"

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val pageName = pluginResponse?.jsonObject?.get("pageName")?.jsonPrimitive?.contentOrNull
            ?: params[ParamNames.PAGE_NAME]?.jsonPrimitive?.contentOrNull
            ?: "specified page"
        return "Successfully switched to page: $pageName"
    }
}
