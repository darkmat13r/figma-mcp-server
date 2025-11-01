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
                        "description" to "ID of the variable to update (required). Format: \"VariableID:\" followed by mode and index (e.g., \"VariableID:1:65\"). Use figma_get_variables to get available variable IDs."
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

            // Handle value - if it's a JSON string, parse it to actual JSON
            params[ParamNames.VALUE]?.let { value ->
                when (value) {
                    is JsonPrimitive -> {
                        if (value.isString) {
                            // Try to parse as JSON if it looks like a JSON object/array
                            val stringValue = value.content
                            if (stringValue.trim().startsWith("{") || stringValue.trim().startsWith("[")) {
                                try {
                                    val parsedValue = Json.parseToJsonElement(stringValue)
                                    put(ParamNames.VALUE, parsedValue)
                                } catch (e: Exception) {
                                    // If parsing fails, use as-is (might be a string variable)
                                    put(ParamNames.VALUE, value)
                                }
                            } else {
                                // Try to parse as number first
                                val numericValue = stringValue.toDoubleOrNull()
                                if (numericValue != null) {
                                    put(ParamNames.VALUE, JsonPrimitive(numericValue))
                                } else {
                                    // Try to parse as boolean
                                    val boolValue = stringValue.toBooleanStrictOrNull()
                                    if (boolValue != null) {
                                        put(ParamNames.VALUE, JsonPrimitive(boolValue))
                                    } else {
                                        // Regular string value
                                        put(ParamNames.VALUE, value)
                                    }
                                }
                            }
                        } else {
                            // Number or boolean
                            put(ParamNames.VALUE, value)
                        }
                    }
                    else -> {
                        // Already a JsonObject or JsonArray
                        put(ParamNames.VALUE, value)
                    }
                }
            }

            params.getStringOrNull(ParamNames.MODE_ID)?.let { put(ParamNames.MODE_ID, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        return "Successfully updated variable value"
    }
}
