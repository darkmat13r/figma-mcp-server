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
 * Set Locked Tool
 *
 * ## Purpose
 * Locks or unlocks a node to prevent/allow editing.
 *
 * ## Parameters
 * - nodeId: string (required) - ID of node to lock/unlock
 * - locked: boolean (required) - true to lock, false to unlock
 */
class SetLockedTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_LOCKED) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Locks or unlocks a node to prevent or allow editing. " +
                    "Locked nodes cannot be moved, resized, or modified.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to lock/unlock (required)"
                    ),
                    ParamNames.LOCKED to mapOf(
                        "type" to "boolean",
                        "description" to "true to lock, false to unlock (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.LOCKED)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UTILITY_OPERATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, "setLocked")
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.LOCKED, params.getRequiredBoolean(ParamNames.LOCKED))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val locked = params[ParamNames.LOCKED]?.jsonPrimitive?.booleanOrNull
        val action = if (locked == true) "locked" else "unlocked"
        return "Successfully $action node: $nodeId"
    }
}
