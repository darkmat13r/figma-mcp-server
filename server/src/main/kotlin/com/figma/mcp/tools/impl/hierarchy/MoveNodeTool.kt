package com.figma.mcp.tools.impl.hierarchy

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
 * Move Node Tool
 *
 * ## Purpose
 * Moves a node to a different parent in the hierarchy.
 *
 * ## Parameters
 * - nodeId: string (required) - Node to move
 * - newParentId: string (required) - New parent node
 * - index: number (optional) - Position within new parent's children
 */
class MoveNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.MOVE_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Move a node to a different parent in the hierarchy. " +
                    "Optionally specify the position within the new parent's children array.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to move (required)"
                    ),
                    ParamNames.NEW_PARENT_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the new parent node (required)"
                    ),
                    ParamNames.INDEX to mapOf(
                        "type" to "number",
                        "description" to "Position within new parent's children (optional, default: end)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.NEW_PARENT_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.MOVE_NODE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.NEW_PARENT_ID, params.getRequiredString(ParamNames.NEW_PARENT_ID))
            params.getDoubleOrNull(ParamNames.INDEX)?.let { put(ParamNames.INDEX, it.toInt()) }
        }
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val newParentId = params[ParamNames.NEW_PARENT_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully moved node $nodeId to parent $newParentId"
    }
}
