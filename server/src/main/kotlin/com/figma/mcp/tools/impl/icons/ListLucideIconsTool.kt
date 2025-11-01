package com.figma.mcp.tools.impl.icons

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.config.FigmaConstants.ParamNames
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
 * List Lucide Icons Tool
 *
 * ## Purpose
 * Provides discovery and search functionality for Lucide icons.
 * Supports listing all icons, searching by query, filtering by category.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - This tool has ONE responsibility: provide icon discovery
 * - Icon searching is delegated to LucideIconService
 * - Response formatting is handled internally
 * - No Figma communication needed (read-only operation)
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: New search methods can be added
 * - Closed for modification: Core listing logic is stable
 * - Uses LucideIconService for all icon operations
 *
 * ### Dependency Inversion Principle (DIP)
 * - Depends on LucideIconService abstraction
 * - Doesn't know about file system implementation
 * - High-level discovery doesn't depend on low-level I/O
 *
 * ## Parameters
 * - query: string (optional) - Search query for icon names, categories, or tags
 * - category: string (optional) - Filter by specific category
 * - limit: number (optional) - Maximum number of results (default: 20)
 *
 * ## Usage Examples
 * ```kotlin
 * // List first 20 icons alphabetically
 * listLucideIcons()
 *
 * // Search for accessibility-related icons
 * listLucideIcons(query = "accessibility")
 *
 * // Get all icons in the "medical" category
 * listLucideIcons(category = "medical")
 *
 * // Search with custom limit
 * listLucideIcons(query = "arrow", limit = 50)
 * ```
 *
 * ## Response Format
 * Returns JSON array with icon objects containing:
 * - name: Icon name
 * - categories: Array of categories
 * - tags: Array of tags
 *
 * @param logger Logger for diagnostic output
 * @param connectionManager Figma connection manager (not used, but required by BaseFigmaTool)
 * @param iconService Lucide icon service for icon discovery
 */
class ListLucideIconsTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager,
    private val iconService: LucideIconService
) : BaseFigmaTool(logger, connectionManager, ToolNames.LIST_LUCIDE_ICONS) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "List and search available Lucide icons. " +
                    "Supports listing all icons, searching by query, and filtering by category. " +
                    "Use this tool to discover icons before creating them with create_lucide_icon. " +
                    "Returns icon names with categories and tags for easy browsing.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.QUERY to mapOf(
                        "type" to "string",
                        "description" to "Search query for icon names, categories, or tags (optional). " +
                                "Examples: 'accessibility', 'arrow', 'medical', 'user'. " +
                                "Leave empty to list all icons."
                    ),
                    ParamNames.CATEGORY to mapOf(
                        "type" to "string",
                        "description" to "Filter by specific category (optional). " +
                                "Examples: 'accessibility', 'medical', 'arrows', 'social'. " +
                                "Takes precedence over query if both provided."
                    ),
                    ParamNames.LIMIT to mapOf(
                        "type" to "number",
                        "description" to "Maximum number of results (optional, " +
                                "default: ${FigmaConstants.LucideIcons.SEARCH_DEFAULT_LIMIT}). " +
                                "Set higher for comprehensive results."
                    )
                ),
                required = emptyList()
            )
        )
    }

    /**
     * Override execute to handle this as a read-only operation
     *
     * This tool doesn't need to communicate with Figma plugin.
     * It's a pure information retrieval operation.
     *
     * @param arguments Tool arguments
     * @return CallToolResult with icon list
     */
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            logger.info("Listing Lucide icons", "arguments" to arguments.toString())

            val query = arguments.getStringOrNull(ParamNames.QUERY)
            val category = arguments.getStringOrNull(ParamNames.CATEGORY)
            val limit = arguments.getIntOrDefault(
                ParamNames.LIMIT,
                FigmaConstants.LucideIcons.SEARCH_DEFAULT_LIMIT
            )

            // Determine which operation to perform
            val iconNames = when {
                // Priority 1: Category filter
                category != null && category.isNotBlank() -> {
                    logger.info("Filtering by category", "category" to category, "limit" to limit)
                    iconService.getIconsByCategory(category, limit)
                }
                // Priority 2: Search query
                query != null && query.isNotBlank() -> {
                    logger.info("Searching icons", "query" to query, "limit" to limit)
                    iconService.searchIcons(query, limit)
                }
                // Priority 3: List all
                else -> {
                    logger.info("Listing all icons", "limit" to limit)
                    iconService.listAllIcons().take(limit)
                }
            }

            // Load detailed icon information
            val icons = iconNames.mapNotNull { iconName ->
                try {
                    val icon = iconService.getIcon(iconName)
                    buildJsonObject {
                        put("name", icon.name)
                        put("categories", buildJsonArray {
                            icon.categories.forEach { add(it) }
                        })
                        put("tags", buildJsonArray {
                            icon.tags.forEach { add(it) }
                        })
                    }
                } catch (e: Exception) {
                    logger.warn("Could not load icon details", "iconName" to iconName)
                    null
                }
            }

            // Build response
            val response = buildJsonObject {
                put("totalResults", icons.size)
                put("icons", buildJsonArray {
                    icons.forEach { add(it) }
                })

                // Include available categories for reference
                try {
                    val allCategories = iconService.getAllCategories()
                    put("availableCategories", buildJsonArray {
                        allCategories.forEach { add(it) }
                    })
                } catch (e: Exception) {
                    logger.warn("Could not load categories")
                }

                // Include search metadata
                put("searchParams", buildJsonObject {
                    query?.let { put("query", it) }
                    category?.let { put("category", it) }
                    put("limit", limit)
                })
            }

            logger.info("Successfully listed icons", "count" to icons.size)

            CallToolResult(
                content = listOf(
                    ToolContent.TextContent(text = response.toJsonString())
                ),
                isError = false
            )

        } catch (e: IconServiceException) {
            logger.error("Icon listing failed", e)
            errorResult(e.message ?: "Failed to list icons")

        } catch (e: Exception) {
            logger.error("Unexpected error listing icons", e)
            errorResult("Failed to list icons: ${e.message}")
        }
    }

    // These methods are not used since we override execute()
    // But must be implemented to satisfy abstract base class

    override fun buildCommandParams(params: JsonObject): JsonObject {
        // Not used - this tool doesn't send commands to Figma
        return buildJsonObject { }
    }
}
