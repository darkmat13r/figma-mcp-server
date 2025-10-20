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
 * Create Variable Collection Tool
 *
 * ## Purpose
 * Creates a new variable collection for organizing design tokens.
 *
 * ## Parameters
 * - name (required): Name of the collection
 * - modes (optional): Array of mode names (e.g., ["Light", "Dark"])
 *
 * ## Returns
 * {
 *   collectionId: string - ID of the created collection
 * }
 */
class CreateVariableCollectionTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_VARIABLE_COLLECTION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Create a variable collection for organizing design tokens. " +
                    "Collections can have multiple modes (e.g., Light/Dark themes). " +
                    "Variables within the collection can have different values per mode.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name of the collection (required)"
                    ),
                    ParamNames.MODES to mapOf(
                        "type" to "array",
                        "description" to "Array of mode names (optional, e.g., [\"Light\", \"Dark\"])",
                        "items" to mapOf("type" to "string")
                    )
                ),
                required = listOf(ParamNames.NAME)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_VARIABLE_COLLECTION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NAME, params.getRequiredString(ParamNames.NAME))
            params.getArrayOrNull(ParamNames.MODES)?.let { put(ParamNames.MODES, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val collectionId = pluginResponse?.jsonObject?.get("collectionId")
            ?.jsonPrimitive?.content
        return if (collectionId != null) {
            "Successfully created variable collection with ID: $collectionId"
        } else {
            "Successfully created variable collection"
        }
    }
}
