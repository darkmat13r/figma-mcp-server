package com.figma.mcp.tools.impl.icons

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.core.ILogger
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.services.IconServiceException
import com.figma.mcp.services.LucideIconData
import com.figma.mcp.services.LucideIconService
import com.figma.mcp.tools.ParameterValidationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

/**
 * Unit Tests for CreateLucideIconTool
 *
 * ## Test Strategy
 * - Use test doubles for dependencies
 * - Test parameter validation
 * - Test command building
 * - Test error handling
 *
 * ## Test Coverage
 * - Valid icon creation
 * - Icon not found handling
 * - Default parameter values
 * - Custom parameter values
 * - Success response formatting
 */
class CreateLucideIconToolTest {

    private lateinit var logger: TestLogger
    private lateinit var connectionManager: TestConnectionManager
    private lateinit var iconService: TestIconService
    private lateinit var tool: CreateLucideIconTool

    // Test doubles
    private class TestLogger : ILogger {
        override fun info(message: String) {}
        override fun error(message: String) {}
        override fun debug(message: String) {}
        override fun warn(message: String) {}
    }

    private class TestConnectionManager : FigmaConnectionManager(TestLogger()) {
        var lastMethod: String? = null
        var lastParams: JsonObject? = null
        var responseToReturn: JsonElement? = null

        override suspend fun sendCommand(method: String, params: JsonObject, requestId: String?): JsonElement? {
            lastMethod = method
            lastParams = params
            return responseToReturn
        }
    }

    private class TestIconService : LucideIconService("test/path") {
        var iconToReturn: LucideIconData? = null
        var shouldThrow: Exception? = null

        override fun getIcon(name: String): LucideIconData {
            shouldThrow?.let { throw it }
            return iconToReturn ?: throw IconServiceException("Icon not found")
        }
    }

    @BeforeEach
    fun setUp() {
        // Arrange: Create test doubles
        logger = TestLogger()
        connectionManager = TestConnectionManager()
        iconService = TestIconService()

        // Create tool instance
        tool = CreateLucideIconTool(logger, connectionManager, iconService)
    }

    // ========================================================================
    // TOOL DEFINITION TESTS
    // ========================================================================

    @Test
    fun `test_getDefinition_returnsCorrectToolName`() {
        // Act: Get tool definition
        val definition = tool.getDefinition()

        // Assert: Should have correct name
        assertEquals(FigmaConstants.ToolNames.CREATE_LUCIDE_ICON, definition.name)
        assertTrue(definition.description.contains("Lucide"))
    }

    @Test
    fun `test_getDefinition_hasRequiredParameters`() {
        // Act: Get tool definition
        val definition = tool.getDefinition()

        // Assert: Should require iconName
        val schema = definition.inputSchema as Map<*, *>
        val properties = schema["properties"] as Map<*, *>

        assertTrue(properties.containsKey(FigmaConstants.ParamNames.ICON_NAME))

        val required = schema["required"] as List<*>
        assertTrue(required.contains(FigmaConstants.ParamNames.ICON_NAME))
    }

    @Test
    fun `test_getDefinition_hasOptionalParameters`() {
        // Act: Get tool definition
        val definition = tool.getDefinition()

        // Assert: Should have optional parameters
        val schema = definition.inputSchema as Map<*, *>
        val properties = schema["properties"] as Map<*, *>

        assertTrue(properties.containsKey(FigmaConstants.ParamNames.SIZE))
        assertTrue(properties.containsKey(FigmaConstants.ParamNames.COLOR))
        assertTrue(properties.containsKey(FigmaConstants.ParamNames.X))
        assertTrue(properties.containsKey(FigmaConstants.ParamNames.Y))
    }

    // ========================================================================
    // PARAMETER EXTRACTION TESTS
    // ========================================================================

    @Test
    fun `test_extractParameters_withValidIcon_succeeds`() {
        // Arrange: Mock icon service
        val iconName = "accessibility"
        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = "<svg></svg>",
            categories = listOf("accessibility"),
            tags = listOf("disability"),
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon

        val arguments = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Extract parameters
        val result = tool.extractParameters(arguments)

        // Assert: Should succeed
        assertNotNull(result)
        verify { iconService.getIcon(iconName) }
    }

