# FigmaToolExecutor

**Purpose**: Executes Figma-specific tool operations by communicating with connected plugins
**Location**: `server/src/main/kotlin/com/figma/mcp/services/FigmaToolExecutor.kt`
**Dependencies**: FigmaConnectionManager

## Tool Methods

### createRectangle(arguments: JsonObject): CallToolResult
- Validates: `width`, `height` (required)
- Optional: `x`, `y`, `fillColor`
- Sends `createNode` command to plugin

### createText(arguments: JsonObject): CallToolResult
- Validates: `text` (required)
- Optional: `fontSize`, `fontFamily`, `color`
- Sends `createNode` command to plugin

### getSelection(): CallToolResult
- No parameters
- Sends `getInfo` command with `type: "selection"`

### setProperties(arguments: JsonObject): CallToolResult
- Validates: `nodeId`, `properties` (required)
- Sends `setProperties` command to plugin

### getNodeInfo(arguments: JsonObject): CallToolResult
- Validates: `nodeId` (required)
- Sends `getInfo` command to plugin

## Error Handling

All methods return `CallToolResult` with:
- `content: List<ToolContent>` - Result or error message
- `isError: Boolean` - Success/failure flag
