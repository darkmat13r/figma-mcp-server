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
 * Find Nodes Tool
 *
 * ## Purpose
 * Search for nodes by various criteria including name, type, and parent.
 *
 * ## Parameters
 * - name: string (optional) - Node name or regex pattern to match
 * - type: string or string[] (optional) - Node type(s) to filter by
 * - parentId: string (optional) - Parent node ID to search within
 * - recursive: boolean (optional) - Whether to search recursively (default: true)
 *
 * ## Returns
 * {
 *   nodes: NodeInfo[] - Array of matching nodes
 * }
 */
class FindNodesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.FIND_NODES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Search for nodes by criteria such as name (supports regex), type, and parent. " +
                    "Supports filtering by multiple types and recursive searching within containers.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Node name or regex pattern to match (optional)"
                    ),
                    ParamNames.TYPE to mapOf(
                        "type" to "string",
                        "description" to "Node type or array of types to filter by (optional). " +
                                "Examples: 'FRAME', 'TEXT', ['FRAME', 'COMPONENT']"
                    ),
                    ParamNames.PARENT_ID to mapOf(
                        "type" to "string",
                        "description" to "Parent node ID to search within (optional)"
                    ),
                    ParamNames.RECURSIVE to mapOf(
                        "type" to "boolean",
                        "description" to "Whether to search recursively through children (optional, default: true)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.FIND_NODES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }

            // Handle type as either string or array
            val typeValue = params["type"]
            when (typeValue) {
                is JsonArray -> {
                    putJsonArray("type") {
                        typeValue.forEach { add(it) }
                    }
                }
                is JsonPrimitive -> {
                    put("type", typeValue.content)
                }
                else -> {
                    // Do nothing if type is not provided
                }
            }

            params.getStringOrNull(ParamNames.PARENT_ID)?.let { put(ParamNames.PARENT_ID, it) }
            params.getBooleanOrNull(ParamNames.RECURSIVE)?.let { put(ParamNames.RECURSIVE, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val nodesData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("nodes", buildJsonArray {})
        }

        val nodeCount = nodesData["nodes"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Found ${nodeCount} node(s): ${nodesData.toJsonString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeCount = pluginResponse?.jsonObject?.get("nodes")?.jsonArray?.size ?: 0
        return "Successfully found $nodeCount matching node(s)"
    }
}
