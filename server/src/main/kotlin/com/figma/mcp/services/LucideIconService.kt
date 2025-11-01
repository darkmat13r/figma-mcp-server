package com.figma.mcp.services

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.core.ILogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Lucide Icon Service
 *
 * ## Purpose
 * Manages Lucide icon library access, providing SVG data and metadata for icon creation.
 * Supports icon discovery, search, and filtering by categories and tags.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - This service has ONE responsibility: manage Lucide icon access
 * - Icon parsing, file I/O, and metadata extraction are encapsulated here
 * - Tools depend on this service, not on file system directly
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: Can add new icon sources or formats
 * - Closed for modification: Core parsing logic is stable
 * - New search methods can be added without changing existing ones
 *
 * ### Dependency Inversion Principle (DIP)
 * - Tools depend on this service abstraction
 * - File system implementation details are hidden
 * - Could be extended to interface if multiple icon providers needed
 *
 * ## Icon Data Structure
 * Each icon has:
 * - name.svg: SVG file with icon graphics
 * - name.json: Metadata with categories, tags, contributors
 *
 * ## Usage Example
 * ```kotlin
 * val service = LucideIconService(logger)
 * val icon = service.getIcon("accessibility")
 * val svgData = icon.svgContent
 * val categories = icon.categories
 * ```
 *
 * @param logger Logger for diagnostic output
 */
