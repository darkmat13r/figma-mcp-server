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
 * Create Section Tool
 *
 * ## Purpose
 * Creates organizational sections on the canvas.
 * Sections help organize large Figma files by grouping related frames.
 *
 * ## Parameters
 * - name: string (required) - Section name
 * - width: number (optional) - Section width
 * - height: number (optional) - Section height
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
 */
class CreateSectionTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_SECTION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates an organizational section on the Figma canvas. " +
                    "Sections are special containers that help organize large files by grouping " +
                    "related frames, components, and other content. Unlike frames, sections don't " +
                    "affect layout or rendering - they're purely organizational.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the section (required). Examples: 'Mobile Screens', 'Components', 'Design System'"
                    ),
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Width in pixels (optional, default: ${Defaults.DEFAULT_WIDTH})"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Height in pixels (optional, default: ${Defaults.DEFAULT_HEIGHT})"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this section (optional)"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this section (optional)"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this section (optional)"
                    )
                ),
                required = listOf(ParamNames.NAME)
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.SECTION)
            put(ParamNames.NAME, params.getRequiredString(ParamNames.NAME))
            put(ParamNames.WIDTH, params.getDoubleOrDefault(ParamNames.WIDTH, Defaults.DEFAULT_WIDTH))
            put(ParamNames.HEIGHT, params.getDoubleOrDefault(ParamNames.HEIGHT, Defaults.DEFAULT_HEIGHT))
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }
}
