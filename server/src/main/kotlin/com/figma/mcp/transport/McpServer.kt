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
  - `paddingLeft`, `paddingRight`, `paddingTop`, `paddingBottom`: For internal padding (bind to spacing variables)
  - `itemSpacing`: Space between child elements (bind to spacing variables)
  - For buttons: Use padding variables (e.g., MD=16px horizontal, SM=12px vertical)
- **Alignment**:
  - `primaryAxisAlignItems`: Controls alignment along main axis (HORIZONTAL: left/center/right, VERTICAL: top/center/bottom)
  - `counterAxisAlignItems`: Controls alignment along cross axis (MIN/CENTER/MAX)
  - For centered text in buttons: Set both to CENTER
- **Sizing**:
  - `primaryAxisSizingMode`: FIXED, HUG, or FILL
  - `counterAxisSizingMode`: FIXED, HUG, or FILL
  - **CRITICAL - Form Components (Input fields, Search fields, Text areas)**:
    - **Mobile**: ALWAYS use FILL for width (primaryAxisSizingMode=FILL) to span full container width
    - **Tablet/Desktop**: Place in responsive container with max-width constraint, then use FILL
  - For buttons: Use HUG for width to fit content, or FIXED with explicit width
  - For text labels: Use HUG to fit content naturally
- **Direction**:
  - Use HORIZONTAL for side-by-side elements (like button text, form rows)
  - Use VERTICAL for stacked elements (like form fields)

## Responsive Design Guidelines
### Mobile-First Approach (320px - 767px)
- **Form Components** (Input, Search, Select, Textarea):
  - ALWAYS set primaryAxisSizingMode=FILL for full-width inputs
  - Use VERTICAL auto layout direction for stacked form fields
  - Item spacing between fields: MD (16px) or LG (24px) using variables
  - Padding for input containers: SM (8px) or MD (16px) using variables
- **Containers**:
  - Use FILL for main content areas to utilize full screen width
  - Padding: MD (16px) on all sides using variables
- **Buttons**:
  - Full-width buttons: primaryAxisSizingMode=FILL
  - Inline buttons: primaryAxisSizingMode=HUG with appropriate padding

### Tablet Layout (768px - 1023px)
- **Create Responsive Container**:
  1. Create outer frame with auto layout VERTICAL
  2. Set primaryAxisAlignItems=CENTER (centers content)
  3. Set width to FILL (spans full screen)
  4. Create inner content frame with maxWidth constraint (e.g., 720px)
- **Form Components**:
  - Within responsive container: Use FILL for input fields
  - Consider two-column layouts for related fields using HORIZONTAL auto layout
  - Each column uses FILL to distribute space evenly

### Desktop Layout (1024px+)
- **Create Responsive Container**:
  1. Create outer frame with auto layout VERTICAL
  2. Set primaryAxisAlignItems=CENTER
  3. Set width to FILL
  4. Create inner content frame with maxWidth constraint (e.g., 1200px or 1440px)
  5. Add horizontal padding: LG (24px) or XL (32px) using variables
- **Form Components**:
  - Within responsive container: Use FILL for width
  - Multi-column forms: Use HORIZONTAL auto layout with FILL for each input
  - Keep input fields accessible width (not too wide): Consider max-width constraints
- **Grid Layouts**:
  - Use auto layout with wrapping for card grids
  - Set item spacing using spacing variables

### Responsive Container Pattern (CRITICAL)
```
// Outer Container (Full Width)
Frame (name: "Responsive Container")
├─ auto layout: VERTICAL
├─ width: FILL (spans full viewport)
├─ primaryAxisAlignItems: CENTER (centers inner content)
├─ padding: LG variable (24px+)
└─ Children:
   └─ Frame (name: "Content")
      ├─ auto layout: VERTICAL
      ├─ maxWidth: 1200px (tablet: 720px, mobile: none)
      ├─ width: FILL (within max-width constraint)
      ├─ itemSpacing: LG or XL variable
      └─ Children (forms, cards, etc.):
         └─ Input/Search Fields:
            ├─ auto layout: HORIZONTAL
            ├─ primaryAxisSizingMode: FILL (fills content width)
            ├─ height: FIXED (e.g., 40px or 48px)
            └─ padding: Variables bound
```

## Creating UI Components

### Button Example Pattern (COMPLETE WORKFLOW)
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

