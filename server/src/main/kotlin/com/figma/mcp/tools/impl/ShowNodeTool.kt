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
 * Show Node Tool
 *
 * ## Purpose
 * Scrolls the Figma viewport to show a specific node.
 *
 * ## Parameters
 * - nodeId: string (required) - ID of node to show
 * - zoom: number (optional) - Zoom level to apply
 */
class ShowNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SHOW_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Scrolls the Figma viewport to show a specific node. " +
                    "Optionally sets the zoom level.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to show (required)"
                    ),
                    ParamNames.ZOOM to mapOf(
                        "type" to "number",
                        "description" to "Zoom level to apply (optional)"
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
            put(ParamNames.OPERATION, "showNode")
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            params.getDoubleOrNull(ParamNames.ZOOM)?.let { put(ParamNames.ZOOM, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully navigated to node: $nodeId"
    }
}
