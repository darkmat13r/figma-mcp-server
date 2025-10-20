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
 * Unbind Variable Tool
 *
 * ## Purpose
 * Removes a variable binding from a node property.
 *
 * ## Parameters
 * - nodeId (required): ID of the node
 * - field (required): Property to unbind (e.g., "fills", "opacity")
 *
 * ## Returns
 * Success message
 */
class UnbindVariableTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.UNBIND_VARIABLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Remove a variable binding from a node property. " +
                    "After unbinding, the property will use a static value instead of being controlled by a variable.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to unbind variable from (required)"
                    ),
                    ParamNames.FIELD to mapOf(
                        "type" to "string",
                        "description" to "Property to unbind variable from (required, e.g., 'fills', 'opacity')"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.FIELD)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.UNBIND_VARIABLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.FIELD, params.getRequiredString(ParamNames.FIELD))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val field = params.getRequiredString(ParamNames.FIELD)
        return "Successfully unbound variable from $field property"
    }
}