class LucideIconService(
    private val logger: ILogger
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Icon Data Class
     *
     * Encapsulates all information about a Lucide icon.
     *
     * @property name Icon name (e.g., "accessibility")
     * @property svgContent Raw SVG content
     * @property categories List of categories (e.g., ["accessibility", "medical"])
     * @property tags List of tags (e.g., ["disability", "wheelchair"])
     * @property contributors List of contributor usernames
     */
    data class LucideIcon(
        val name: String,
        val svgContent: String,
        val categories: List<String>,
        val tags: List<String>,
        val contributors: List<String>
    )

    /**
     * Icon Search Result
     *
     * Contains icon name and relevance score for search results.
     *
     * @property name Icon name
     * @property score Relevance score (higher = more relevant)
     */
    data class IconSearchResult(
        val name: String,
        val score: Int
    )

    /**
     * Get the icons directory path
     *
     * Uses absolute path from FigmaConstants.
     * Validates that directory exists and is readable.
     *
     * @return Path to icons directory
     * @throws IconServiceException if directory doesn't exist or isn't readable
     */
    private fun getIconsDirectory(): Path {
        val iconsPath = Paths.get(FigmaConstants.LucideIcons.ICONS_DIRECTORY_PATH)

        if (!Files.exists(iconsPath)) {
            throw IconServiceException(
                "${FigmaConstants.LucideIcons.ERROR_DIRECTORY_NOT_FOUND}: ${iconsPath.toAbsolutePath()}"
            )
        }

        if (!Files.isReadable(iconsPath)) {
            throw IconServiceException(
                "${FigmaConstants.LucideIcons.ERROR_DIRECTORY_NOT_READABLE}: ${iconsPath.toAbsolutePath()}"
            )
        }

        return iconsPath
    }

    /**
     * Get all available icon names
     *
     * Scans the icons directory for .svg files and returns icon names.
     * Names are extracted from filenames (e.g., "accessibility.svg" -> "accessibility").
     *
     * @return List of icon names, sorted alphabetically
     * @throws IconServiceException if directory cannot be read
     */
    fun listAllIcons(): List<String> {
        return try {
            val iconsDir = getIconsDirectory()

            Files.list(iconsDir)
                .filter { it.toString().endsWith(FigmaConstants.LucideIcons.SVG_EXTENSION) }
                .map { it.fileName.toString().removeSuffix(FigmaConstants.LucideIcons.SVG_EXTENSION) }
                .sorted()
                .toList()
                .also {
                    logger.info("Listed ${it.size} Lucide icons")
                }
        } catch (e: IconServiceException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to list icons", e)
            throw IconServiceException("${FigmaConstants.LucideIcons.ERROR_FAILED_TO_LIST}: ${e.message}")
        }
    }

    /**
     * Get icon by name
     *
     * Retrieves icon SVG content and metadata.
     * Parses both .svg and .json files for complete icon data.
     *
     * @param iconName Icon name without extension (e.g., "accessibility")
     * @return LucideIcon with SVG content and metadata
     * @throws IconServiceException if icon doesn't exist or cannot be read
     */
    fun getIcon(iconName: String): LucideIcon {
        return try {
            val iconsDir = getIconsDirectory()
            val svgFile = iconsDir.resolve("$iconName${FigmaConstants.LucideIcons.SVG_EXTENSION}")
            val jsonFile = iconsDir.resolve("$iconName${FigmaConstants.LucideIcons.JSON_EXTENSION}")

            // Validate SVG file exists
            if (!Files.exists(svgFile)) {
                throw IconServiceException(
                    "${FigmaConstants.LucideIcons.ERROR_ICON_NOT_FOUND}: $iconName"
                )
            }

            // Read SVG content
            val svgContent = Files.readString(svgFile)

            // Read metadata if available
            val (categories, tags, contributors) = if (Files.exists(jsonFile)) {
                parseMetadata(jsonFile)
            } else {
                logger.warn("Metadata file not found for icon: $iconName")
                Triple(emptyList(), emptyList(), emptyList())
            }

            LucideIcon(
                name = iconName,
                svgContent = svgContent,
                categories = categories,
                tags = tags,
                contributors = contributors
            ).also {
                logger.debug("Loaded icon: $iconName (${categories.size} categories, ${tags.size} tags)")
            }

        } catch (e: IconServiceException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get icon: $iconName", e)
            throw IconServiceException(
                "${FigmaConstants.LucideIcons.ERROR_FAILED_TO_READ}: $iconName - ${e.message}"
            )
        }
    }

    /**
     * Parse metadata JSON file
     *
     * Extracts categories, tags, and contributors from icon metadata.
     *
     * @param jsonFile Path to .json metadata file
     * @return Triple of (categories, tags, contributors)
     */
    private fun parseMetadata(jsonFile: Path): Triple<List<String>, List<String>, List<String>> {
        return try {
            val jsonContent = Files.readString(jsonFile)
            val jsonObject = json.parseToJsonElement(jsonContent).jsonObject

            val categories = jsonObject[FigmaConstants.LucideIcons.METADATA_FIELD_CATEGORIES]
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content }
                ?: emptyList()

            val tags = jsonObject[FigmaConstants.LucideIcons.METADATA_FIELD_TAGS]
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content }
                ?: emptyList()

            val contributors = jsonObject[FigmaConstants.LucideIcons.METADATA_FIELD_CONTRIBUTORS]
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content }
                ?: emptyList()

            Triple(categories, tags, contributors)
        } catch (e: Exception) {
            logger.warn("Failed to parse metadata: ${jsonFile.fileName}", "error" to (e.message ?: "unknown"))
            Triple(emptyList(), emptyList(), emptyList())
        }
    }

    /**
     * Search icons by query
     *
     * Searches icon names, categories, and tags for matches.
     * Returns results sorted by relevance score.
     *
     * Search scoring:
     * - Exact name match: 100 points
     * - Name contains query: 50 points
     * - Category match: 30 points
     * - Tag match: 20 points
     *
     * @param query Search query string
     * @param limit Maximum number of results (default: 20)
     * @return List of matching icon names, sorted by relevance
     */
    fun searchIcons(query: String, limit: Int = FigmaConstants.LucideIcons.SEARCH_DEFAULT_LIMIT): List<String> {
        if (query.isBlank()) {
            return listAllIcons().take(limit)
        }

        val normalizedQuery = query.lowercase().trim()
        val results = mutableListOf<IconSearchResult>()

        return try {
            val allIcons = listAllIcons()

            for (iconName in allIcons) {
                var score = 0

                // Score by name match
                if (iconName.equals(normalizedQuery, ignoreCase = true)) {
                    score += FigmaConstants.LucideIcons.SEARCH_SCORE_NAME_EXACT
                } else if (iconName.contains(normalizedQuery, ignoreCase = true)) {
                    score += FigmaConstants.LucideIcons.SEARCH_SCORE_NAME_CONTAINS
                }

                // Score by metadata (if available)
                try {
                    val icon = getIcon(iconName)

                    // Score by categories
                    if (icon.categories.any { it.contains(normalizedQuery, ignoreCase = true) }) {
                        score += FigmaConstants.LucideIcons.SEARCH_SCORE_CATEGORY_MATCH
                    }

                    // Score by tags
                    if (icon.tags.any { it.contains(normalizedQuery, ignoreCase = true) }) {
                        score += FigmaConstants.LucideIcons.SEARCH_SCORE_TAG_MATCH
                    }
                } catch (e: Exception) {
                    // Continue without metadata if it fails
                    logger.debug("Could not load metadata for scoring: $iconName")
                }

                if (score > 0) {
                    results.add(IconSearchResult(iconName, score))
                }
            }

            results.sortByDescending { it.score }
            results.take(limit).map { it.name }
                .also {
                    logger.info("Found ${it.size} icons matching query: $query")
                }

        } catch (e: Exception) {
            logger.error("Search failed for query: $query", e)
            throw IconServiceException("${FigmaConstants.LucideIcons.ERROR_SEARCH_FAILED}: ${e.message}")
        }
    }

    /**
     * Get icons by category
     *
     * Retrieves all icons belonging to a specific category.
     *
     * @param category Category name (e.g., "accessibility", "medical")
     * @param limit Maximum number of results (optional)
     * @return List of icon names in the category
     */
    fun getIconsByCategory(category: String, limit: Int? = null): List<String> {
        val normalizedCategory = category.lowercase().trim()
        val results = mutableListOf<String>()

        return try {
            val allIcons = listAllIcons()

            for (iconName in allIcons) {
                try {
                    val icon = getIcon(iconName)
                    if (icon.categories.any { it.equals(normalizedCategory, ignoreCase = true) }) {
                        results.add(iconName)

                        if (limit != null && results.size >= limit) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Continue with next icon
                    logger.debug("Could not check category for: $iconName")
                }
            }

            results.also {
                logger.info("Found ${it.size} icons in category: $category")
            }

        } catch (e: Exception) {
            logger.error("Failed to get icons by category: $category", e)
            throw IconServiceException(
                "${FigmaConstants.LucideIcons.ERROR_CATEGORY_SEARCH_FAILED}: $category - ${e.message}"
            )
        }
    }

    /**
     * Get all available categories
     *
     * Scans all icon metadata to build a list of unique categories.
     *
     * @return List of unique category names, sorted alphabetically
     */
    fun getAllCategories(): List<String> {
        val categories = mutableSetOf<String>()

        return try {
            val allIcons = listAllIcons()

            for (iconName in allIcons) {
                try {
                    val icon = getIcon(iconName)
                    categories.addAll(icon.categories)
                } catch (e: Exception) {
                    // Continue with next icon
                    logger.debug("Could not load categories for: $iconName")
                }
            }

            categories.sorted().also {
                logger.info("Found ${it.size} unique categories")
            }

        } catch (e: Exception) {
            logger.error("Failed to get all categories", e)
            throw IconServiceException(
                "${FigmaConstants.LucideIcons.ERROR_FAILED_TO_LIST_CATEGORIES}: ${e.message}"
            )
        }
    }

    /**
     * Extract SVG path data from SVG content
     *
     * Parses SVG content to extract path data for Figma rendering.
     * This is a simple extraction that gets all path, circle, line elements.
     *
     * @param svgContent Raw SVG content
     * @return Cleaned SVG content suitable for Figma
     */
    fun extractSvgPathData(svgContent: String): String {
        // Remove XML declaration and comments
        return svgContent
            .replace("""<\?xml[^>]*\?>""".toRegex(), "")
            .replace("""<!--[\s\S]*?-->""".toRegex(), "")
            .trim()
    }
}

/**
 * Custom exception for icon service errors
 *
 * Thrown when icon operations fail (not found, cannot read, etc.)
 */
class IconServiceException(message: String) : Exception(message)
