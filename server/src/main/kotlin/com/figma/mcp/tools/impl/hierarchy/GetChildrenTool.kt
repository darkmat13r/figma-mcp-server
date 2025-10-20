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
 * Get Children Tool
 *
 * ## Purpose
 * Gets all children of a specific node.
 *
 * ## Parameters
 * - nodeId: string (required) - Parent node ID
 * - recursive: boolean (optional) - Whether to get all descendants recursively
 *
 * ## Returns
 * {
 *   children: NodeInfo[] - Array of child node information
 * }
 */
class GetChildrenTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_CHILDREN) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get all children of a node. " +
                    "Optionally retrieve all descendants recursively through the entire hierarchy.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the parent node (required)"
                    ),
                    ParamNames.RECURSIVE to mapOf(
                        "type" to "boolean",
                        "description" to "Whether to get all descendants recursively (optional, default: false)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_CHILDREN
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            params.getBooleanOrNull(ParamNames.RECURSIVE)?.let { put(ParamNames.RECURSIVE, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val childrenData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("children", buildJsonArray {})
        }

        val childCount = childrenData["children"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Found ${childCount} child node(s): ${childrenData.toString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val childCount = pluginResponse?.jsonObject?.get("children")?.jsonArray?.size ?: 0
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully retrieved $childCount child node(s) for: $nodeId"
    }
}
