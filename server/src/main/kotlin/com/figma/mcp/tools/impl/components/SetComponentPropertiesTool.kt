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
 * Set Component Properties Tool
 *
 * ## Purpose
 * Defines component properties for a component master.
 *
 * ## Parameters
 * - componentId (required): ID of the component
 * - properties (required): Array of ComponentPropertyDefinition objects
 *
 * ComponentPropertyDefinition:
 * - name: string
 * - type: "BOOLEAN" | "TEXT" | "INSTANCE_SWAP" | "VARIANT"
 * - defaultValue?: any
 * - variantOptions?: string[]
 *
 * ## Returns
 * Success message
 */
class SetComponentPropertiesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_COMPONENT_PROPERTIES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Define component properties for a component. " +
                    "Properties allow instances to be customized. " +
                    "Property types: BOOLEAN, TEXT, INSTANCE_SWAP, VARIANT. " +
                    "Each property has name, type, and optional defaultValue/variantOptions.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.COMPONENT_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the component to add properties to (required)"
                    ),
                    ParamNames.PROPERTIES to mapOf(
                        "type" to "array",
                        "description" to "Array of ComponentPropertyDefinition objects (required). Each must have 'name' and 'type' fields. " +
                                "Type can be: BOOLEAN, TEXT, INSTANCE_SWAP, or VARIANT. " +
                                "Optional fields: 'defaultValue' (any type), 'variantOptions' (array of strings).",
                        "items" to mapOf("type" to "object")
                    )
                ),
                required = listOf(ParamNames.COMPONENT_ID, ParamNames.PROPERTIES)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_COMPONENT_PROPERTIES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.COMPONENT_ID, params.getRequiredString(ParamNames.COMPONENT_ID))
            put(ParamNames.PROPERTIES, params.getRequiredArray(ParamNames.PROPERTIES))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val propertyCount = params.getRequiredArray(ParamNames.PROPERTIES).size
        return "Successfully set $propertyCount component properties"
    }
}
