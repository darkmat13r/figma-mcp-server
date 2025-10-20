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
                                "Keys are mode IDs, values are the variable value for that mode."
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
            params.getObjectOrNull(ParamNames.VALUES)?.let { put(ParamNames.VALUES, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val variableId = pluginResponse?.jsonObject?.get("variableId")
            ?.jsonPrimitive?.content
        return if (variableId != null) {
            "Successfully created variable with ID: $variableId"
        } else {
            "Successfully created variable"
        }
    }
}
