package com.figma.mcp.tools.impl.variables

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
 * Set Variable Value Tool
 *
 * ## Purpose
 * Updates the value of a variable, optionally for a specific mode.
 *
 * ## Parameters
 * - variableId (required): ID of the variable to update
 * - value (required): New value for the variable
 * - modeId (optional): Mode ID to set value for (defaults to first mode)
 *
 * ## Returns
 * Success message
 */
class SetVariableValueTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_VARIABLE_VALUE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Update the value of a variable. " +
                    "Can set value for a specific mode by providing modeId. " +
                    "Value type must match the variable type (boolean, number, string, or color object).",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.VARIABLE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the variable to update (required)"
                    ),
                    ParamNames.VALUE to mapOf(
                        "description" to "New value for the variable (required). " +
                                "Type must match variable type:\n" +
                                "  - BOOLEAN: true/false\n" +
                                "  - FLOAT: number\n" +
                                "  - STRING: text\n" +
                                "  - COLOR: Supports multiple formats:\n" +
                                "    * Object: {r: 0-1, g: 0-1, b: 0-1, a?: 0-1}\n" +
                                "    * Hex string: \"#RRGGBB\" or \"#RGB\"\n" +
                                "    * Array: [r, g, b] where values are 0-255 or 0-1"
                    ),
                    ParamNames.MODE_ID to mapOf(
                        "type" to "string",
                        "description" to "Mode ID to set value for (optional, defaults to first mode)"
                    )
                ),
                required = listOf(ParamNames.VARIABLE_ID, ParamNames.VALUE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_VARIABLE_VALUE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.VARIABLE_ID, params.getRequiredString(ParamNames.VARIABLE_ID))
            // Pass value as-is (can be any JSON type)
            params[ParamNames.VALUE]?.let { put(ParamNames.VALUE, it) }
            params.getStringOrNull(ParamNames.MODE_ID)?.let { put(ParamNames.MODE_ID, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        return "Successfully updated variable value"
    }
}
