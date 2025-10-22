package com.figma.mcp.transport

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.tools.FigmaToolRegistry
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Figma MCP Server using Official Kotlin SDK
 *
 * ## Purpose
 * This class wraps the official Model Context Protocol Kotlin SDK to create
 * a Figma-specific MCP server that dynamically exposes all registered Figma tools.
 *
 * ## Architecture
 * ```
 * Claude Code (MCP Client)
 *     ↓ MCP Protocol (stdio/SSE)
 * McpServer (this class - using official SDK)
 *     ↓ Delegates to
 * FigmaToolRegistry (Strategy + Registry Pattern)
 *     ↓ Routes to individual tool implementations
 * IFigmaTool implementations (CreateFrameTool, CreateRectangleTool, etc.)
 *     ↓ WebSocket
 * Figma Plugin
 *     ↓ Figma Plugin API
 * Figma Document
 * ```
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Only responsible for MCP protocol integration and tool registration
 * - Tool execution logic delegated to FigmaToolRegistry
 * - Individual tool implementations in separate classes
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: Add new tools by registering them in FigmaToolRegistry
 * - Closed for modification: This class doesn't change when adding new tools
 * - No hardcoded tool list - all tools come from registry
 *
 * ### Dependency Inversion Principle (DIP)
 * - Depends on FigmaToolRegistry abstraction
 * - Individual tools injected via registry, not directly instantiated
 *
 * ## Design Improvements (vs Legacy Version)
 *
 * ### Before (Legacy - 5 hardcoded tools)
 * - 370 lines of code
 * - Hardcoded tool registration (87 lines per tool)
 * - Hardcoded tool handlers (switch statement)
 * - Violates Open-Closed Principle
 * - Adding tool = modify 3 sections of code
 *
 * ### After (New - Dynamic registry)
 * - ~150 lines of code (60% reduction)
 * - Dynamic tool registration from registry
 * - Single generic handler for all tools
 * - Follows Open-Closed Principle
 * - Adding tool = 0 lines in this file (register in DI module only)
 *
 * ## Usage Example
 * ```kotlin
 * // In DI configuration
 * val toolRegistry = FigmaToolRegistry(logger)
 * toolRegistry.registerAll(
 *     CreateFrameTool(...),
 *     CreateRectangleTool(...),
 *     // ... 10 more Category 1 tools
 * )
 * val mcpServer = McpServer(logger, toolRegistry)
 * mcpServer.start()
 * ```
 *
 * ## Tools Provided (Dynamic - from Registry)
 * The server exposes all tools registered in FigmaToolRegistry.
 * As of now (Category 1 complete):
 * 1. figma_create_frame - Create container frames
 * 2. figma_create_component - Create reusable components
 * 3. figma_create_instance - Create component instances
 * 4. figma_create_rectangle - Create rectangle shapes
 * 5. figma_create_ellipse - Create ellipse/circle shapes
 * 6. figma_create_text - Create text nodes
 * 7. figma_create_polygon - Create polygon shapes
 * 8. figma_create_star - Create star shapes
 * 9. figma_create_line - Create line shapes
 * 10. figma_create_group - Group nodes together
 * 11. figma_create_section - Create organizational sections
 * 12. figma_create_boolean_operation - Boolean operations (union, subtract, etc.)
 *
 * Plus 3 legacy tools (to be migrated to registry):
 * - figma_get_selection - Get current selection
 * - figma_set_properties - Set node properties
 * - figma_get_node_info - Get node information
 */
