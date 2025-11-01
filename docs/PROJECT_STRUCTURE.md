# Figma MCP Server - Project Structure & Architecture Analysis

## Project Overview

This is a sophisticated Figma plugin that integrates with Model Context Protocol (MCP), allowing Claude Code to programmatically interact with Figma designs through an MCP server. The architecture follows SOLID principles and uses a layered, modular design.

**Key Directories:**
- `/server/` - Kotlin-based MCP server with Ktor framework
- `/figma-plugin/` - TypeScript plugin UI and command handlers
- `/server/src/main/kotlin/com/figma/mcp/` - Server source code

---

## Server Architecture (Kotlin/Ktor)

### 1. Core Transport & Routing

**File:** `/server/src/main/kotlin/com/figma/mcp/routes/McpRoutes.kt`

- **Purpose:** HTTP/SSE endpoints for MCP protocol communication
- **Key Features:**
  - File-specific routing via query parameter (`?fileId=abc123`)
  - SSE (Server-Sent Events) for streaming MCP protocol messages
  - Health check and diagnostics endpoints
  - Session management with file ID tracking

**Pattern:** Each SSE connection is associated with a specific Figma file, allowing proper isolation of commands and responses.

### 2. MCP Server Implementation

**File:** `/server/src/main/kotlin/com/figma/mcp/transport/McpServer.kt`

**Architecture Highlights:**

```
Claude Code (MCP Client)
    ↓ MCP Protocol (stdio/SSE)
McpServer (Official Kotlin SDK)
    ↓ Delegates to
FigmaToolRegistry (Strategy + Registry Pattern)
    ↓ Routes to individual tool implementations
IFigmaTool implementations (CreateFrameTool, etc.)
    ↓ WebSocket to
Figma Plugin
    ↓ Figma Plugin API
Figma Document
```

**Key Responsibilities:**
- **Tool Registration:** Dynamically registers all tools from FigmaToolRegistry
- **Tool Execution:** Generic handler that delegates to registry (no hardcoded switch statements)
- **Prompts:** Registers system prompts for AI behavior guidance (massive 800+ line prompt with design system rules)
- **Resources:** Manages read-only MCP resources for design data
- **Content Types:** Supports TextContent, ImageContent, and EmbeddedResources

**Design Improvements:**
- Legacy version: 370 lines with 5 hardcoded tools (87 lines per tool)
- New version: ~150 lines with dynamic registry (60% reduction)
- Adding tool = 0 lines in McpServer (register in DI only)

### 3. Tool Architecture

#### Base Class Pattern

**File:** `/server/src/main/kotlin/com/figma/mcp/tools/BaseFigmaTool.kt`

Implements **Template Method Pattern** with common steps:
1. Extract parameters → `extractParameters()` (overrideable)
2. Build command → `buildCommandParams()` (abstract - must implement)
3. Send to plugin → `sendCommand()` (common implementation)
4. Format response → `formatSuccessResponse()` (overrideable)

**Key Features:**
- Helper methods for parameter extraction: `getRequiredString()`, `getDoubleOrDefault()`, etc.
- Exception handling: `ParameterValidationException`, `FigmaConnectionException`
- Custom success message building
- JSON serialization utilities

#### Tool Registry

**File:** `/server/src/main/kotlin/com/figma/mcp/tools/FigmaToolRegistry.kt`

**Pattern:** Registry Pattern + Strategy Pattern

**Capabilities:**
- Thread-safe tool registration (ConcurrentHashMap)
- Single source of truth for all tools
- Tool lookup by name
- Tool execution with validation
- Dynamic tool list for MCP tools/list response

**Key Methods:**
```kotlin
registry.register(tool: IFigmaTool)
registry.registerAll(vararg tools: IFigmaTool)
registry.executeTool(toolName: String, arguments: JsonObject): CallToolResult
registry.getAllTools(): List<Tool>  // For MCP tools/list
```

#### Example Tool Implementation

**File:** `/server/src/main/kotlin/com/figma/mcp/tools/impl/CreatePolygonTool.kt`

```kotlin
class CreatePolygonTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_POLYGON) {
    
    override fun getDefinition(): Tool { ... }  // Tool metadata & schema
    override fun validate(arguments: JsonObject): String? { ... }  // Validation
    override fun buildCommandParams(params: JsonObject): JsonObject { ... }  // Command building
    override fun buildSuccessMessage(...): String { ... }  // Response formatting
}
```

