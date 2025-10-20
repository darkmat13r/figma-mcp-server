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
 * Set Instance Properties Tool
 *
 * ## Purpose
 * Overrides component property values on an instance.
 *
 * ## Parameters
 * - instanceId (required): ID of the instance
 * - properties (required): Record<string, any> - key-value pairs of property overrides
 *
 * ## Returns
 * Success message
 */
class SetInstancePropertiesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_INSTANCE_PROPERTIES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Override component property values on an instance. " +
                    "Properties must be defined on the master component. " +
                    "Pass a key-value object where keys are property names and values are the new property values.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.INSTANCE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the instance to modify (required)"
                    ),
                    ParamNames.PROPERTIES to mapOf(
                        "type" to "object",
                        "description" to "Record<string, any> - Property overrides as key-value pairs (required). " +
                                "Keys are property names, values are the new values to set."
                    )
                ),
                required = listOf(ParamNames.INSTANCE_ID, ParamNames.PROPERTIES)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_INSTANCE_PROPERTIES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.INSTANCE_ID, params.getRequiredString(ParamNames.INSTANCE_ID))
            put(ParamNames.PROPERTIES, params.getObjectOrNull(ParamNames.PROPERTIES) ?: buildJsonObject {})
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val propertyCount = params.getObjectOrNull(ParamNames.PROPERTIES)?.keys?.size ?: 0
        return "Successfully set $propertyCount instance properties"
    }
}
