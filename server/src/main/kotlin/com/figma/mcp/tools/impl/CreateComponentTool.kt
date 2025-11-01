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
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
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
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
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
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }
}
