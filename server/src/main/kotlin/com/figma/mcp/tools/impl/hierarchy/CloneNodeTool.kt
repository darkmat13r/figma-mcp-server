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
 * Clone Node Tool
 *
 * ## Purpose
 * Duplicates a node, creating an exact copy.
 *
 * ## Parameters
 * - nodeId: string (required) - Node to clone
 * - deep: boolean (optional) - Whether to clone children recursively (default: true)
 *
 * ## Returns
 * {
 *   clonedNodeId: string - ID of the newly created clone
 * }
 */
class CloneNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CLONE_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Duplicate a node, creating an exact copy. " +
                    "Optionally perform a deep clone that includes all children.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to clone (required)"
                    ),
                    ParamNames.DEEP to mapOf(
                        "type" to "boolean",
                        "description" to "Whether to clone children recursively (optional, default: true)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CLONE_NODE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            params.getBooleanOrNull(ParamNames.DEEP)?.let { put(ParamNames.DEEP, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val cloneData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("error", "No clone information returned")
        }

        val clonedNodeId = cloneData["clonedNodeId"]?.jsonPrimitive?.contentOrNull

        val message = if (clonedNodeId != null) {
            "Successfully cloned node. New node ID: $clonedNodeId"
        } else {
            "Node cloned successfully"
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
        val clonedNodeId = pluginResponse?.jsonObject?.get("clonedNodeId")?.jsonPrimitive?.contentOrNull
        return if (clonedNodeId != null) {
            "Successfully cloned node $nodeId -> $clonedNodeId"
        } else {
            "Successfully cloned node $nodeId"
        }
    }
}
