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
 * Create Component From Node Tool
 *
 * ## Purpose
 * Converts an existing node into a component master.
 *
 * ## Parameters
 * - nodeId (required): ID of the node to convert to component
 * - name (optional): Name for the component
 * - description (optional): Description for the component
 *
 * ## Returns
 * {
 *   componentId: string - ID of the created component
 * }
 */
class CreateComponentFromNodeTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_COMPONENT_FROM_NODE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Convert an existing node to a component. " +
                    "The node becomes a reusable component master that can be instantiated.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to convert to component (required)"
                    ),
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the component (optional)"
                    ),
                    ParamNames.DESCRIPTION to mapOf(
                        "type" to "string",
                        "description" to "Description of the component's purpose (optional)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_COMPONENT_FROM_NODE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }
            params.getStringOrNull(ParamNames.DESCRIPTION)?.let { put(ParamNames.DESCRIPTION, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val componentId = pluginResponse?.jsonObject?.get("componentId")
            ?.jsonPrimitive?.content
        return if (componentId != null) {
            "Successfully created component with ID: $componentId"
        } else {
            "Successfully converted node to component"
        }
    }
}
