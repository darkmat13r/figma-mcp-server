# FigmaConnectionManager

**Purpose**: Manages WebSocket connections to Figma plugins and handles request/response matching
**Location**: `server/src/main/kotlin/com/figma/mcp/services/FigmaConnectionManager.kt`

## Responsibilities

- Maintains pool of active Figma plugin connections
- Generates unique request IDs
- Matches responses to pending requests
- Handles connection lifecycle

## Public API

- `registerConnection(clientId: String, session: DefaultWebSocketServerSession)` - Register new plugin
- `unregisterConnection(clientId: String)` - Cleanup disconnected plugin
- `handleResponse(requestId: String, result: JsonElement?)` - Process plugin response
- `sendCommand(method: String, params: JsonObject, timeout: Long = 5000): JsonElement?` - Send command and wait
- `hasConnections(): Boolean` - Check if any plugins connected
- `getConnectionCount(): Int` - Get active connection count

## Connection Flow

1. Plugin connects via WebSocket (registered in WebSocketRoutes)
2. Connection registered with `registerConnection()`
3. Tools send commands via `sendCommand()`
4. Plugin responses handled by `handleResponse()`
5. Cleanup on disconnect via `unregisterConnection()`
