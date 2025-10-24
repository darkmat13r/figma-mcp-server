# File-Specific MCP Routing

## Overview

This document explains the file-specific routing implementation (Approach B) that enables multiple Claude Code sessions and multiple Figma files to work together without commands executing on the wrong files.

## Problem Statement

### Before File-Specific Routing

The original architecture had a critical flaw:

```
Claude Session 1 → SSE (port 1234) → MCP Server → WebSocket (port 8080) → ANY Figma Plugin
Claude Session 2 → SSE (port 1234) → MCP Server → WebSocket (port 8080) → ANY Figma Plugin
```

**Issues:**
- Multiple Claude sessions could send commands to any connected Figma plugin
- Multiple Figma files could receive commands meant for other files
- No way to ensure command routing to the correct file

### After File-Specific Routing

```
Claude Session 1 (File A) → SSE?fileId=abc123 → MCP Server → WebSocket?fileId=abc123 → Figma Plugin (File A)
Claude Session 2 (File B) → SSE?fileId=def456 → MCP Server → WebSocket?fileId=def456 → Figma Plugin (File B)
```

**Benefits:**
- Each Claude session is associated with a specific Figma file
- Commands are routed only to the correct Figma plugin instance
- Multiple files can be edited simultaneously without interference

## Architecture

### Session Management Components

#### 1. SSE Session Manager
**Location:** `/server/src/main/kotlin/com/figma/mcp/session/SseSessionManager.kt`

**Purpose:** Tracks Server-Sent Events (SSE) connections from Claude Code with their associated file IDs.

**Key Features:**
- Maps SSE session IDs to Figma file IDs
- One Claude session per file (replaces old session if file is reopened)
- Thread-safe using ConcurrentHashMap

**API:**
```kotlin
interface ISessionManager<T> {
    fun registerSession(sessionId: String, fileId: String, session: T)
    fun unregisterSession(sessionId: String)
    fun getSessionByFileId(fileId: String): T?
    fun getFileIdForSession(sessionId: String): String?
}
```

#### 2. WebSocket Session Manager
**Location:** `/server/src/main/kotlin/com/figma/mcp/session/WebSocketSessionManager.kt`

**Purpose:** Tracks WebSocket connections from Figma plugins with their associated file IDs.

**Key Features:**
- Maps WebSocket session IDs to Figma file IDs
- Includes sender actor for thread-safe message sending
- One plugin connection per file

**Data Structure:**
```kotlin
data class WebSocketConnectionWrapper(
    val session: DefaultWebSocketServerSession,
    val senderActor: SendChannel<WebSocketMessage>,
    val fileId: String
)
```

#### 3. Route Resolver
**Location:** `/server/src/main/kotlin/com/figma/mcp/session/RouteResolver.kt`

**Purpose:** Determines which WebSocket session to route commands to based on the originating SSE session.

**Routing Logic:**
1. Receive SSE session ID from tool execution
2. Look up file ID for SSE session
3. Find WebSocket session for that file ID
4. Return WebSocket session for command execution

**Result Types:**
```kotlin
sealed class RouteResolutionResult {
    data class Success(val sessionId: String, val fileId: String)
    data class Failure(val error: String, val errorCode: RouteErrorCode)
}
```

### Connection Flow

#### Plugin Connection (Figma → Server)

1. Plugin opens Figma file with `figma.fileKey = "abc123"`
2. Plugin connects to: `ws://localhost:8080/figma?fileId=abc123`
3. Server extracts `fileId` from query parameters
4. WebSocket Session Manager registers the connection:
   - Session ID: `ws_<uuid>`
   - File ID: `abc123`
5. Plugin receives welcome message with session and file info

#### Claude Code Connection (Claude → Server)

1. User configures Claude Desktop with: `http://localhost:1234/sse?fileId=abc123`
2. Claude Code connects via SSE to that URL
3. Server extracts `fileId` from query parameters
4. SSE Session Manager registers the connection:
   - Session ID: `sse_<uuid>`
   - File ID: `abc123`
5. MCP server instance is returned to Claude

#### Command Execution (Claude → Figma)

1. Claude calls a tool (e.g., `create_rectangle`)
2. Tool execution request includes SSE session context
3. Route Resolver determines routing:
   ```
   SSE Session → fileId: abc123 → WebSocket Session
   ```
4. Command is sent to the correct WebSocket session
5. Figma plugin executes command
6. Response flows back through the same route

## Configuration

### Figma Plugin Configuration

The plugin automatically detects its file ID and builds the correct URLs.

