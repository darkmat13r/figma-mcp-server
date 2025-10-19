package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.Defaults
import com.figma.mcp.config.FigmaConstants.NodeTypes
import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Create Instance Tool
 *
 * ## Purpose
 * Creates an instance of an existing component.
 * Instances inherit properties from the master component.
 *
 * ## Parameters
 * - componentId: string (required) - ID of the master component to instantiate
 * - x: number (optional) - X position
 * - y: number (optional) - Y position
 */
class CreateInstanceTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_INSTANCE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates an instance of an existing component in Figma. " +
                    "Instances inherit all properties from the master component and can be customized " +
                    "using component properties. Changes to the master propagate to all instances.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.COMPONENT_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the master component to instantiate (required)"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    )
                ),
                required = listOf(ParamNames.COMPONENT_ID)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.INSTANCE)
            put(ParamNames.COMPONENT_ID, params.getRequiredString(ParamNames.COMPONENT_ID))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))
        }
    }
}
