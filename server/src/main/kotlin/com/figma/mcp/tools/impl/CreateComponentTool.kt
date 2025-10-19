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
 * Create Component Tool
 *
 * ## Purpose
 * Creates reusable component masters that can be instantiated multiple times.
 * Components are the foundation of design systems in Figma.
 *
 * ## Parameters
 * - name: string (required) - Component name
 * - width: number (optional) - Component width
 * - height: number (optional) - Component height
 * - description: string (optional) - Component description
 */
class CreateComponentTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_COMPONENT) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a reusable component master in Figma. " +
                    "Components can be instantiated multiple times and changes to the master " +
                    "propagate to all instances. Essential for building design systems.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the component (required)"
                    ),
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Width in pixels (optional, default: ${Defaults.DEFAULT_WIDTH})"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Height in pixels (optional, default: ${Defaults.DEFAULT_HEIGHT})"
                    ),
                    ParamNames.DESCRIPTION to mapOf(
                        "type" to "string",
                        "description" to "Description of the component's purpose (optional)"
                    )
                ),
                required = listOf(ParamNames.NAME)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.COMPONENT)
            put(ParamNames.NAME, params.getRequiredString(ParamNames.NAME))
            put(ParamNames.WIDTH, params.getDoubleOrDefault(ParamNames.WIDTH, Defaults.DEFAULT_WIDTH))
            put(ParamNames.HEIGHT, params.getDoubleOrDefault(ParamNames.HEIGHT, Defaults.DEFAULT_HEIGHT))
            params.getStringOrNull(ParamNames.DESCRIPTION)?.let { put(ParamNames.DESCRIPTION, it) }
        }
    }
}