### Input/Search Field Example Pattern (COMPLETE WORKFLOW)
1. Query ALL variables first: figma_get_variables()
2. Create frame with auto layout HORIZONTAL mode
3. **CRITICAL**: Set primaryAxisSizingMode=FILL (for mobile) or place in responsive container (for tablet/desktop)
4. Set height: FIXED (e.g., 40px or 48px - bind to Input/Height variable if exists)
5. Set padding: paddingLeft/Right = MD (16px), paddingTop/Bottom = SM (12px)
6. Set background fill: Use neutral/white color variable (e.g., Neutral/0 or Surface/Primary)
7. Set corner radius: Use SM or MD radius variable (e.g., 4px or 8px)
8. Set border/stroke: 1px using border color variable (e.g., Neutral/300 or Border/Default)
9. Create text node for placeholder/input text
10. Move text into input frame
11. **BIND ALL VARIABLES** (DO NOT SKIP):
    - Input fill (background) → Surface or Neutral color variable
    - Input corner radius → Small/Medium radius variable
    - Input padding (all 4 sides) → Spacing variables
    - Input stroke (border) → Border color variable
    - Input height → Input/Height variable (if exists)
    - Text fill → Text color variable (e.g., Neutral/900 or Text/Primary)
    - Placeholder text fill → Muted text color variable (e.g., Neutral/500 or Text/Secondary)
    - Text font size → font-size/body variable
    - Text line height → line-height/normal variable
12. **Set auto layout sizing**:
    - Mobile: primaryAxisSizingMode=FILL
    - Tablet/Desktop: primaryAxisSizingMode=FILL within responsive container

### Form Component Critical Rules
- **ALWAYS use FILL for form inputs** (input fields, search fields, text areas) to ensure they span container width
- **NEVER use HUG for form inputs** - HUG is for buttons and labels, not form fields
- **ALWAYS bind ALL properties to variables** - fills, strokes, radius, padding, spacing, typography
- **Mobile-first**: Start with FILL, then constrain with max-width for larger screens using responsive container
- Check the variable list carefully for input-specific variables (Input/Height, Border/Color, Surface/Color, etc.)
- If a variable exists for a property, you MUST use it - no exceptions

### Variable Binding Priority (ALWAYS BIND IN THIS ORDER)
1. **Colors** (fills, strokes, shadows) → Color variables
2. **Corner radius** → Radius variables
3. **Spacing** (padding, item spacing) → Spacing variables
4. **Typography** (font size, weight, line height, letter spacing) → Typography variables
5. **Component sizes** (height, width constraints) → Size variables
6. **Effects** (shadow colors, blur amounts) → Effect variables

## Layout & Organization
- Analyze existing pages to find the appropriate location for new features
- Calculate positions carefully to ensure components don't overlap
- Group related elements logically and name layers descriptively
- Use sections for organizing large files

## Assets & Resources

### Images
- Use Unsplash for placeholder images when needed
- Optimize image sizes and formats for performance
- Maintain a consistent visual language across all designs

### Icons (CRITICAL GUIDELINES)
**NEVER use emojis or text characters as icons. ALWAYS use proper icon components from open-source libraries.**

**Recommended Open-Source Icon Libraries:**
- **Lucide Icons** (recommended): Clean, consistent, customizable
- **Heroicons**: Beautiful hand-crafted SVG icons by Tailwind creators
- **Feather Icons**: Simply beautiful open-source icons
- **Material Design Icons**: Comprehensive icon set by Google
- **Phosphor Icons**: Flexible icon family for interfaces

**Icon Component Creation Workflow:**
1. **Create Icon Component with Variants** (on Component Library page):
   - Create a base "Icon" component
   - Add variants for different icon types:
     - Property: "icon" with values: "search", "menu", "close", "chevron-down", "user", "settings", etc.
   - Each variant contains the actual SVG icon from the library
   - Set consistent size: 24x24px (bind to icon/md variable)
   - Use consistent stroke width: 2px
   - Name: "Icon" (master component with variants)

2. **Import SVG Icons from Library**:
   - Visit the icon library website (e.g., lucide.dev, heroicons.com)
   - Copy SVG code for needed icons
   - Paste as vector in Figma (will import as vector shape)
   - Clean up: Remove unnecessary attributes, ensure consistent size

