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
 * Create Page Tool
 *
 * ## Purpose
 * Creates a new page in the Figma document.
 *
 * ## Parameters
 * - name (optional string) - Page name, defaults to "Page N"
 * - switchToPage (optional boolean) - Whether to switch to the newly created page (default: true)
 *
 * ## Returns
 * {
 *   pageId: string,
 *   pageName: string,
 *   message: string
 * }
 */
class CreatePageTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_PAGE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a new page in the Figma document. " +
                    "Optionally specify a custom name and whether to switch to the new page immediately.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the new page (optional, defaults to 'Page N')"
                    ),
                    ParamNames.SWITCH_TO_PAGE to mapOf(
                        "type" to "boolean",
                        "description" to "Whether to switch to the newly created page (optional, default: true)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_PAGE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }
            params.getBooleanOrNull(ParamNames.SWITCH_TO_PAGE)?.let {
                put(ParamNames.SWITCH_TO_PAGE, it)
            }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val pageData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("message", "Page created successfully")
        }

        val pageName = pageData["pageName"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val pageId = pageData["pageId"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val message = pageData["message"]?.jsonPrimitive?.contentOrNull
            ?: "Created new page: $pageName (ID: $pageId)"

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
            ?: params[ParamNames.NAME]?.jsonPrimitive?.contentOrNull
            ?: "new page"
        return "Successfully created page: $pageName"
    }
}
