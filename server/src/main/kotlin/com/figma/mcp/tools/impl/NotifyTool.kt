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
 * Notify Tool
 *
 * ## Purpose
 * Shows a notification message to the user in Figma.
 *
 * ## Parameters
 * - message: string (required) - Message to display
 * - timeout: number (optional) - Duration in milliseconds
 * - error: boolean (optional) - true for error styling
 */
class NotifyTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.NOTIFY) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Shows a notification message to the user in Figma. " +
                    "Useful for providing feedback on operations or displaying information.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.MESSAGE to mapOf(
                        "type" to "string",
                        "description" to "Message to display to the user (required)"
                    ),
                    ParamNames.TIMEOUT to mapOf(
                        "type" to "number",
                        "description" to "Duration in milliseconds (optional, default: 3000)"
                    ),
                    ParamNames.ERROR to mapOf(
                        "type" to "boolean",
                        "description" to "true for error styling, false for normal (optional, default: false)"
                    )
                ),
                required = listOf(ParamNames.MESSAGE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UTILITY_OPERATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, "notify")
            put(ParamNames.MESSAGE, params.getRequiredString(ParamNames.MESSAGE))
            params.getDoubleOrNull(ParamNames.TIMEOUT)?.let { put(ParamNames.TIMEOUT, it.toInt()) }
            params.getBooleanOrNull(ParamNames.ERROR)?.let { put(ParamNames.ERROR, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val message = params[ParamNames.MESSAGE]?.jsonPrimitive?.contentOrNull
        return "Successfully displayed notification: $message"
    }
}
