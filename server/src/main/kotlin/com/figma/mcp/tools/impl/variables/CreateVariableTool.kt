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
 * Normalize a variable value - convert string numbers to actual numbers
 */
private fun normalizeVariableValue(value: JsonElement): JsonElement {
    return when (value) {
        is JsonPrimitive -> {
            if (value.isString) {
                val stringValue = value.content
                // Try to parse as number
                val numericValue = stringValue.toDoubleOrNull()
                if (numericValue != null) {
                    JsonPrimitive(numericValue)
                } else {
                    // Try to parse as boolean
                    val boolValue = stringValue.toBooleanStrictOrNull()
                    if (boolValue != null) {
                        JsonPrimitive(boolValue)
                    } else {
                        // Keep as string (might be hex color, etc.)
                        value
                    }
                }
            } else {
                value
            }
        }
        else -> value
    }
}

/**
 * Create Variable Tool
 *
 * ## Purpose
 * Creates a design token variable within a collection.
 *
 * ## Parameters
 * - name (required): Name of the variable
 * - collectionId (required): ID of the collection to add variable to
 * - type (required): Variable type - "BOOLEAN" | "FLOAT" | "STRING" | "COLOR"
 * - values (optional): Record<string, any> - values per mode (modeId -> value)
 *
 * ## Returns
 * {
 *   variableId: string - ID of the created variable
 * }
 */
class CreateVariableTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_VARIABLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Create a design token variable within a collection. " +
                    "Variables can store values like colors, numbers, strings, or booleans. " +
                    "Variable types: BOOLEAN, FLOAT, STRING, COLOR. " +
                    "Optionally provide values per mode.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name of the variable (required)"
                    ),
                    ParamNames.COLLECTION_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the collection to add variable to (required)"
                    ),
                    ParamNames.TYPE to mapOf(
                        "type" to "string",
                        "description" to "Variable type (required): BOOLEAN, FLOAT, STRING, or COLOR",
                        "enum" to listOf("BOOLEAN", "FLOAT", "STRING", "COLOR")
                    ),
                    ParamNames.VALUES to mapOf(
                        "type" to "object",
                        "description" to "Record<string, any> - Values per mode (optional). " +
                                "Keys are mode IDs, values are the variable value for that mode. " +
                                "For COLOR variables, values support multiple formats:\n" +
                                "  * Object: {r: 0-1, g: 0-1, b: 0-1, a?: 0-1}\n" +
                                "  * Hex string: \"#RRGGBB\" or \"#RGB\"\n" +
                                "  * Array: [r, g, b] where values are 0-255 or 0-1"
                    )
                ),
                required = listOf(ParamNames.NAME, ParamNames.COLLECTION_ID, ParamNames.TYPE)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_VARIABLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NAME, params.getRequiredString(ParamNames.NAME))
            put(ParamNames.COLLECTION_ID, params.getRequiredString(ParamNames.COLLECTION_ID))
            put(ParamNames.TYPE, params.getRequiredString(ParamNames.TYPE))

            // Handle values - parse if it's a JSON string and normalize values
            params[ParamNames.VALUES]?.let { values ->
                when (values) {
                    is JsonPrimitive -> {
                        if (values.isString) {
                            val stringValue = values.content
                            if (stringValue.trim().startsWith("{")) {
                                try {
                                    val parsedValue = Json.parseToJsonElement(stringValue) as? JsonObject
                                    if (parsedValue != null) {
                                        // Normalize the values in the object
                                        val normalizedValues = buildJsonObject {
                                            parsedValue.forEach { (key, value) ->
                                                put(key, normalizeVariableValue(value))
                                            }
                                        }
                                        put(ParamNames.VALUES, normalizedValues)
                                    }
                                } catch (e: Exception) {
                                    // If parsing fails, skip it
                                    logger.warn("Failed to parse values JSON string: ${e.message}")
                                }
                            }
                        }
                    }
                    is JsonObject -> {
                        // Normalize the values in the object
                        val normalizedValues = buildJsonObject {
                            values.forEach { (key, value) ->
                                put(key, normalizeVariableValue(value))
                            }
                        }
                        put(ParamNames.VALUES, normalizedValues)
                    }
                    else -> {
                        // Unexpected type, log warning
                        logger.warn("Unexpected values type: ${values::class.simpleName}")
                    }
                }
            }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val responseObj = pluginResponse?.jsonObject
        val variableId = responseObj?.get("variableId")?.jsonPrimitive?.content
        val valuesSet = responseObj?.get("valuesSet")?.jsonObject
        val errors = responseObj?.get("errors")?.jsonArray

        val message = StringBuilder()

        if (variableId != null) {
            message.append("Successfully created variable with ID: $variableId")
        } else {
            message.append("Successfully created variable")
        }

        // Add information about values that were set
        if (valuesSet != null && valuesSet.isNotEmpty()) {
            val successfulModes = valuesSet.entries.count { it.value.jsonPrimitive.boolean }
            val totalModes = valuesSet.size

            message.append("\nValues set: $successfulModes/$totalModes modes")

            // List which modes succeeded/failed
            valuesSet.entries.forEach { (modeId, success) ->
                val status = if (success.jsonPrimitive.boolean) "✓" else "✗"
                message.append("\n  $status Mode $modeId")
            }
        }

        // Add any errors encountered
        if (errors != null && errors.isNotEmpty()) {
            message.append("\n\nWarnings/Errors:")
            errors.forEach { error ->
                message.append("\n  • ${error.jsonPrimitive.content}")
            }
        }

        return message.toString()
    }
}
