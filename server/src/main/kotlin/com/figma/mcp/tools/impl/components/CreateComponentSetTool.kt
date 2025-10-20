package com.figma.mcp.tools.impl.components

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
 * Create Component Set Tool
 *
 * ## Purpose
 * Creates a variant set from multiple components.
 *
 * ## Parameters
 * - componentIds (required): Array of component IDs to combine into variant set
 * - name (optional): Name for the component set
 *
 * ## Returns
 * {
 *   componentSetId: string - ID of the created component set
 * }
 */
class CreateComponentSetTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_COMPONENT_SET) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Create a component set (variant group) from multiple components. " +
                    "Component sets allow you to group related component variants together. " +
                    "Requires at least 2 component IDs.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.COMPONENT_IDS to mapOf(
                        "type" to "array",
                        "description" to "Array of component IDs to combine into variant set (required, minimum 2)",
                        "items" to mapOf("type" to "string")
                    ),
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the component set (optional)"
                    )
                ),
                required = listOf(ParamNames.COMPONENT_IDS)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_COMPONENT_SET
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.COMPONENT_IDS, params.getRequiredArray(ParamNames.COMPONENT_IDS))
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val componentSetId = pluginResponse?.jsonObject?.get("componentSetId")
            ?.jsonPrimitive?.content
        val componentCount = params.getRequiredArray(ParamNames.COMPONENT_IDS).size
        return if (componentSetId != null) {
            "Successfully created component set with ID: $componentSetId from $componentCount components"
        } else {
            "Successfully created component set from $componentCount components"
        }
    }
}
