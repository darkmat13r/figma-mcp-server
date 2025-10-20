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
 * Add Variant Tool
 *
 * ## Purpose
 * Adds a new variant to an existing component set.
 *
 * ## Parameters
 * - componentSetId (required): ID of the component set
 * - properties (required): Record<string, string> - variant property values
 *
 * ## Returns
 * {
 *   variantId: string - ID of the created variant component
 * }
 */
class AddVariantTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.ADD_VARIANT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Add a new variant to an existing component set. " +
                    "Provide property values as key-value pairs to define the new variant. " +
                    "Example: {\"Size\": \"Large\", \"State\": \"Hover\"}",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.COMPONENT_SET_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the component set to add variant to (required)"
                    ),
                    ParamNames.PROPERTIES to mapOf(
                        "type" to "object",
                        "description" to "Record<string, string> - Variant property values as key-value pairs (required). " +
                                "Example: {\"Size\": \"Large\", \"State\": \"Hover\"}"
                    )
                ),
                required = listOf(ParamNames.COMPONENT_SET_ID, ParamNames.PROPERTIES)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.ADD_VARIANT
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.COMPONENT_SET_ID, params.getRequiredString(ParamNames.COMPONENT_SET_ID))
            put(ParamNames.PROPERTIES, params.getObjectOrNull(ParamNames.PROPERTIES) ?: buildJsonObject {})
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val variantId = pluginResponse?.jsonObject?.get("variantId")
            ?.jsonPrimitive?.content
        return if (variantId != null) {
            "Successfully added variant with ID: $variantId"
        } else {
            "Successfully added variant to component set"
        }
    }
}
