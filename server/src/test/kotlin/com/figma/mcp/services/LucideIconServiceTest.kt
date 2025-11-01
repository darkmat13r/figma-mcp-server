package com.figma.mcp.services

import com.figma.mcp.core.ILogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit Tests for LucideIconService
 *
 * ## Test Coverage
 * - Icon loading and parsing
 * - Metadata extraction
 * - Search functionality
 * - Category filtering
 * - Error handling
 *
 * ## Test Philosophy (AAA Pattern)
 * All tests follow the Arrange-Act-Assert pattern:
 * 1. Arrange: Set up test data and mocks
 * 2. Act: Execute the method under test
 * 3. Assert: Verify expected outcomes
 *
 * ## Testing Strategy
 * - Test happy paths (successful operations)
 * - Test edge cases (empty results, invalid input)
 * - Test error conditions (missing files, invalid data)
 * - Test boundary conditions (limits, large datasets)
 */
class LucideIconServiceTest {

    private lateinit var logger: TestLogger
    private lateinit var iconService: LucideIconService

    // Test double for logger
    private class TestLogger : ILogger {
        override fun info(message: String) {}
        override fun error(message: String) {}
        override fun debug(message: String) {}
        override fun warn(message: String) {}
    }

    @BeforeEach
    fun setUp() {
        // Arrange: Create test logger
        logger = TestLogger()

        // Create service instance with actual icon path
        val iconsPath = "../figma-plugin/external/lucide/icons"
        iconService = LucideIconService(iconsPath)
    }

    // ========================================================================
    // HAPPY PATH TESTS
    // ========================================================================

    @Test
    fun `test_listAllIcons_returnsNonEmptyList`() {
        // Act: List all icons
        val icons = iconService.listAllIcons()

        // Assert: Should return icons
        assertTrue(icons.isNotEmpty(), "Should return at least one icon")
        assertTrue(icons.all { it.isNotBlank() }, "All icon names should be non-blank")
        assertEquals(icons.sorted(), icons, "Icons should be sorted alphabetically")

        // Verify logging
        verify { logger.info("Listed ${icons.size} Lucide icons") }
    }

    @Test
    fun `test_getIcon_withValidName_returnsIconWithMetadata`() {
        // Arrange: Use a known icon
        val iconName = "accessibility"

        // Act: Get icon
        val icon = iconService.getIcon(iconName)

        // Assert: Icon should have valid data
        assertEquals(iconName, icon.name)
        assertNotNull(icon.svgContent)
        assertTrue(icon.svgContent.contains("<svg"), "SVG content should contain svg tag")
        assertTrue(icon.categories.isNotEmpty(), "Should have at least one category")
        assertTrue(icon.tags.isNotEmpty(), "Should have at least one tag")
    }

    @Test
    fun `test_searchIcons_withValidQuery_returnsMatchingIcons`() {
        // Arrange: Search for arrow icons
        val query = "arrow"

        // Act: Search icons
        val results = iconService.searchIcons(query)

        // Assert: Should return matching icons
        assertTrue(results.isNotEmpty(), "Should find icons matching '$query'")
        assertTrue(
            results.any { it.contains(query, ignoreCase = true) },
            "At least one result should contain the query"
        )
    }

    @Test
    fun `test_getIconsByCategory_withValidCategory_returnsIcons`() {
        // Arrange: Use a known category
        val category = "accessibility"

        // Act: Get icons by category
        val icons = iconService.getIconsByCategory(category)

        // Assert: Should return icons in that category
        assertTrue(icons.isNotEmpty(), "Should find icons in category '$category'")

        // Verify at least one icon actually has that category
        val firstIcon = iconService.getIcon(icons.first())
        assertTrue(
            firstIcon.categories.any { it.equals(category, ignoreCase = true) },
            "Icon should belong to category '$category'"
        )
    }

    @Test
    fun `test_getAllCategories_returnsUniqueCategories`() {
        // Act: Get all categories
        val categories = iconService.getAllCategories()

        // Assert: Should return categories
        assertTrue(categories.isNotEmpty(), "Should return at least one category")
        assertEquals(categories.toSet().size, categories.size, "Categories should be unique")
        assertEquals(categories.sorted(), categories, "Categories should be sorted")
    }

