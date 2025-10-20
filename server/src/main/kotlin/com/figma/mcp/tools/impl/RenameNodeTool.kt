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
 * Rename Node Tool
 *
 * ## Purpose
 * Changes the name of a node in the Figma layers panel.
 *
 * ## Parameters
 * - nodeId: string (required) - ID of node to rename
 * - name: string (required) - New name for the node
 */
class RenameNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.RENAME_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Changes the name of a node in the Figma layers panel. " +
                    "Useful for organizing and identifying nodes.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to rename (required)"
                    ),
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "New name for the node (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.NAME)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UTILITY_OPERATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, "renameNode")
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.NAME, params.getRequiredString(ParamNames.NAME))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val name = params[ParamNames.NAME]?.jsonPrimitive?.contentOrNull
        return "Successfully renamed node $nodeId to: $name"
    }
}