    @Test
    fun `test_extractParameters_withInvalidIcon_throwsException`() {
        // Arrange: Mock icon service to throw exception
        val iconName = "nonexistent"

        every { iconService.getIcon(iconName) } throws IconServiceException("Icon not found")

        val arguments = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act & Assert: Should throw ParameterValidationException
        assertThrows<ParameterValidationException> {
            tool.extractParameters(arguments)
        }
    }

    // ========================================================================
    // COMMAND BUILDING TESTS
    // ========================================================================

    @Test
    fun `test_buildCommandParams_withDefaultValues_usesDefaults`() {
        // Arrange: Mock icon service
        val iconName = "accessibility"
        val svgContent = "<svg><path d='M10 10'/></svg>"
        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = svgContent,
            categories = listOf("accessibility"),
            tags = listOf("disability"),
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon
        every { iconService.extractSvgPathData(svgContent) } returns svgContent

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Build command params
        val commandParams = tool.buildCommandParams(params)

        // Assert: Should use default values
        assertEquals(iconName, commandParams[FigmaConstants.ParamNames.ICON_NAME]?.jsonPrimitive?.content)
        assertEquals(
            FigmaConstants.Defaults.DEFAULT_ICON_SIZE,
            commandParams[FigmaConstants.ParamNames.SIZE]?.jsonPrimitive?.double
        )
        assertEquals(
            FigmaConstants.Defaults.DEFAULT_ICON_COLOR,
            commandParams[FigmaConstants.ParamNames.COLOR]?.jsonPrimitive?.content
        )
        assertEquals(
            FigmaConstants.Defaults.DEFAULT_POSITION_X,
            commandParams[FigmaConstants.ParamNames.X]?.jsonPrimitive?.double
        )
        assertEquals(
            FigmaConstants.Defaults.DEFAULT_POSITION_Y,
            commandParams[FigmaConstants.ParamNames.Y]?.jsonPrimitive?.double
        )
    }

    @Test
    fun `test_buildCommandParams_withCustomValues_usesCustomValues`() {
        // Arrange: Mock icon service
        val iconName = "heart"
        val customSize = 48.0
        val customColor = "#FF0000"
        val customX = 100.0
        val customY = 200.0
        val svgContent = "<svg><path d='M10 10'/></svg>"

        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = svgContent,
            categories = listOf("social"),
            tags = listOf("love"),
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon
        every { iconService.extractSvgPathData(svgContent) } returns svgContent

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
            put(FigmaConstants.ParamNames.SIZE, customSize)
            put(FigmaConstants.ParamNames.COLOR, customColor)
            put(FigmaConstants.ParamNames.X, customX)
            put(FigmaConstants.ParamNames.Y, customY)
        }

        // Act: Build command params
        val commandParams = tool.buildCommandParams(params)