class McpServer(
    private val logger: ILogger,
    private val toolRegistry: FigmaToolRegistry
) {
    private val server: Server
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Create the MCP server using the official SDK
        server = Server(
            serverInfo = Implementation(
                name = "figma-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(
                        listChanged = true
                    ),
                    prompts = ServerCapabilities.Prompts(
                        listChanged = true
                    )
                )
            )
        )

        // Register all Figma tools from the registry
        registerToolsFromRegistry()

        // Register prompts
        registerPrompts()

        logger.info(
            "McpServer initialized with official Kotlin SDK",
            "toolCount" to toolRegistry.getToolCount()
        )
    }

    /**
     * Register all Figma tools from the registry with the MCP server
     *
     * This method dynamically registers all tools from FigmaToolRegistry.
     * It follows the Open-Closed Principle - adding new tools requires
     * no modification to this code.
     *
     * ### How it works:
     * 1. Get all tool definitions from registry
     * 2. For each tool, register it with the MCP SDK
     * 3. Attach a generic handler that delegates to the registry
     *
     * ### Benefits:
     * - No hardcoded tool list
     * - No switch/if-else statements
     * - Adding tool = 0 lines changed in this file
     * - Single source of truth (FigmaToolRegistry)
     */
    private fun registerToolsFromRegistry() {
        val tools = toolRegistry.getAllTools()

        if (tools.isEmpty()) {
            logger.warn("No Figma tools registered in FigmaToolRegistry")
            return
        }

        tools.forEach { toolDefinition ->
            // Pass the complete input schema directly to the SDK
            // The schema already includes type, properties, and required fields
            val inputSchema = Tool.Input(
                properties = toolDefinition.inputSchema["properties"] as? kotlinx.serialization.json.JsonObject
                    ?: kotlinx.serialization.json.buildJsonObject { },
                required = (toolDefinition.inputSchema["required"] as? kotlinx.serialization.json.JsonArray)
                    ?.map { element ->
                        element.toString().removeSurrounding("\"")
                    }
                    ?: emptyList()
            )

            // Register the tool with MCP SDK
            server.addTool(
                name = toolDefinition.name,
                description = toolDefinition.description,
                inputSchema = inputSchema
            ) { request ->
                // Generic handler that delegates to the registry
                handleToolExecution(toolDefinition.name, request)
            }

            logger.debug(
                "Registered Figma tool with MCP SDK",
                "toolName" to toolDefinition.name
            )
        }

        logger.info(
            "Successfully registered all Figma tools from registry",
            "count" to tools.size,
            "toolNames" to toolRegistry.getAllToolNames().joinToString(", ")
        )
    }

    /**
     * Register system prompts with the MCP server
     *
     * Prompts are templates for common interactions that guide the AI's behavior.
     * This method registers a UI/UX professional designer prompt that instructs
     * the AI on best practices for Figma design work.
     */
    private fun registerPrompts() {
        try {
            // Register the UI/UX Professional Designer prompt
            server.addPrompt(
                name = "figma-designer",
                description = "Professional UI/UX designer persona for creating Figma designs following best practices"
            ) {
                GetPromptResult(
                    description = "You are a professional UI/UX designer working in Figma",
                    messages = listOf(
                        PromptMessage(
                            role = Role.user,
                            content = TextContent(
                                text = """You are a professional UI/UX designer working in Figma. Follow these principles:

## Design System & Variables - CRITICAL RULES
- **STEP 1**: ALWAYS start by querying ALL existing variables using figma_get_variables
- **STEP 2**: NEVER use hardcoded values - EVERY property must use a variable if one exists
- **STEP 3**: After creating ANY node, IMMEDIATELY bind ALL applicable variables

### Variables to Bind (Complete Checklist):
**For Buttons/Containers:**
- Background fill → Bind to color variable (e.g., Primary, Primary/500)
- Corner radius → Bind to corner radius variable (e.g., Medium = 8px)
- Padding values → Bind to spacing variables (e.g., MD = 16px, SM = 8px)
- Item spacing → Bind to spacing variables
- Drop shadow color → Should match or complement fill color variable

**For Text:**
- Fill color → Bind to color variable (e.g., Neutral/100 for white text)
- Font size → Bind to typography variable (e.g., font-size/body = 16px)
- Font weight → Bind to font weight variable (e.g., font-weight/medium = 500)
- Line height → Bind to line height variable (e.g., line-height/normal = 1.5)
- Letter spacing → Bind to letter spacing variable if applicable

**For Spacing:**
- Use spacing variables: XS=4px, SM=8px, MD=16px, LG=24px, XL=32px, XXL=48px
- Bind padding/margins to these variables, not hardcoded numbers

### How to Bind Variables:
After creating a node, use figma_bind_variable for EACH property:
```
figma_bind_variable(nodeId, field="fills", variableId="VariableID:xxx")
figma_bind_variable(nodeId, field="cornerRadius", variableId="VariableID:xxx")
figma_bind_variable(nodeId, field="topLeftPadding", variableId="VariableID:xxx")
// etc. for ALL applicable properties
```

### If Variables Don't Exist:
Create a complete design system with variable collections for:
- Colors: Primary, secondary, neutral, semantic (success, error, warning, info)
- Spacing: XS, SM, MD, LG, XL, XXL
- Corner radius: Small, Medium, Large, Full
- Typography: Font sizes, weights, line heights, letter spacing
- Component sizes: Button/Height, Input/Height, Icon sizes

## Auto Layout Best Practices
- **ALWAYS use auto layout** for containers (frames) instead of absolute positioning
- **Spacing Properties**:
  - `paddingLeft`, `paddingRight`, `paddingTop`, `paddingBottom`: For internal padding
  - `itemSpacing`: Space between child elements
  - For buttons: Use padding (e.g., 16px horizontal, 12px vertical)
- **Alignment**:
  - `primaryAxisAlignItems`: Controls alignment along main axis (HORIZONTAL: left/center/right, VERTICAL: top/center/bottom)
  - `counterAxisAlignItems`: Controls alignment along cross axis (MIN/CENTER/MAX)
  - For centered text in buttons: Set both to CENTER
- **Sizing**:
  - `primaryAxisSizingMode`: FIXED or HUG (hug content)
  - `counterAxisSizingMode`: FIXED or HUG
  - For buttons with centered text: Use HUG for width to fit content, or FIXED with explicit width
- **Direction**:
  - Use HORIZONTAL for side-by-side elements (like button text)
  - Use VERTICAL for stacked elements

## Creating UI Components
**Button Example Pattern (COMPLETE WORKFLOW)**:
1. Query ALL variables first: figma_get_variables()
2. Create frame with auto layout HORIZONTAL mode
3. Set padding using VARIABLES: paddingLeft/Right = MD (16px), paddingTop/Bottom = SM (12px)
4. Set alignment: primaryAxisAlignItems=CENTER, counterAxisAlignItems=CENTER
5. Set background fill using PRIMARY color variable
6. Set corner radius using MEDIUM radius variable
7. Add drop shadow with color matching/complementing button color
8. Create text node with white text
9. Move text into button frame
10. **BIND ALL VARIABLES** (DO NOT SKIP):
    - Button fill → Primary color variable
    - Button corner radius → Medium radius variable
    - Button padding (all 4 sides) → Spacing variables
    - Text fill → Neutral/100 or white color variable
    - Text font size → font-size/body variable
    - Text font weight → font-weight/medium variable
    - Text line height → line-height/normal variable

**Critical Rules**:
- NEVER skip variable binding - it's not optional
- ALWAYS bind ALL properties that have matching variables
- Check the variable list carefully for typography, spacing, and color variables
- If a variable exists for a property, you MUST use it

## Layout & Organization
- Analyze existing pages to find the appropriate location for new features
- Calculate positions carefully to ensure components don't overlap
- Group related elements logically and name layers descriptively
- Use sections for organizing large files

## Assets & Resources
- Use Unsplash for placeholder images when needed
- Optimize image sizes and formats for performance
- Maintain a consistent visual language across all designs

## Component Library First (CRITICAL WORKFLOW)

### Step 1: Check Component Library
Before creating ANYTHING, check if a component already exists:
1. **Get all pages**: figma_get_all_pages()
2. **Look for** "Component Library", "Components", or "Design System" page
3. **Search for components**: figma_search_nodes() for "Button", "Input", "Card", etc.
4. **If component exists**: Use figma_create_instance(componentId) to reuse it
5. **If component doesn't exist**: Create it as a reusable component on the Component Library page

### Step 2: Component vs Feature Decision
- **Building a reusable component** (button, input, card):
  - Switch to "Component Library" page
  - Create using figma_create_component()
  - Build with full variable bindings
  - Return to original page
  - Use figma_create_instance() to place instances

- **Building a feature/screen**:
  - Use figma_create_instance() for existing components
  - Only create new elements for unique, non-reusable content

## Workflow
1. **Query Variables FIRST**: ALWAYS run figma_get_variables() before creating anything
   - Review ALL variables: colors, spacing, typography, corner radius
   - Note the variable IDs for binding later
2. **Check Component Library**: Search for existing components before creating new ones
   - Use figma_get_all_pages() and figma_search_nodes()
   - Reuse existing components via figma_create_instance()
3. **Analyze**: Review existing design pages and components
4. **Plan**: Determine if creating component or feature
   - New component → Create on Component Library page
   - Feature → Use component instances
5. **Design**: Create elements using:
   - Auto layout with proper padding and spacing
   - Hardcoded values initially (for creation)
   - Components for reusable elements
6. **BIND VARIABLES** (MANDATORY STEP - DO NOT SKIP):
   - Systematically bind EVERY property that has a matching variable
   - Button fills → Color variables
   - Corner radius → Radius variables
   - Padding (all 4 sides) → Spacing variables
   - Text fills → Color variables
   - Font size → Typography variables
   - Font weight → Weight variables
   - Line height → Line height variables
   - Letter spacing → Spacing variables (if applicable)
7. **Verify**:
   - Using components where appropriate (not recreating)
   - ALL properties use variables (no hardcoded values remain)
   - Proper padding and centered content
   - Components are on Component Library page
   - Adherence to design system

**CRITICAL RULES**:
- Components FIRST: Always check existing components before creating
- Component Library ONLY: Master components live on Component Library page
- Instance EVERYWHERE: Use instances on design pages, never duplicate components
- Variables ALWAYS: Every property uses design system variables

Remember: Components FIRST, Variables ALWAYS, Instances EVERYWHERE. Quality, consistency, and design system adherence are paramount."""
                            )
                        )
                    )
                )
            }

            logger.info("Successfully registered Figma designer prompt")
        } catch (e: Exception) {
            logger.error("Failed to register prompts", e)
        }
    }

    /**
     * Generic tool execution handler
     *
     * This single handler replaces 5+ hardcoded handlers in the legacy version.
     * It delegates execution to the FigmaToolRegistry which routes to the
     * appropriate IFigmaTool implementation.
     *
     * ### Benefits:
     * - DRY: No code duplication across handlers
     * - Extensible: Works for all current and future tools
     * - Consistent: Same error handling for all tools
     * - Maintainable: Single place to update handler logic
     *
     * @param toolName The name of the tool to execute
     * @param request The MCP SDK CallToolRequest
     * @return CallToolResult in SDK format
     */
    private suspend fun handleToolExecution(
        toolName: String,
        request: CallToolRequest
    ): CallToolResult {
        return try {
            logger.info(
                "→ MCP handleToolExecution() STARTED",
                "toolName" to toolName,
                "hasArguments" to (request.arguments.toString() != "{}")
            )

            // Execute the tool via registry
            logger.info("  Calling toolRegistry.executeTool()...", "toolName" to toolName)
            val result = toolRegistry.executeTool(
                toolName = toolName,
                arguments = request.arguments,
                validateArgs = true
            )
            logger.info(
                "  ✓ toolRegistry.executeTool() returned",
                "toolName" to toolName,
                "isError" to result.isError,
                "contentCount" to result.content.size
            )

            // Convert our CallToolResult to SDK's CallToolResult format
            logger.info("  Converting result to SDK format...", "toolName" to toolName)
            val sdkResult = CallToolResult(
                content = result.content.map { content ->
                    when (content) {
                        is ToolContent.TextContent -> {
                            TextContent(text = content.text)
                        }
                        is ToolContent.ImageContent -> {
                            // SDK supports images too, but we'll use text for now
                            TextContent(text = "[Image: ${content.mimeType}]")
                        }
                        is ToolContent.EmbeddedResource -> {
                            TextContent(text = "[Resource: ${content.resource.uri}]")
                        }
                    }
                },
                isError = result.isError
            )
            logger.info(
                "  ✓ Conversion complete, returning result",
                "toolName" to toolName,
                "sdkContentCount" to sdkResult.content.size
            )

            logger.info("← MCP handleToolExecution() RETURNING", "toolName" to toolName)
            sdkResult
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error(
                "✗ ERROR in handleToolExecution",
                e,
                "toolName" to toolName,
                "errorType" to e.javaClass.simpleName
            )
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Tool execution failed: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }

    /**
     * Get the MCP Server instance for HTTP/SSE transport
     * This allows the Ktor routing to handle MCP requests via SSE
     */
    fun getServer(): Server {
        return server
    }

    /**
     * Start the MCP server with SSE transport (HTTP-based)
     *
     * When running as an HTTP server with Ktor, the MCP protocol
     * is exposed via Server-Sent Events (SSE) endpoints.
     * The actual connection handling is done by Ktor routing.
     */
    suspend fun start() {
        try {
            logger.info(
                "MCP server initialized and ready for SSE transport",
                "registeredTools" to toolRegistry.getToolCount()
            )
            logger.info("Server will accept connections via HTTP/SSE endpoints")
            // When using SSE transport, the connection is handled by Ktor routing
            // No need to start stdio transport
        } catch (e: Exception) {
            logger.error("Failed to initialize MCP server", e)
            throw e
        }
    }

    /**
     * Stop the MCP server
     */
    fun stop() {
        logger.info("Stopping MCP server...")
        // The SDK handles cleanup when the transport is closed
    }

    /**
     * Get statistics about registered tools (for monitoring/debugging)
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "toolCount" to toolRegistry.getToolCount(),
            "toolNames" to toolRegistry.getAllToolNames(),
            "serverVersion" to "1.0.0",
            "sdkVersion" to "official-kotlin-sdk"
        )
    }
}