**Patterns Applied:**
- Single Responsibility: Only handles polygon creation
- Open-Closed: Closed for modification, open for extension
- Dependency Injection: Logger and connection manager injected

---

## Tool Categories & Registration

### Current Tools (70+ total)

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

Tool names follow convention: `figma_<action>_<subject>`

**Categories:**

1. **Category 1: Node Creation (12 tools)**
   - CreateFrameTool, CreateComponentTool, CreateInstanceTool
   - CreateRectangleTool, CreateEllipseTool, CreateTextTool
   - CreatePolygonTool, CreateStarTool, CreateLineTool
   - CreateGroupTool, CreateSectionTool, CreateBooleanOperationTool

2. **Category 3: Styling (8 tools)**
   - SetFillsTool, SetStrokesTool, SetEffectsTool, SetOpacityTool
   - SetCornerRadiusTool, ApplyPaintStyleTool, ApplyEffectStyleTool, ApplyTextStyleTool

3. **Category 4: Typography (6 tools)**
   - SetTextContentTool, SetTextStyleTool, SetTextAlignmentTool
   - SetTextAutoResizeTool, SetTextTruncationTool, LoadFontTool

4. **Category 5: Components & Variants (7 tools)**
   - CreateComponentFromNodeTool, SetComponentPropertiesTool
   - SetInstancePropertiesTool, DetachInstanceTool, SwapInstanceTool
   - CreateComponentSetTool, AddVariantTool

5. **Category 6: Variables & Tokens (6 tools)**
   - CreateVariableCollectionTool, CreateVariableTool, BindVariableTool
   - GetVariablesTool, SetVariableValueTool, UnbindVariableTool

6. **Category 7: Hierarchy & Query (13 tools)**
   - GetNodeInfoTool, GetSelectionTool, SetSelectionTool
   - FindNodesTool, GetChildrenTool, GetParentTool, MoveNodeTool
   - CloneNodeTool, GetCurrentPageNodesTool, SearchNodesTool
   - GetAllPagesTool, SwitchPageTool, CreatePageTool

7. **Category 9: Image & Media (4 tools)**
   - CreateImageTool, SetImageFillTool, ExportNodeTool, GetImageFillsTool

8. **Category 10: Utility (7 tools)**
   - DeleteNodeTool, ShowNodeTool, RenameNodeTool
   - SetVisibleTool, SetLockedTool, NotifyTool, GetUserInfoTool

9. **Category 11: Style Management (10 tools)**
   - GetStyleByIdTool, GetLocalPaintStylesTool, GetLocalTextStylesTool
   - GetLocalEffectStylesTool, GetLocalGridStylesTool
   - CreatePaintStyleTool, CreateTextStyleTool, CreateEffectStyleTool
   - CreateGridStyleTool, SetFillStyleIdTool

### Tool Registration

**File:** `/server/src/main/kotlin/com/figma/mcp/infrastructure/Di Module.kt`

DI configuration registers all tools:

```kotlin
single {
    val toolRegistry = FigmaToolRegistry(get())
    
    toolRegistry.registerAll(
        // Category 1: Node Creation (12 tools)
        CreateFrameTool(get(), get()),
        CreateComponentTool(get(), get()),
        // ... etc
        
        // Category 3: Styling (8 tools)
        SetFillsTool(get(), get()),
        // ... etc
    )
    
    toolRegistry
}
```

**To Add New Tool:**
1. Create class extending `BaseFigmaTool`
2. Add one line to DI registration
3. Done! Tool automatically available in MCP

---

## Dependency Injection (Koin)

**File:** `/server/src/main/kotlin/com/figma/mcp/infrastructure/Di Module.kt`

**Key Components:**

1. **Core Services**
   - `ILogger`: Logging abstraction (Slf4j implementation)
   - `Json`: JSON serialization with Kotlinx

2. **Session Management**
   - `SseSessionManager`: Manages SSE sessions (Claude Code connections)
   - `WebSocketSessionManager`: Manages WebSocket sessions (Figma plugin connections)
   - `RouteResolver`: Routes commands from SSE to correct WebSocket session