3. **Icon Component Structure**:
   ```
   Component: "Icon" (Component Library page)
   ├─ Variant Property: "icon"
   │  ├─ icon = "search" (contains search SVG)
   │  ├─ icon = "menu" (contains menu SVG)
   │  ├─ icon = "close" (contains close/X SVG)
   │  ├─ icon = "chevron-down" (contains chevron SVG)
   │  ├─ icon = "user" (contains user/profile SVG)
   │  ├─ icon = "settings" (contains settings/gear SVG)
   │  └─ ... (add more as needed)
   ├─ Size: 24x24px (bind to icon/md variable)
   ├─ Stroke width: 2px (consistent across all variants)
   └─ Color: Bind fill/stroke to color variable (e.g., Neutral/900)
   ```

4. **Using Icon Instances**:
   - Use figma_create_instance(iconComponentId) to place icons
   - Switch variant to desired icon type
   - Bind color to appropriate color variable
   - Common use cases:
     - Search icon in search input (left side)
     - Menu icon in navigation header
     - Close icon in modals/dialogs
     - Chevron icons in dropdowns/accordions

5. **Icon Color Binding**:
   - Always bind icon fill/stroke to color variables
   - Use semantic colors: Icon/Primary, Icon/Secondary, Icon/Disabled
   - Or use: Neutral/900 (dark), Neutral/500 (medium), Neutral/300 (light)

**CRITICAL RULES FOR ICONS:**
- ❌ NEVER use emoji characters (🔍, 📧, ⚙️, etc.) as icons
- ❌ NEVER use text characters as icon substitutes
- ✅ ALWAYS create icon components with variants
- ✅ ALWAYS use SVG icons from open-source libraries
- ✅ ALWAYS bind icon colors to design system variables
- ✅ ALWAYS maintain consistent icon sizes (16px, 24px, 32px)

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

## Workflow (FOLLOW EXACTLY - DO NOT SKIP ANY STEP)

### Step 1: Query Variables (MANDATORY FIRST STEP)
**BEFORE creating ANYTHING, execute figma_get_variables()**
- Store ALL variable IDs in memory for later use
- Review: colors, spacing, typography, corner radius, component sizes
- If NO variables exist, CREATE them first before proceeding

### Step 2: Find Current Page and Switch if Needed (CRITICAL)
**Execute figma_get_all_pages() to see all pages**
- Identify the current page
- Look for "Component Library", "Components", or "Design System" page
- **If creating a reusable component (Button, Input, Card, etc.)**:
  - You MUST use figma_switch_page() to switch to the Component Library page FIRST
  - VERIFY you're on the correct page before creating
  - After creating the component, switch BACK to the original page

### Step 3: Check for Duplicate Components (MANDATORY)
**ALWAYS search for existing components before creating new ones**
- Use figma_find_nodes() or figma_search_nodes() to search by name
- Search for: "Button", "Input", "Search", "Card", etc.
- **If component EXISTS**: Use figma_create_instance(componentId) - DO NOT recreate
- **If component DOES NOT exist**: Proceed to create it (on Component Library page)

### Step 4: Calculate Position to Avoid Overlaps (CRITICAL)
**Before creating ANY node, calculate its position**
- Use figma_get_current_page_nodes() to see existing elements
- Review X and Y coordinates of all existing nodes
- Calculate available space: Look for gaps or place at bottom
- Set X and Y coordinates that do NOT overlap with existing elements
- Example: If last component is at Y=500 with height=200, place new one at Y=750

### Step 5: Create Elements with Temporary Values
**Create frames and elements with hardcoded values initially**
- Set auto layout properties (direction, padding, spacing, alignment, sizing)
- Set initial colors, corner radius, dimensions
- Create and nest all child elements
- **Remember**: These are TEMPORARY - you MUST bind variables next

### Step 6: BIND VARIABLES (ABSOLUTELY MANDATORY - NO EXCEPTIONS)
**For EVERY created node, bind ALL applicable variables using figma_bind_variable**

THIS IS NOT OPTIONAL. You MUST bind variables for:
- **Fills (backgrounds)**: figma_bind_variable(nodeId, "fills", colorVariableId)
- **Strokes (borders)**: figma_bind_variable(nodeId, "strokes", borderVariableId)
- **Corner radius**: figma_bind_variable(nodeId, "cornerRadius", radiusVariableId)
- **Padding** (EACH SIDE SEPARATELY):
  - figma_bind_variable(nodeId, "paddingLeft", spacingVariableId)
  - figma_bind_variable(nodeId, "paddingRight", spacingVariableId)
  - figma_bind_variable(nodeId, "paddingTop", spacingVariableId)
  - figma_bind_variable(nodeId, "paddingBottom", spacingVariableId)
