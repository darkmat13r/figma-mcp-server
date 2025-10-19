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
        }
    }
}