3. **Tool Registry**
   - `FigmaToolRegistry`: Central tool management with all 70+ tools registered

4. **Connection Management**
   - `FigmaConnectionManager`: WebSocket connection pool to Figma plugins

5. **Resource Management**
   - `ExportedImageResourceManager`: Temporary file resources for exports

6. **Transport**
   - `WebSocketRoutes`: WebSocket endpoint for Figma plugin
   - `McpRoutes`: HTTP/SSE endpoints for Claude Code
   - `McpServer`: Official Kotlin SDK MCP implementation

---

## Configuration Management

**Constants File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

**Structure:**
- **ToolNames:** All 70+ tool identifiers
- **PluginMethods:** WebSocket command names
- **NodeTypes:** Figma node type constants
- **ParamNames:** Parameter name constants
- **LayoutModes, BooleanOperations:** Enum-like constants
- **Defaults:** Default values for positions, dimensions, etc.
- **ErrorMessages, SuccessMessages:** Message templates

**Pattern:** Eliminates magic strings/numbers throughout codebase

---

## Figma Plugin Communication

### Plugin-to-Server Protocol

**Files:**
- `/figma-plugin/src/plugin/code.ts` - WebSocket communication
- `/figma-plugin/src/plugin/constants.ts` - Plugin constants (mirrors server)

**Communication Flow:**

```
1. Server sends command via WebSocket:
   {
     type: "createNode",
     params: { type: "FRAME", width: 100, height: 100, x: 0, y: 0 }
   }

2. Plugin receives and routes to handler
   nodeCreationHandlers["FRAME"](params)

3. Handler creates node and sends response:
   {
     success: true,
     nodeId: "ID:abc123"
   }

4. Server receives response and formats for MCP
```

### Handler Registry (Plugin-Side)

**File:** `/figma-plugin/src/plugin/code.ts`

Similar to server, uses Strategy Pattern:

```typescript
const nodeCreationHandlers: Record<string, (params: Record<string, any>) => Promise<SceneNode>> = {
  [NodeTypes.FRAME]: NodeHandlers.createFrame,
  [NodeTypes.COMPONENT]: NodeHandlers.createComponent,
  [NodeTypes.INSTANCE]: NodeHandlers.createInstance,
  [NodeTypes.RECTANGLE]: NodeHandlers.createRectangle,
  // ... etc
};

async function handleCreateNode(params: Record<string, any>, requestId?: string): Promise<void> {
  const handler = nodeCreationHandlers[params.type];
  const node = await handler(params);
  sendWSResponse(requestId, { success: true, nodeId: node.id });
}
```

### Handler Modules

**Files:**
- `nodeHandlers.ts` - Node creation (createFrame, createRectangle, etc.)
- `styleHandlers.ts` - Style operations
- `typographyHandlers.ts` - Text properties
- `componentHandlers.ts` - Component operations
- `variableHandlers.ts` - Design tokens
- `imageHandlers.ts` - Image operations
- `hierarchyHandlers.ts` - Tree operations
- `utilityHandlers.ts` - Utility operations

**Pattern:** Each module exports handlers for specific categories

### Common Properties Application

**File:** `/figma-plugin/src/plugin/nodeHandlers.ts`

```typescript
async function applyCommonProperties(node: SceneNode, params: Record<string, any>): Promise<void> {
    // Position
    if (params.x !== undefined && 'x' in node) {
        (node as LayoutMixin).x = params.x;
    }
    
    // Dimensions, Name, Fills, Strokes, Styles, etc.
    // ...
}
```

---

## Key Architectural Patterns Used

### 1. Registry Pattern
- **Location:** FigmaToolRegistry (server), nodeCreationHandlers (plugin)
- **Benefit:** Extensible without modifying core code
- **Example:** Add new tool → register in DI → automatically available

### 2. Strategy Pattern
- **Handlers are strategies:** Each tool/handler is interchangeable
- **Benefit:** Flexible, testable, composable
- **Example:** Different node types use different strategies

### 3. Template Method Pattern
- **BaseFigmaTool.execute():** Defines algorithm skeleton
- **Benefit:** DRY, consistent error handling
- **Example:** All tools follow same extraction → validation → execution → formatting flow

