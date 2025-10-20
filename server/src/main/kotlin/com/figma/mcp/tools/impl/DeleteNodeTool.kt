package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Delete Node Tool
 *
 * ## Purpose
 * Removes a node from the Figma document.
 *
 * ## Parameters
 * - nodeId: string (required) - ID of node to delete
 */
class DeleteNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.DELETE_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Deletes a node from the Figma document. " +
                    "This operation cannot be undone via the MCP API.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to delete (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UTILITY_OPERATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, "deleteNode")
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully deleted node: $nodeId"
    }
}