    @Test
    fun `test_extractSvgPathData_removesXmlDeclaration`() {
        // Arrange: SVG with XML declaration
        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg">
                <path d="M10 10 L 20 20"/>
            </svg>
        """.trimIndent()

        // Act: Extract path data
        val result = iconService.extractSvgPathData(svgContent)

        // Assert: XML declaration should be removed
        assertFalse(result.contains("<?xml"), "Should remove XML declaration")
        assertTrue(result.contains("<svg"), "Should contain SVG tag")
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    fun `test_searchIcons_withEmptyQuery_returnsAllIcons`() {
        // Arrange: Empty query
        val query = ""

        // Act: Search with empty query
        val results = iconService.searchIcons(query)

        // Assert: Should return icons (up to default limit)
        assertTrue(results.isNotEmpty(), "Should return icons for empty query")
        assertTrue(results.size <= 20, "Should respect default limit")
    }

    @Test
    fun `test_searchIcons_withLimit_respectsLimit`() {
        // Arrange: Search with custom limit
        val query = "a" // Very common letter
        val limit = 5

        // Act: Search with limit
        val results = iconService.searchIcons(query, limit)

        // Assert: Should not exceed limit
        assertTrue(results.size <= limit, "Should respect limit parameter")
    }

    @Test
    fun `test_getIconsByCategory_withLimit_respectsLimit`() {
        // Arrange: Get category with limit
        val category = "accessibility"
        val limit = 3

        // Act: Get icons with limit
        val results = iconService.getIconsByCategory(category, limit)

        // Assert: Should not exceed limit
        assertTrue(results.size <= limit, "Should respect limit parameter")
    }

    @Test
    fun `test_searchIcons_withNoMatches_returnsEmptyList`() {
        // Arrange: Search for non-existent icon
        val query = "xyzabc123nonexistent"

        // Act: Search for impossible match
        val results = iconService.searchIcons(query)

        // Assert: Should return empty list
        assertTrue(results.isEmpty(), "Should return empty list for no matches")
    }

    // ========================================================================
    // ERROR CONDITION TESTS
    // ========================================================================

    @Test
    fun `test_getIcon_withInvalidName_throwsException`() {
        // Arrange: Invalid icon name
        val invalidName = "nonexistent-icon-xyz"

        // Act & Assert: Should throw IconServiceException
        val exception = assertThrows<IconServiceException> {
            iconService.getIcon(invalidName)
        }

        assertTrue(
            exception.message?.contains("Icon not found") ?: false,
            "Error message should mention icon not found"
        )
    }

    @Test
    fun `test_getIconsByCategory_withInvalidCategory_returnsEmptyList`() {
        // Arrange: Non-existent category
        val invalidCategory = "nonexistent-category-xyz"

        // Act: Get icons by invalid category
        val results = iconService.getIconsByCategory(invalidCategory)

        // Assert: Should return empty list (not throw exception)
        assertTrue(results.isEmpty(), "Should return empty list for invalid category")
    }

    // ========================================================================
    // BOUNDARY CONDITION TESTS
    // ========================================================================

    @Test
    fun `test_searchIcons_withZeroLimit_returnsEmpty`() {
        // Arrange: Search with zero limit
        val query = "arrow"
        val limit = 0

        // Act: Search with zero limit
        val results = iconService.searchIcons(query, limit)

        // Assert: Should return empty list
        assertTrue(results.isEmpty(), "Should return empty list with zero limit")
    }

    @Test
    fun `test_searchIcons_withLargeLimit_worksCorrectly`() {
        // Arrange: Search with very large limit
        val query = "" // All icons
        val limit = 10000

        // Act: Search with large limit
        val results = iconService.searchIcons(query, limit)

        // Assert: Should work without errors
        assertTrue(results.isNotEmpty(), "Should return icons")
        // Should return all icons or limit, whichever is smaller
        val allIcons = iconService.listAllIcons()
        assertEquals(
            minOf(allIcons.size, limit),
            results.size,
            "Should return all available icons"
        )
    }

    // ========================================================================
    // SEARCH SCORING TESTS
    // ========================================================================

    @Test
    fun `test_searchIcons_exactMatch_ranksHigher`() {
        // Arrange: Search for exact icon name
        val exactName = "accessibility"

        // Act: Search for exact name
        val results = iconService.searchIcons(exactName, limit = 10)

        // Assert: Exact match should be first result
        assertTrue(results.isNotEmpty(), "Should find the icon")
        assertEquals(
            exactName,
            results.first(),
            "Exact match should rank highest"
        )
    }

    @Test
    fun `test_searchIcons_partialMatch_findsResults`() {
        // Arrange: Search for partial match
        val partial = "acc" // Should match "accessibility"

        // Act: Search for partial
        val results = iconService.searchIcons(partial)

        // Assert: Should find icons containing the partial string
        assertTrue(results.isNotEmpty(), "Should find partial matches")
        assertTrue(
            results.any { it.contains(partial, ignoreCase = true) },
            "Should include icons containing '$partial'"
        )
    }

    // ========================================================================
    // INTEGRATION TESTS
    // ========================================================================

    @Test
    fun `test_fullWorkflow_searchAndLoad`() {
        // Arrange: Full workflow test
        val query = "alert"

        // Act: Search for icons
        val searchResults = iconService.searchIcons(query, limit = 5)

        // Assert: Search should succeed
        assertTrue(searchResults.isNotEmpty(), "Search should return results")

        // Act: Load first icon from results
        val icon = iconService.getIcon(searchResults.first())

        // Assert: Icon should load successfully
        assertNotNull(icon)
        assertTrue(icon.svgContent.isNotBlank())
        assertTrue(icon.categories.isNotEmpty() || icon.tags.isNotEmpty())
    }

    @Test
    fun `test_fullWorkflow_categoryBrowse`() {
        // Arrange: Browse by category workflow

        // Act: Get all categories
        val categories = iconService.getAllCategories()
        assertTrue(categories.isNotEmpty(), "Should have categories")

        // Act: Get icons in first category
        val firstCategory = categories.first()
        val icons = iconService.getIconsByCategory(firstCategory, limit = 3)

        // Assert: Should find icons
        assertTrue(icons.isNotEmpty(), "Should find icons in category")

        // Act: Load first icon
        val icon = iconService.getIcon(icons.first())

        // Assert: Icon should belong to category
        assertTrue(
            icon.categories.any { it.equals(firstCategory, ignoreCase = true) },
            "Icon should belong to selected category"
        )
    }
}
