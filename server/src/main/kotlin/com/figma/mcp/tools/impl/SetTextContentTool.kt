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
 * Set Text Content Tool
 *
 * ## Purpose
 * Changes the text content of a text node.
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - characters: string (required) - New text content to display
 */
class SetTextContentTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_TEXT_CONTENT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Changes the text content of a text node in Figma. " +
                    "Use this to update the displayed text in any text layer.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to modify (required)"
                    ),
                    ParamNames.CHARACTERS to mapOf(
                        "type" to "string",
                        "description" to "New text content to display (required)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.CHARACTERS)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_TEXT_CONTENT
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.CHARACTERS, params.getRequiredString(ParamNames.CHARACTERS))
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set text content for node: $nodeId"
    }
}