### 4. Dependency Inversion Principle
- **Abstractions:** IFigmaTool, ILogger, FigmaConnectionManager
- **Benefit:** Loosely coupled, testable, replaceable components

### 5. Single Responsibility Principle
- **Each tool:** ONE responsibility (create frame, set fills, etc.)
- **Each module:** ONE concern (node handlers, styles, variables, etc.)
- **Benefit:** Easy to understand, modify, test

---

## Communication Sequence

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Claude Code (User Request)                                   │
│    "Create a button with 120x40 size"                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 2. MCP Client (Claude Code SDK)                                 │
│    Sends: CallToolRequest("figma_create_frame", {...})          │
│    Protocol: stdio or HTTP/SSE                                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 3. McpRoutes (Ktor HTTP/SSE Handler)                            │
│    Route: GET /sse?fileId=abc123                                │
│    Validates fileId, creates SSE session                        │
│    Passes request to McpServer                                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 4. McpServer (Official Kotlin SDK)                              │
│    handleToolExecution("figma_create_frame", arguments)         │
│    Calls: toolRegistry.executeTool()                            │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 5. FigmaToolRegistry                                            │
│    tool = getTool("figma_create_frame")  // CreateFrameTool     │
│    result = tool.execute(arguments)                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 6. CreateFrameTool (BaseFigmaTool)                              │
│    ├─ extractParameters(arguments)                              │
│    ├─ buildCommandParams(params)                                │
│    ├─ sendCommand("createNode", commandParams) // WebSocket     │
│    └─ formatSuccessResponse(response)                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 7. WebSocket to Figma Plugin (8081)                             │
│    Message: { type: "FRAME", width: 120, height: 40, ... }     │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 8. Plugin WebSocket Handler (code.ts)                           │
│    ├─ handleCreateNode(params)                                  │
│    ├─ handler = nodeCreationHandlers["FRAME"]                   │
│    ├─ node = await handler(params)                              │
│    └─ sendWSResponse({ success: true, nodeId: "ID:xyz" })      │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 9. Plugin Node Handler (nodeHandlers.ts)                        │
│    ├─ const frame = figma.createFrame()                         │
│    ├─ applyCommonProperties(frame, params)                      │
│    └─ figma.currentPage.appendChild(frame)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 10. Figma Plugin API                                            │
│     Creates actual Frame node in Figma document                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 11. Response Back Through Chain                                 │
│     Plugin → WebSocket → Tool → Registry → McpServer → SSE      │
│     → MCP Client → Claude Code                                  │
│     Response: { nodeId: "ID:xyz", success: true }               │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│ 12. Claude Code                                                 │
│     "Successfully created frame with ID: ID:xyz"                │
│     Can now perform next operation                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Coding Conventions

### Server-Side (Kotlin)

1. **Naming:**
   - Tools: `<Action><Subject>Tool` (CreateFrameTool, SetFillsTool)
   - Tool names: `figma_<verb>_<noun>` (figma_create_frame)
   - Classes inherit from `BaseFigmaTool`

2. **Constants:**
   - All magic values in `FigmaConstants.kt`
   - Organized by category (ToolNames, NodeTypes, ParamNames, etc.)

3. **Error Handling:**
   - Custom exceptions: `ParameterValidationException`, `FigmaConnectionException`
   - Always include logging at key points
   - Return `CallToolResult` with `isError = true` for failures

4. **Logging:**
   - Template: `logger.info("Message", "key" to value)`
   - Lifecycle: "→ START", "  Step details", "← RETURN"
   - Errors: `logger.error("Message", exception, context)`

### Plugin-Side (TypeScript)

1. **Naming:**
   - Handlers: `<verb><noun>` (createFrame, setFills)
   - Constants mirrored from server
   - Types from `types.ts`

2. **Async Handlers:**
   - All handlers return `Promise<SceneNode>` or `Promise<void>`
   - Use `await applyCommonProperties(node, params)`
   - Always `figma.currentPage.appendChild(node)`

3. **Parameter Handling:**
   - Extract with defaults: `params[ParamNames.WIDTH] ?? Defaults.DEFAULT_WIDTH`
   - Validate before use
   - Type safety with TypeScript

4. **Error Handling:**
   - Throw `Error` with descriptive message
   - Caught by `handleCreateNode` wrapper
   - Response includes error in message