**UI Display:**
```
File ID: abc123
MCP URL: http://localhost:1234/sse?fileId=abc123
WebSocket Server: ws://localhost:8080/figma?fileId=abc123
```

Users can copy the MCP URL directly from the plugin UI.

### Claude Desktop Configuration

**Location:** `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS)

**Single File Configuration:**
```json
{
  "mcpServers": {
    "figma-design-system": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123"
      }
    }
  }
}
```

**Multiple File Configuration:**
```json
{
  "mcpServers": {
    "figma-design-system": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123"
      }
    },
    "figma-marketing-site": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=def456"
      }
    }
  }
}
```

## URL Formats

### SSE Endpoint (Claude Code)
```
Format: http://localhost:1234/sse?fileId=<fileId>
Example: http://localhost:1234/sse?fileId=abc123xyz

Parameters:
  - fileId (required): The Figma file key from figma.fileKey
```

### WebSocket Endpoint (Figma Plugin)
```
Format: ws://localhost:8080/figma?fileId=<fileId>
Example: ws://localhost:8080/figma?fileId=abc123xyz

Parameters:
  - fileId (required): The Figma file key from figma.fileKey
```

### Legacy Endpoints (Backward Compatibility)
```
/mcp?fileId=<fileId> - SSE endpoint (deprecated, use /sse)
/?fileId=<fileId> - WebSocket endpoint (deprecated, use /figma)
```

## Error Handling

### Common Errors

#### 1. No File ID Provided
**Error:** "No fileId query parameter provided"

**Cause:** Client attempted to connect without fileId parameter

**Solution:** Ensure URL includes `?fileId=<yourFileId>`

#### 2. No WebSocket for File
**Error:** "No active WebSocket connection for file: abc123"

**Cause:** Figma plugin is not connected for the specified file

**Solution:**
1. Open the Figma file
2. Start the Figma MCP plugin in that file
3. Connect the plugin to the WebSocket server

#### 3. SSE Session Not Found
**Error:** "SSE session not found or has no associated file ID"

**Cause:** Claude Code session disconnected or was never registered

**Solution:** Restart Claude Code connection with correct fileId in URL

### Error Codes

```kotlin
enum class RouteErrorCode {
    SSE_SESSION_NOT_FOUND,          // SSE session doesn't exist
    NO_FILE_ID_FOR_SESSION,         // SSE session has no file ID
    NO_WEBSOCKET_FOR_FILE,          // No plugin connected for file
    WEBSOCKET_SESSION_NOT_FOUND,    // WebSocket session missing
    INVALID_FILE_ID,                // File ID format is invalid
    MULTIPLE_SESSIONS_FOR_FILE      // Conflict detected
}
```

## Diagnostics

### Health Check Endpoint

```bash
curl http://localhost:8080/mcp/health
```

**Response:**
```json
{
  "status": "healthy",
  "server": "figma-mcp-server",
  "version": "2.0.0",
  "transport": "sse",
  "endpoints": {
    "sse": "/sse?fileId=<your-file-id>",
    "mcp": "/mcp?fileId=<your-file-id> (deprecated)"
  },
  "sessions": {
    "type": "SSE",
    "totalSessions": 2,
    "activeFileIds": ["abc123", "def456"],
    "sessionMappings": {
      "sse_uuid1": "abc123",
      "sse_uuid2": "def456"
    }
  }
}
```

### Session Diagnostics Endpoint

```bash
curl http://localhost:8080/diagnostics/sessions
```

**Response:**
```json
{
  "sse": {
    "type": "SSE",
    "totalSessions": 2,
    "activeFileIds": ["abc123", "def456"],
    "sessionMappings": {...}
  }
}
```

### Route Resolver Diagnostics

In logs, the Route Resolver provides:
- Total SSE sessions
- Total WebSocket sessions
- Routable files (files with both SSE and WebSocket)
- SSE-only files (Claude connected, no plugin)
- WebSocket-only files (plugin connected, no Claude)

## Testing

### Manual Testing Steps

1. **Start the MCP Server**
   ```bash
   cd server
   ./gradlew run
   ```

2. **Open First Figma File**
   - Open Figma file A
   - Note the file key in URL: `figma.com/file/abc123/...`
   - Run the MCP plugin
   - Copy the MCP URL from plugin UI

3. **Configure Claude Desktop for File A**
   - Update `claude_desktop_config.json` with File A's MCP URL
   - Restart Claude Desktop

4. **Test File A Commands**
   - Ask Claude to create shapes in File A
   - Verify they appear in correct file

5. **Repeat for File B**
   - Open different Figma file B
   - Note file key: `figma.com/file/def456/...`
   - Run MCP plugin in File B
   - Add second MCP server config for File B
   - Restart Claude Desktop

6. **Test Multi-File Routing**
   - Send commands to File A via Claude session A
   - Send commands to File B via Claude session B
   - Verify no cross-contamination

### Expected Behavior

- Each Claude session only affects its designated Figma file
- Switching between Claude MCP server configs targets different files
- Plugin disconnection only affects that file's commands
- Reconnecting a plugin reestablishes routing for that file

## Implementation Details

### Thread Safety

All session managers use `ConcurrentHashMap` for thread-safe operations:
- Multiple Claude sessions can connect simultaneously
- Multiple Figma plugins can connect simultaneously
- Concurrent command execution is supported

### Actor Pattern

WebSocket sending uses the actor pattern for thread safety:
```kotlin
sealed class WebSocketMessage {
    data class Send(val frame: Frame.Text, val result: CompletableDeferred<Unit>)
    object Close
}
```

Each WebSocket connection has a dedicated sender actor that serializes all sends to prevent race conditions.

### Session Lifecycle

**SSE Session:**
1. Connect → Register with file ID
2. Active → Route commands
3. Disconnect → Unregister, clean up

**WebSocket Session:**
1. Connect → Register with file ID, create sender actor
2. Active → Execute commands, send responses
3. Disconnect → Unregister, close actor, clean up

### Constants

All constants are centralized in `SessionConstants.kt`:
```kotlin
object SessionConstants {
    const val QUERY_PARAM_FILE_ID = "fileId"
    const val WS_PATH_FIGMA = "/figma"
    const val SSE_PATH_SSE = "/sse"
    const val ERROR_NO_FILE_ID = "No fileId query parameter provided"
    const val SESSION_ID_PREFIX_SSE = "sse_"
    const val SESSION_ID_PREFIX_WS = "ws_"
}
```

No magic strings or numbers in the implementation.

## Troubleshooting

### Plugin Shows "Loading..." for File ID
**Cause:** Plugin hasn't received file info yet
**Solution:** Wait a moment; if it persists, restart the plugin

### Commands Execute on Wrong File
**Cause:** File ID mismatch in configuration
**Solution:**
1. Check plugin UI for correct file ID
2. Verify Claude config has matching file ID
3. Restart both plugin and Claude

### "No WebSocket for File" Error
**Cause:** Plugin not connected
**Solution:**
1. Open Figma file
2. Start MCP plugin
3. Click "Connect" button
4. Verify "Connected" status

### Multiple Claude Sessions Interfere
**Cause:** Same file ID used in multiple configs
**Solution:** Each Claude MCP config should have unique file ID

## Future Enhancements

Potential improvements to the routing system:

1. **Session Persistence**: Store session mappings to survive server restarts
2. **Session Expiration**: Auto-cleanup of stale sessions after timeout
3. **Load Balancing**: Support multiple WebSocket connections per file
4. **Authentication**: Add API keys or tokens for security
5. **Session Monitoring**: Real-time dashboard for active sessions
6. **Metrics**: Track command routing statistics and performance

## SOLID Principles Applied

### Single Responsibility Principle
- `SseSessionManager`: Only manages SSE sessions
- `WebSocketSessionManager`: Only manages WebSocket sessions
- `RouteResolver`: Only handles routing logic

### Open-Closed Principle
- New session types can be added without modifying existing managers
- Routing strategies can be extended without changing core logic

### Liskov Substitution Principle
- All session managers implement `ISessionManager<T>`
- Can be substituted without breaking code

### Interface Segregation Principle
- `ISessionManager`: Focused interface for session operations
- `IRouteResolver`: Separate interface for routing
- Clients only depend on methods they use

### Dependency Inversion Principle
- All components depend on abstractions (interfaces)
- Concrete implementations injected via Koin DI
- High-level routing doesn't depend on low-level session details

## Summary

The file-specific routing implementation provides:
- ✅ Isolated routing per Figma file
- ✅ Support for multiple Claude sessions
- ✅ Support for multiple Figma files
- ✅ Thread-safe concurrent operations
- ✅ Comprehensive error handling
- ✅ Easy configuration and setup
- ✅ Production-ready architecture following SOLID principles

Commands from Claude Code now reliably execute in the correct Figma file, enabling teams to work on multiple designs simultaneously without interference.
