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
 * Bind Variable Tool
 *
 * ## Purpose
 * Binds a variable to a node property (e.g., fills, opacity).
 *
 * ## Parameters
 * - nodeId (required): ID of the node
 * - field (required): Property to bind (e.g., "fills", "opacity", "width")
 * - variableId (required): ID of the variable to bind
 *
 * ## Returns
 * Success message
 */
class BindVariableTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.BIND_VARIABLE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Bind a variable to a node property. " +
                    "This allows the property value to be controlled by a variable, " +
                    "enabling design tokens and theming. " +
                    "Common fields: fills, strokes, opacity, width, height, cornerRadius. " +
                    "For fills and strokes, the variable is bound to the color property of the first paint in the array.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the node to bind variable to (required)"
                    ),
                    ParamNames.FIELD to mapOf(
                        "type" to "string",
                        "description" to "Property to bind variable to (required, e.g., 'fills', 'strokes', 'opacity', 'width'). " +
                                "Note: For 'fills' and 'strokes', the node must have at least one fill/stroke, and the variable " +
                                "will be bound to the color property of the first paint."
                    ),
                    ParamNames.VARIABLE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the variable to bind (required). Format: \"VariableID:\" followed by mode and index (e.g., \"VariableID:1:65\"). Use figma_get_variables to get available variable IDs."
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.FIELD, ParamNames.VARIABLE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.BIND_VARIABLE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.FIELD, params.getRequiredString(ParamNames.FIELD))
            put(ParamNames.VARIABLE_ID, params.getRequiredString(ParamNames.VARIABLE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val field = params.getRequiredString(ParamNames.FIELD)
        return "Successfully bound variable to $field property"
    }
}
