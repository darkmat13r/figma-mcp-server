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
 * Set Visible Tool
 *
 * ## Purpose
 * Shows or hides a node in the Figma canvas.
 *
 * ## Parameters
 * - nodeId: string (required) - ID of node to show/hide
 * - visible: boolean (required) - true to show, false to hide
 */
class SetVisibleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_VISIBLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Shows or hides a node in the Figma canvas. " +
                    "Hidden nodes are not visible in the final design but remain in the layers panel.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to show/hide (required)"
                    ),
                    ParamNames.VISIBLE to mapOf(
                        "type" to "boolean",
                        "description" to "true to show, false to hide (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.VISIBLE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UTILITY_OPERATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, "setVisible")
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.VISIBLE, params.getRequiredBoolean(ParamNames.VISIBLE))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        val visible = params[ParamNames.VISIBLE]?.jsonPrimitive?.booleanOrNull
        val action = if (visible == true) "shown" else "hidden"
        return "Successfully $action node: $nodeId"
    }
}
