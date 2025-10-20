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
 * Get Parent Tool
 *
 * ## Purpose
 * Gets the parent node of a specific node.
 *
 * ## Parameters
 * - nodeId: string (required) - Child node ID
 *
 * ## Returns
 * NodeInfo object of the parent, or null if node has no parent
 */
class GetParentTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_PARENT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Get the parent node of a specific node. " +
                    "Returns the parent node information, or null if the node has no parent.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the child node (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_PARENT
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
        val parentData = pluginResponse?.jsonObject

        val message = if (parentData == null || parentData["parent"]?.jsonPrimitive?.contentOrNull == null) {
            "Node has no parent (top-level node)"
        } else {
            "Parent node: ${parentData.toString()}"
        }

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
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully retrieved parent information for: $nodeId"
    }
}
