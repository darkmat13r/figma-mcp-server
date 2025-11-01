package com.figma.mcp.tools.impl.icons

import com.figma.mcp.config.FigmaConstants.Defaults
import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.SuccessMessages
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.services.IconServiceException
import com.figma.mcp.services.LucideIconService
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Lucide Icon Tool
 *
 * ## Purpose
 * Creates Lucide icons in Figma from the local icon library.
 * Lucide is a beautiful, consistent icon library with 1000+ open-source icons.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - This tool has ONE responsibility: create Lucide icons in Figma
 * - Icon loading is delegated to LucideIconService
 * - SVG parsing is delegated to the service
 * - Figma communication is handled by BaseFigmaTool
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: New icon sources can be added without modifying this tool
 * - Closed for modification: Core icon creation logic is stable
 * - Uses LucideIconService abstraction for icon access
 *
 * ### Dependency Inversion Principle (DIP)
 * - Depends on LucideIconService abstraction, not file system
 * - Depends on BaseFigmaTool abstraction, not WebSocket details
 * - High-level icon creation doesn't depend on low-level I/O
 *
 * ## Parameters
 * - iconName: string (required) - Name of Lucide icon (e.g., "accessibility", "activity")
 * - size: number (optional) - Icon size in pixels (default: 24)
 * - color: string (optional) - Icon color as hex (default: "#000000")
 * - x: number (optional) - X position (default: 0)
 * - y: number (optional) - Y position (default: 0)
 *
 * ## Usage Example
 * ```kotlin
 * // Create an accessibility icon at default size
 * createLucideIcon(iconName = "accessibility")
 *
 * // Create a large red heart icon
 * createLucideIcon(
 *   iconName = "heart",
 *   size = 48,
 *   color = "#FF0000",
 *   x = 100,
 *   y = 100
 * )
 * ```
 *
 * @param logger Logger for diagnostic output
 * @param connectionManager Figma connection manager for WebSocket communication
 * @param iconService Lucide icon service for icon access
 */
class CreateLucideIconTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager,
    private val iconService: LucideIconService
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_LUCIDE_ICON) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a Lucide icon in Figma from the local icon library. " +
                    "Lucide is a beautiful, consistent icon library with 1000+ open-source icons. " +
                    "Icons are created as SVG vectors with customizable size and color. " +
                    "Use list_lucide_icons to discover available icons.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.ICON_NAME to mapOf(
                        "type" to "string",
                        "description" to "Name of Lucide icon (required). Examples: 'accessibility', 'activity', " +
                                "'airplay', 'alert-circle'. Use list_lucide_icons to find icons."
                    ),
                    ParamNames.SIZE to mapOf(
                        "type" to "number",
                        "description" to "Icon size in pixels (optional, default: ${Defaults.DEFAULT_ICON_SIZE}). " +
                                "Common sizes: 16, 20, 24, 32, 48, 64."
                    ),
                    ParamNames.COLOR to mapOf(
                        "type" to "string",
                        "description" to "Icon color as hex code (optional, default: ${Defaults.DEFAULT_ICON_COLOR}). " +
                                "Examples: '#FF0000', '#3B82F6', '#10B981'."
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
                required = listOf(ParamNames.ICON_NAME)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_LUCIDE_ICON
    }

    /**
     * Extract and validate parameters
     *
     * Validates icon name and loads icon from service.
     * Throws ParameterValidationException if icon doesn't exist.
     *
     * @param arguments Raw MCP arguments
     * @return Validated parameters with icon data
     * @throws ParameterValidationException if icon doesn't exist
     */
    override fun extractParameters(arguments: JsonObject): JsonObject {
        val iconName = arguments.getRequiredString(ParamNames.ICON_NAME)

        // Validate icon exists by loading it
        try {
            val icon = iconService.getIcon(iconName)

            logger.info(
                "Loaded Lucide icon",
                "iconName" to iconName,
                "categories" to icon.categories.joinToString(", "),
                "tags" to icon.tags.joinToString(", ")
            )

            // Return original arguments (icon data will be loaded in buildCommandParams)
            return arguments

        } catch (e: IconServiceException) {
            logger.error("Icon not found", e, "iconName" to iconName)
            throw com.figma.mcp.tools.ParameterValidationException(e.message ?: "Icon not found: $iconName")
        }
    }

    /**
     * Build command parameters for Figma plugin
     *
     * Loads icon SVG data and builds command payload.
     * Includes icon name, SVG content, size, color, and position.
     *
     * @param params Validated parameters
     * @return JsonObject with command parameters for Figma plugin
     */
    override fun buildCommandParams(params: JsonObject): JsonObject {
        val iconName = params.getRequiredString(ParamNames.ICON_NAME)

        // Load icon (already validated in extractParameters)
        val icon = iconService.getIcon(iconName)
        val svgData = iconService.extractSvgPathData(icon.svgContent)

        return buildJsonObject {
            put(ParamNames.ICON_NAME, iconName)
            put("svgData", svgData)  // Raw SVG content for plugin to parse
            put(ParamNames.SIZE, params.getDoubleOrDefault(ParamNames.SIZE, Defaults.DEFAULT_ICON_SIZE))
            put(ParamNames.COLOR, params.getStringOrDefault(ParamNames.COLOR, Defaults.DEFAULT_ICON_COLOR))
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))

            // Include metadata for debugging/logging
            put("categories", buildJsonArray {
                icon.categories.forEach { add(it) }
            })
            put("tags", buildJsonArray {
                icon.tags.forEach { add(it) }
            })
        }
    }

    /**
     * Format success response
     *
     * Returns structured response with icon details and node ID.
     *
     * @param pluginResponse Response from Figma plugin
     * @param params Original parameters
     * @return CallToolResult with success content
     */
    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val nodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull
        val iconName = params.getStringOrNull(ParamNames.ICON_NAME) ?: "unknown"
        val size = params.getDoubleOrNull(ParamNames.SIZE) ?: Defaults.DEFAULT_ICON_SIZE
        val color = params.getStringOrNull(ParamNames.COLOR) ?: Defaults.DEFAULT_ICON_COLOR

        val responseData = buildJsonObject {
            put("iconName", iconName)
            put("size", size)
            put("color", color)
            nodeId?.let { put("nodeId", it) }
            put("message", SuccessMessages.lucideIconCreated(iconName, nodeId))
        }

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = responseData.toJsonString())
            ),
            isError = false
        )
    }

    /**
     * Build success message
     *
     * Creates human-readable success message with icon details.
     *
     * @param pluginResponse Response from Figma plugin
     * @param params Original parameters
     * @return Success message string
     */
    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val nodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull
        val iconName = params.getStringOrNull(ParamNames.ICON_NAME) ?: "unknown"

        return SuccessMessages.lucideIconCreated(iconName, nodeId)
    }
}