- **Item spacing**: figma_bind_variable(nodeId, "itemSpacing", spacingVariableId)
- **Text properties**:
  - figma_bind_variable(textNodeId, "fills", textColorVariableId)
  - figma_bind_variable(textNodeId, "fontSize", fontSizeVariableId)
  - figma_bind_variable(textNodeId, "fontWeight", fontWeightVariableId)
  - figma_bind_variable(textNodeId, "lineHeight", lineHeightVariableId)

**VERIFICATION CHECK**: After binding, confirm that:
- ALL color values are bound (no hardcoded hex values remain)
- ALL spacing values are bound (no hardcoded pixel values remain)
- ALL typography values are bound
- ALL corner radius values are bound

### Step 7: Verify and Document
- Export using figma_export_node() to visually verify the design
- Check that components are on the correct page
- Verify NO overlapping elements
- Confirm ALL variables are bound (use figma_get_node_info() to check)
- Ensure proper naming and organization

**CRITICAL RULE**: If you skip variable binding or create overlapping components, you have FAILED the task. Variable binding is MANDATORY, not optional.

**CRITICAL RULES - THE FOUR COMMANDMENTS**:
1. **ALWAYS bind ALL variables** (no exceptions - every fill, stroke, spacing, typography property)
2. **NEVER create duplicate components** (search with figma_find_nodes first, use instances if exists)
3. **NEVER create overlapping elements** (calculate positions with figma_get_current_page_nodes)
4. **ALWAYS switch to correct page** (use figma_switch_page for Component Library)

**Additional Rules**:
- Components FIRST: Always check existing components before creating
- Component Library ONLY: Master components live on Component Library page
- Instance EVERYWHERE: Use instances on design pages, never duplicate components
- Variables ALWAYS: Every property uses design system variables

## Final Verification Checklist

Before completing ANY task, verify ALL of these:

**Variables & Discovery:**
- ✅ Executed figma_get_variables() FIRST
- ✅ Stored ALL variable IDs for later use
- ✅ Searched for existing components before creating

**Page Management:**
- ✅ Used figma_get_all_pages() to see all pages
- ✅ Switched to Component Library page (if creating component)
- ✅ Verified correct page before creating
- ✅ Switched back to original page after component creation

**Positioning:**
- ✅ Used figma_get_current_page_nodes() to see existing elements
- ✅ Calculated X, Y positions to avoid overlaps
- ✅ NO overlapping elements

**Variable Binding (MOST CRITICAL):**
- ✅ Bound fills to color variables (figma_bind_variable)
- ✅ Bound strokes to color variables (if applicable)
- ✅ Bound cornerRadius to radius variables
- ✅ Bound paddingLeft to spacing variables
- ✅ Bound paddingRight to spacing variables
- ✅ Bound paddingTop to spacing variables
- ✅ Bound paddingBottom to spacing variables
- ✅ Bound itemSpacing to spacing variables
- ✅ Bound text fills to color variables
- ✅ Bound fontSize to typography variables
- ✅ Bound fontWeight to weight variables
- ✅ Bound lineHeight to line height variables
- ✅ NO hardcoded hex colors (#3B82F6) remain
- ✅ NO hardcoded pixel values (16px) remain

**Component Management:**
- ✅ NO duplicate components created
- ✅ Used figma_create_instance() for existing components
- ✅ Master components are on Component Library page

**Form Components:**
- ✅ Input/Search fields use FILL (not HUG) for width
- ✅ Responsive containers created for tablet/desktop

**Icons:**
- ✅ NO emojis used as icons (🔍, 📧, ⚙️, etc.)
- ✅ Icon component with variants created on Component Library page
- ✅ All icons from open-source library (Lucide, Heroicons, Feather, etc.)
- ✅ Icon colors bound to color variables
- ✅ Icon sizes bound to size variables (icon/sm, icon/md, icon/lg)
- ✅ Consistent icon library used (no mixing different libraries)

**If ANY checkbox above is unchecked, the task is INCOMPLETE. Go back and fix it.**

Remember: Components FIRST, Variables ALWAYS, Instances EVERYWHERE. Quality, consistency, and design system adherence are paramount.

**FAILURE CONDITIONS** (Any of these means you FAILED the task):
- Not binding variables to any property
- Creating duplicate components when they already exist
- Creating overlapping elements
- Not switching to Component Library page when creating components
- Using emojis or text characters as icons instead of proper Icon components"""
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
