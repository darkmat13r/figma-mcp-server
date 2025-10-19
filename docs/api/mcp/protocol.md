# MCP Protocol Interfaces

**Purpose**: Define Model Context Protocol message structures

## Core Types

### MCPRequest
```typescript
{
  id: string           // Unique request identifier
  method: string       // Method name (e.g., "tools/call")
  params?: object      // Method parameters
}
```

### MCPResponse
```typescript
{
  id: string           // Matches request ID
  result?: any         // Success result
  error?: {            // Error details
    code: number
    message: string
    data?: any
  }
}
```

### CallToolRequest
```kotlin
data class CallToolRequest(
  val name: String,           // Tool name
  val arguments: JsonObject   // Tool arguments
)
```

### CallToolResult
```kotlin
data class CallToolResult(
  val content: List<ToolContent>,  // Result content
  val isError: Boolean             // Success/failure
)
```

## Transport Layers

### HTTP/SSE (Port 1234)
- Used by Claude Code to connect to MCP server
- Server-Sent Events for bidirectional communication
- Endpoint: `http://localhost:1234/sse`

### WebSocket (Port 8080)
- Used by Figma plugin to connect to server
- Bidirectional real-time communication
- Endpoint: `ws://localhost:8080/`

## Tool Execution Flow

```
Claude Code → MCP Server (SSE/HTTP)
    ↓
FigmaToolExecutor
    ↓
FigmaConnectionManager → WebSocket
    ↓
Figma Plugin → Figma API
    ↓
Response flows back through same chain
```