        // Assert: Should use custom values
        assertEquals(customSize, commandParams[FigmaConstants.ParamNames.SIZE]?.jsonPrimitive?.double)
        assertEquals(customColor, commandParams[FigmaConstants.ParamNames.COLOR]?.jsonPrimitive?.content)
        assertEquals(customX, commandParams[FigmaConstants.ParamNames.X]?.jsonPrimitive?.double)
        assertEquals(customY, commandParams[FigmaConstants.ParamNames.Y]?.jsonPrimitive?.double)
    }

    @Test
    fun `test_buildCommandParams_includesSvgData`() {
        // Arrange: Mock icon service
        val iconName = "star"
        val svgContent = "<svg><path d='M10 10'/></svg>"
        val cleanedSvg = svgContent // After cleaning

        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = svgContent,
            categories = listOf("shapes"),
            tags = emptyList(),
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon
        every { iconService.extractSvgPathData(svgContent) } returns cleanedSvg

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Build command params
        val commandParams = tool.buildCommandParams(params)

        // Assert: Should include SVG data
        assertTrue(commandParams.containsKey("svgData"))
        assertEquals(cleanedSvg, commandParams["svgData"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test_buildCommandParams_includesMetadata`() {
        // Arrange: Mock icon service with metadata
        val iconName = "accessibility"
        val categories = listOf("accessibility", "medical")
        val tags = listOf("disability", "wheelchair")

        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = "<svg></svg>",
            categories = categories,
            tags = tags,
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon
        every { iconService.extractSvgPathData(any()) } returns "<svg></svg>"

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Build command params
        val commandParams = tool.buildCommandParams(params)

        // Assert: Should include metadata
        assertTrue(commandParams.containsKey("categories"))
        assertTrue(commandParams.containsKey("tags"))

        val returnedCategories = commandParams["categories"]?.jsonArray?.map { it.jsonPrimitive.content }
        assertEquals(categories, returnedCategories)

        val returnedTags = commandParams["tags"]?.jsonArray?.map { it.jsonPrimitive.content }
        assertEquals(tags, returnedTags)
    }

    // ========================================================================
    // SUCCESS RESPONSE TESTS
    // ========================================================================

    @Test
    fun `test_formatSuccessResponse_includesIconDetails`() {
        // Arrange: Mock plugin response
        val iconName = "heart"
        val nodeId = "123:456"
        val size = 32.0
        val color = "#FF0000"

        val pluginResponse = buildJsonObject {
            put("nodeId", nodeId)
        }

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
            put(FigmaConstants.ParamNames.SIZE, size)
            put(FigmaConstants.ParamNames.COLOR, color)
        }

        // Act: Format success response
        val result = tool.formatSuccessResponse(pluginResponse, params)

        // Assert: Should include icon details
        assertFalse(result.isError)
        assertTrue(result.content.isNotEmpty())

        val responseText = (result.content.first() as com.figma.mcp.protocol.ToolContent.TextContent).text
        val responseJson = Json.parseToJsonElement(responseText).jsonObject

        assertEquals(iconName, responseJson["iconName"]?.jsonPrimitive?.content)
        assertEquals(size, responseJson["size"]?.jsonPrimitive?.double)
        assertEquals(color, responseJson["color"]?.jsonPrimitive?.content)
        assertEquals(nodeId, responseJson["nodeId"]?.jsonPrimitive?.content)
        assertTrue(responseJson.containsKey("message"))
    }

    @Test
    fun `test_buildSuccessMessage_withNodeId_includesId`() {
        // Arrange: Plugin response with node ID
        val iconName = "star"
        val nodeId = "789:012"

        val pluginResponse = buildJsonObject {
            put("nodeId", nodeId)
        }

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Build success message
        val message = tool.buildSuccessMessage(pluginResponse, params)

        // Assert: Should include node ID
        assertTrue(message.contains(iconName))
        assertTrue(message.contains(nodeId))
        assertTrue(message.contains("Successfully"))
    }

    @Test
    fun `test_buildSuccessMessage_withoutNodeId_stillSucceeds`() {
        // Arrange: Plugin response without node ID
        val iconName = "circle"

        val pluginResponse = buildJsonObject { }

        val params = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Build success message
        val message = tool.buildSuccessMessage(pluginResponse, params)

        // Assert: Should still succeed
        assertTrue(message.contains(iconName))
        assertTrue(message.contains("Successfully"))
    }

    // ========================================================================
    // INTEGRATION TESTS
    // ========================================================================

    @Test
    fun `test_execute_fullWorkflow_succeeds`() = runBlocking {
        // Arrange: Set up full mock chain
        val iconName = "accessibility"
        val nodeId = "100:200"

        val mockIcon = LucideIconService.LucideIcon(
            name = iconName,
            svgContent = "<svg><path d='M10 10'/></svg>",
            categories = listOf("accessibility"),
            tags = listOf("disability"),
            contributors = emptyList()
        )

        every { iconService.getIcon(iconName) } returns mockIcon
        every { iconService.extractSvgPathData(any()) } returns mockIcon.svgContent
        every { connectionManager.hasConnections() } returns true

        val pluginResponse = buildJsonObject {
            put("nodeId", nodeId)
        }

        coEvery {
            connectionManager.sendCommand(any(), any(), any())
        } returns pluginResponse

        val arguments = buildJsonObject {
            put(FigmaConstants.ParamNames.ICON_NAME, iconName)
        }

        // Act: Execute tool
        val result = tool.execute(arguments)

        // Assert: Should succeed
        assertFalse(result.isError)
        assertTrue(result.content.isNotEmpty())

        // Verify command was sent to Figma with correct method
        assertEquals(FigmaConstants.PluginMethods.CREATE_LUCIDE_ICON, connectionManager.lastMethod)

        // Verify parameters were passed correctly
        assertNotNull(connectionManager.lastParams)
        assertEquals(iconName, connectionManager.lastParams?.get(FigmaConstants.ParamNames.ICON_NAME)?.jsonPrimitive?.content)
    }
}
