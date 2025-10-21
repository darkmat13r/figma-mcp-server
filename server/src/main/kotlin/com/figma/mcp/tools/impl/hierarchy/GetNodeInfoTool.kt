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
 * Get Node Info Tool
 *
 * ## Purpose
 * Gets detailed information about a specific node, including all its properties,
 * styles, and metadata.
 *
 * ## Parameters
 * - nodeId: string (required) - The ID of the node to get information about
 *
 * ## Returns
 * Complete node information including:
 * - id, name, type
 * - dimensions (x, y, width, height)
 * - fills, strokes, effects
 * - parent and children information
 * - and all other node properties
 */
class GetNodeInfoTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_NODE_INFO) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get detailed node information including all properties, styles, and metadata. " +
                    "Returns complete information about a specific node by its ID.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to get information about (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_NODE_INFO
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the entire node info as JSON
        val nodeInfo = pluginResponse?.jsonObject ?: buildJsonObject {
            put("error", "No node information returned")
        }

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = nodeInfo.toJsonString())
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully retrieved node information for: $nodeId"
    }
}
