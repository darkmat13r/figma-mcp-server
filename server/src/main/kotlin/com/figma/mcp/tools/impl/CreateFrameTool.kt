package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants
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
 * Create Frame Tool
 *
 * ## Purpose
 * Creates container frames - the primary layout building block in Figma.
 * Frames support auto-layout and are used to organize other nodes.
 *
 * ## Parameters
 * - name: string (optional) - Frame name
 * - width: number (optional, default: 100) - Frame width
 * - height: number (optional, default: 100) - Frame height
 * - x: number (optional, default: 0) - X position
 * - y: number (optional, default: 0) - Y position
 * - layoutMode: "NONE" | "HORIZONTAL" | "VERTICAL" (optional) - Auto-layout mode
 * - fills: Paint[] (optional) - Fill colors/gradients
 *
 * ## Example Usage
 * ```json
 * {
 *   "name": "Header Container",
 *   "width": 1440,
 *   "height": 80,
 *   "layoutMode": "HORIZONTAL"
 * }
 * ```
 */
class CreateFrameTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_FRAME) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a container frame in Figma. " +
                    "Frames are the primary layout building block and support auto-layout. " +
                    "Use frames to organize content, create responsive designs, and build component hierarchies.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the frame (optional)"
                    ),
                    ParamNames.WIDTH to mapOf(
                        "type" to "number",
                        "description" to "Width in pixels (optional, default: ${Defaults.DEFAULT_WIDTH})"
                    ),
                    ParamNames.HEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Height in pixels (optional, default: ${Defaults.DEFAULT_HEIGHT})"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    ),
                    ParamNames.LAYOUT_MODE to mapOf(
                        "type" to "string",
                        "enum" to listOf(
                            FigmaConstants.LayoutModes.NONE,
                            FigmaConstants.LayoutModes.HORIZONTAL,
                            FigmaConstants.LayoutModes.VERTICAL
                        ),
                        "description" to "Auto-layout mode: NONE (default), HORIZONTAL, or VERTICAL"
                    ),
                    ParamNames.FILLS to mapOf(
                        "type" to "array",
                        "description" to "Array of Paint objects for fill colors (optional)"
                    ),
                    "paddingLeft" to mapOf(
                        "type" to "number",
                        "description" to "Left padding in pixels (optional, requires layoutMode HORIZONTAL or VERTICAL)"
                    ),
                    "paddingRight" to mapOf(
                        "type" to "number",
                        "description" to "Right padding in pixels (optional, requires layoutMode HORIZONTAL or VERTICAL)"
                    ),
                    "paddingTop" to mapOf(
                        "type" to "number",
                        "description" to "Top padding in pixels (optional, requires layoutMode HORIZONTAL or VERTICAL)"
                    ),
                    "paddingBottom" to mapOf(
                        "type" to "number",
                        "description" to "Bottom padding in pixels (optional, requires layoutMode HORIZONTAL or VERTICAL)"
                    ),
                    "itemSpacing" to mapOf(
                        "type" to "number",
                        "description" to "Spacing between items in pixels (optional, requires layoutMode HORIZONTAL or VERTICAL)"
                    ),
                    "primaryAxisAlignItems" to mapOf(
                        "type" to "string",
                        "enum" to listOf("MIN", "CENTER", "MAX", "SPACE_BETWEEN"),
                        "description" to "Alignment along main axis (optional). For HORIZONTAL: MIN=left, CENTER=center, MAX=right. For VERTICAL: MIN=top, CENTER=center, MAX=bottom"
                    ),
                    "counterAxisAlignItems" to mapOf(
                        "type" to "string",
                        "enum" to listOf("MIN", "CENTER", "MAX"),
                        "description" to "Alignment along cross axis (optional). Perpendicular to main axis alignment"
                    ),
                    "primaryAxisSizingMode" to mapOf(
                        "type" to "string",
                        "enum" to listOf("FIXED", "AUTO"),
                        "description" to "Sizing mode along main axis (optional). FIXED=fixed size, AUTO=hug contents"
                    ),
                    "counterAxisSizingMode" to mapOf(
                        "type" to "string",
                        "enum" to listOf("FIXED", "AUTO"),
                        "description" to "Sizing mode along cross axis (optional). FIXED=fixed size, AUTO=hug contents"
                    )
                ),
                required = emptyList() // All parameters are optional
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.FRAME)

            // Optional parameters with defaults
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }
            put(ParamNames.WIDTH, params.getDoubleOrDefault(ParamNames.WIDTH, Defaults.DEFAULT_WIDTH))
            put(ParamNames.HEIGHT, params.getDoubleOrDefault(ParamNames.HEIGHT, Defaults.DEFAULT_HEIGHT))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))

            // Layout mode (optional)
            params.getStringOrNull(ParamNames.LAYOUT_MODE)?.let {
                put(ParamNames.LAYOUT_MODE, it)
            }

            // Fills (optional)
            params.getArrayOrNull(ParamNames.FILLS)?.let {
                put(ParamNames.FILLS, it)
            }

            // Auto layout padding properties
            params.getDoubleOrNull("paddingLeft")?.let { put("paddingLeft", it) }
            params.getDoubleOrNull("paddingRight")?.let { put("paddingRight", it) }
            params.getDoubleOrNull("paddingTop")?.let { put("paddingTop", it) }
            params.getDoubleOrNull("paddingBottom")?.let { put("paddingBottom", it) }

            // Item spacing
            params.getDoubleOrNull("itemSpacing")?.let { put("itemSpacing", it) }

            // Alignment properties
            params.getStringOrNull("primaryAxisAlignItems")?.let { put("primaryAxisAlignItems", it) }
            params.getStringOrNull("counterAxisAlignItems")?.let { put("counterAxisAlignItems", it) }

            // Sizing mode properties
            params.getStringOrNull("primaryAxisSizingMode")?.let { put("primaryAxisSizingMode", it) }
            params.getStringOrNull("counterAxisSizingMode")?.let { put("counterAxisSizingMode", it) }
        }
    }
}