---

## Configuration & Constants

### Server Constants

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

```kotlin
object FigmaConstants {
    object ToolNames {
        const val CREATE_FRAME = "figma_create_frame"
        const val CREATE_POLYGON = "figma_create_polygon"
        // ... 70+ tools
    }
    
    object Defaults {
        const val DEFAULT_POSITION_X = 0.0
        const val DEFAULT_POSITION_Y = 0.0
        const val MIN_POLYGON_SIDES = 3
        const val MAX_POLYGON_SIDES = 100
    }
    
    object ErrorMessages {
        fun missingParam(paramName: String) = "Missing required parameter: $paramName"
    }
}
```

### Plugin Constants

**File:** `/figma-plugin/src/plugin/constants.ts`

```typescript
export const NodeTypes = {
  FRAME: 'FRAME',
  POLYGON: 'POLYGON',
  STAR: 'STAR',
  // ... 12 types
};

export const PluginMethods = {
  CREATE_NODE: 'createNode',
  // ... 70+ methods
};

export const Defaults = {
  MIN_POLYGON_SIDES: 3,
  MAX_POLYGON_SIDES: 100,
};
```

---

## Key Design Principles

### 1. DRY (Don't Repeat Yourself)
- Constants defined once, used everywhere
- BaseFigmaTool provides common execution flow
- Handler registry patterns eliminate switch statements

### 2. SOLID Principles
- **S**: Each tool has one responsibility
- **O**: New tools don't require modifying existing code
- **L**: All tools can substitute BaseFigmaTool
- **I**: Tools depend on abstractions (IFigmaTool, ILogger)
- **D**: Depends on abstractions, not concrete implementations

### 3. Separation of Concerns
- Server: MCP protocol, tool coordination
- Registry: Tool management and routing
- Tools: Implementation of specific operations
- Plugin: Figma API interaction

### 4. Testability
- Tools are independently testable
- Mock connections possible
- No hardcoded dependencies

### 5. Extensibility
- Add new tool = 1 class + 1 registration line
- Plugin-side mirrors server-side organization
- No modification to existing code needed

---

## Important Files Reference

| Component | File | Lines | Purpose |
|-----------|------|-------|---------|
| MCP Server | McpServer.kt | 1187 | MCP protocol integration |
| Tool Registry | FigmaToolRegistry.kt | 234 | Tool management |
| Base Tool | BaseFigmaTool.kt | 399 | Common tool logic |
| Routes | McpRoutes.kt | 145 | HTTP/SSE endpoints |
| Constants | FigmaConstants.kt | 200+ | All constants |
| DI Module | Di Module.kt | 300+ | Dependency injection |
| Plugin Code | code.ts | 500+ | WebSocket handling |
| Constants (Plugin) | constants.ts | 344 | Plugin constants |
| Node Handlers | nodeHandlers.ts | 500+ | Node creation |

---

## For Adding Lucide Icon Tool

**Key Insights:**

1. **Server-Side:**
   - Create `CreateLucideIconTool extends BaseFigmaTool`
   - Define tool name in FigmaConstants.ToolNames
   - Register in Di Module.kt

2. **Plugin-Side:**
   - Add icon SVG handler to nodeHandlers.ts or iconHandlers.ts (new file)
   - Support icon name parameter → fetch SVG → create vector group
   - Add plugin method constant in constants.ts

3. **Parameters Needed:**
   - `iconName`: Lucide icon identifier (e.g., "search", "menu")
   - `size`: Icon size in pixels (default 24)
   - `color`: Color hex or bind to variable
   - `strokeWidth`: Stroke thickness
   - `x`, `y`: Position

4. **Response:**
   - Return icon node ID
   - Icon should be a component with variants for different icon types

5. **Integration Points:**
   - FigmaConstants.ToolNames.CREATE_LUCIDE_ICON = "figma_create_lucide_icon"
   - Di Module: `CreateLucideIconTool(get(), get())`
   - Plugin: Add lucideIconHandlers to handler registry

6. **SVG Handling:**
   - Import Lucide SVG data (base64 or URLs)
   - Convert to vector shapes in Figma
   - Handle fill/stroke from parameters
   - Bind colors to design system variables
