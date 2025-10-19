# Plugin WebSocket Integration

**Purpose**: Handles WebSocket communication between plugin and MCP server
**Locations**:
- `figma-plugin/src/ui/hooks/useWebSocket.ts`
- `figma-plugin/src/plugin/code.ts`

## Message Flow

### Server → Plugin (Commands)
Messages with `method` field are tool execution commands:
```typescript
{
  id: "req_123_...",
  method: "createNode",
  params: { type: "TEXT", text: "Hello" }
}
```

### Plugin → Server (Responses)
Responses include the original request ID:
```typescript
{
  id: "req_123_...",
  result: { success: true, nodeId: "1:2" }
}
```

## useWebSocket Hook

**Methods:**
- `connect(url: string)` - Establish WebSocket connection
- `disconnect()` - Close connection
- `sendRequest(request: MCPRequest)` - Send MCP request
- `clearLogs()` - Clear console logs

**State:**
- `connectionState: ConnectionState` - Current connection status
- `lastResponse: MCPResponse | null` - Last received response
- `error: string | null` - Last error message
- `logs: ConsoleLog[]` - Message log history

## Plugin Command Handler

**Function:** `handleWSCommand(command: any)`

**Supported Methods:**
- `createNode` - Creates Figma nodes (rectangle, text, etc.)
- `getInfo` - Retrieves selection or node information
- `setProperties` - Updates node properties

**Helpers:**
- `sendWSResponse(requestId: string, result: any)` - Send response to server
- `createNode(nodeType: string, properties: object, requestId?: string)` - Create node with optional response
