# Figma MCP Integration - Complete Flow Documentation

This document provides a comprehensive view of how Claude Code integrates with Figma through the MCP Server.

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Startup Sequence](#startup-sequence)
3. [Message Flow Examples](#message-flow-examples)
4. [Code Walkthrough](#code-walkthrough)
5. [Error Handling](#error-handling)
6. [Performance Considerations](#performance-considerations)

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        User                                       │
└─────────────┬────────────────────────────────────────────────────┘
              │
              │ Natural language request
              │ "Create a red rectangle in Figma"
              │
┌─────────────▼────────────────────────────────────────────────────┐
│                     Claude Code                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  MCP Client (Built-in)                                     │  │
│  │  - Launches MCP servers as subprocesses                    │  │
│  │  - Manages stdio communication                             │  │
│  │  - Decides when to use tools                               │  │
│  └────────────────┬───────────────────────────────────────────┘  │
└─────────────────┼─────────────────────────────────────────────────┘
                  │
                  │ JSON-RPC 2.0 over stdin/stdout
                  │ {"jsonrpc":"2.0","method":"tools/call",...}
                  │
┌─────────────────▼─────────────────────────────────────────────────┐
│               Figma MCP Server (Ktor Application)                 │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ Application.kt (Main Entry Point)                            ││
│  │  - Starts HTTP server (port 8080)                            ││
│  │  - Launches StdioTransport (background coroutine)            ││
│  │  Line 76-81: environment.monitor.subscribe(ApplicationStarted)││
│  └────────┬──────────────────────────────────────┬──────────────┘│
│           │                                       │               │
│           │ stdio                                 │ HTTP          │
│           │                                       │               │
│  ┌────────▼──────────────┐           ┌───────────▼─────────────┐ │
│  │ StdioTransport        │           │ WebSocketRoutes         │ │
│  │ (Line 54: start())    │           │ (Figma Plugin Handler)  │ │
│  │                       │           │                         │ │
│  │ 1. Read stdin line    │           │ 1. Accept WS connection │ │
│  │ 2. Parse JSON-RPC     │           │ 2. Register client      │ │
│  │ 3. Route to command   │           │ 3. Forward messages     │ │
│  └────────┬──────────────┘           └───────────┬─────────────┘ │
│           │                                       │               │
│           └───────────┬───────────────────────────┘               │
│                       │                                           │
│           ┌───────────▼────────────────────┐                      │
│           │  CommandRegistry               │                      │
│           │  - Registered handlers by name │                      │
│           │  - initialize                  │                      │
│           │  - tools/list                  │                      │
│           │  - tools/call                  │                      │
│           └───────────┬────────────────────┘                      │
│                       │                                           │
│     ┌─────────────────┼─────────────────┐                         │
│     │                 │                 │                         │
│ ┌───▼────┐      ┌─────▼─────┐    ┌─────▼─────┐                   │
│ │Initialize│    │ListTools  │    │CallTool   │                   │
│ │Command   │    │Command    │    │Command    │                   │
│ │(Line 40) │    │(Line 51)  │    │(Line 65)  │                   │
│ └──────────┘    └───────────┘    └─────┬─────┘                   │
│                                         │                         │
│                            ┌────────────▼───────────────┐         │
│                            │  FigmaToolExecutor         │         │
│                            │  (Line 56-279)             │         │
│                            │  - createRectangle()       │         │
│                            │  - createText()            │         │
│                            │  - getSelection()          │         │
│                            │  - setProperties()         │         │
│                            │  - getNodeInfo()           │         │
│                            └────────────┬───────────────┘         │
│                                         │                         │
│                            ┌────────────▼───────────────┐         │
│                            │ FigmaConnectionManager     │         │
│                            │ (Line 75)                  │         │
│                            │ - sendCommand()            │         │
│                            │ - Request/response match   │         │
│                            └────────────┬───────────────┘         │
└──────────────────────────────────────────┼─────────────────────────┘
                                           │
                                           │ WebSocket
                                           │ ws://localhost:8080/figma
                                           │
┌──────────────────────────────────────────▼─────────────────────────┐
│                      Figma Plugin                                  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  Plugin Code (TypeScript/JavaScript)                         │ │
│  │  - Connects to ws://localhost:8080/figma                     │ │
│  │  - Receives commands: createNode, getInfo, setProperties     │ │
│  │  - Calls Figma Plugin API                                    │ │
│  │  - Returns results                                           │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
└───────────────────────────┼───────────────────────────────────────┘
                            │
                            │ Figma Plugin API
                            │
┌───────────────────────────▼───────────────────────────────────────┐
│                    Figma Document                                 │
│                                                                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │ Rectangle  │  │    Text    │  │   Frame    │                 │
│  │   Node     │  │    Node    │  │    Node    │  ...            │
│  └────────────┘  └────────────┘  └────────────┘                 │
└───────────────────────────────────────────────────────────────────┘
```

---

## Startup Sequence

### 1. Claude Code Starts MCP Server

When Claude Code starts, it reads `~/.config/claude/mcp_config.json`:

```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": ["-jar", "/path/to/server-all.jar"],
      "env": {"LOG_LEVEL": "info"}
    }
  }
}
```

Claude Code then:
```bash
# Spawns subprocess
java -jar /path/to/server-all.jar

# Sets up pipes:
# - stdin: Claude → Server
# - stdout: Server → Claude
# - stderr: Server logs
```

### 2. Server Application Starts

**Entry Point**: `Application.kt:29`

```kotlin
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}
```

**Module Configuration**: `Application.kt:48`

```kotlin
fun Application.module() {
    // 1. Configure DI (Koin)
    configureDependencyInjection()  // Line 50

    // 2. Configure WebSocket
    configureWebSockets()  // Line 53

    // 3. Configure JSON
    configureSerialization()  // Line 56

    // 4. Configure CORS/HTTP
    configureHTTP()  // Line 59

    // 5. Configure logging
    configureMonitoring()  // Line 62

    // 6. Configure routes (WebSocket endpoint)
    configureRouting()  // Line 65

    // 7. ⭐ START STDIO TRANSPORT ⭐
    val stdioTransport: StdioTransport by inject()

    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            log.info("Starting MCP stdio transport...")
            stdioTransport.start()  // BLOCKS reading stdin
        }
    }
}
```

**Result**:
- HTTP server listening on port 8080
- WebSocket endpoint at `/figma`
- Stdio transport running in background coroutine
- Reading stdin line by line

### 3. Initialize Handshake

**Step 3.1**: Claude sends initialize request

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "clientInfo": {
      "name": "claude-code",
      "version": "1.0.0"
    },
    "capabilities": {}
  }
}
```

**Step 3.2**: StdioTransport receives message

`StdioTransport.kt:60`
```kotlin
val line = reader.readLine() ?: break
processMessage(line)
```

**Step 3.3**: Parse and route

`StdioTransport.kt:94`
```kotlin
val request = json.decodeFromString<MCPRequest>(message)
val handler = commandRegistry.get(request.method)  // "initialize"
val result = handler.execute(context)
```

**Step 3.4**: InitializeCommand executes

`InitializeCommand.kt:40`
```kotlin
override suspend fun execute(context: CommandContext): CommandResult {
    val result = InitializeResult(
        protocolVersion = "2024-11-05",
        capabilities = ServerCapabilities(
            tools = ToolsCapability(listChanged = true),
            resources = ResourcesCapability(listChanged = true, subscribe = false),
            prompts = PromptsCapability(listChanged = true)
        ),
        serverInfo = Implementation(
            name = "figma-mcp-server",
            version = "1.0.0"
        ),
        instructions = "Figma MCP Server provides tools to interact with Figma..."
    )

    return CommandResult(
        success = true,
        data = json.encodeToJsonElement(result)
    )
}
```

**Step 3.5**: Response sent to stdout

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": {
      "name": "figma-mcp-server",
      "version": "1.0.0"
    },
    "capabilities": {
      "tools": {"listChanged": true},
      "resources": {"listChanged": true, "subscribe": false},
      "prompts": {"listChanged": true}
    },
    "instructions": "Figma MCP Server..."
  }
}
```

**Step 3.6**: Claude reads response from stdout
- Stores server capabilities
- Now knows server supports tools

### 4. Figma Plugin Connects (Parallel)

While MCP handshake happens, Figma plugin connects:

**Step 4.1**: User opens plugin in Figma Desktop App

**Step 4.2**: Plugin connects to WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8080/figma');
```

**Step 4.3**: Server registers connection

`FigmaConnectionManager.kt:48`
```kotlin
fun registerConnection(clientId: String, session: DefaultWebSocketServerSession) {
    connections[clientId] = session
    logger.info("Figma plugin connected", "clientId" to clientId)
}
```

**Result**: Server now has active WebSocket to Figma

---

## Message Flow Examples

### Example 1: Create Rectangle

#### User Input
```
"Create a red rectangle 200x100 pixels in Figma"
```

#### Flow

**1. Claude decides to use a tool**

Claude's reasoning:
- User wants to create something in Figma
- Tool available: `figma_create_rectangle`
- Required params: width, height
- Optional params: fillColor

**2. Claude sends tools/call request**

```json
{
  "jsonrpc": "2.0",
  "id": "5",
  "method": "tools/call",
  "params": {
    "name": "figma_create_rectangle",
    "arguments": {
      "width": 200,
      "height": 100,
      "fillColor": "#FF0000"
    }
  }
}
```

**3. StdioTransport routes to CallToolCommand**

`StdioTransport.kt:107`
```kotlin
val handler = commandRegistry.get("tools/call")
```

**4. CallToolCommand parses and routes**

`CallToolCommand.kt:86`
```kotlin
val result = when (params.name) {
    "figma_create_rectangle" -> figmaToolExecutor.createRectangle(params.arguments)
    // ...
}
```

**5. FigmaToolExecutor executes**

`FigmaToolExecutor.kt:56`
```kotlin
suspend fun createRectangle(arguments: JsonObject): CallToolResult {
    val width = arguments["width"]?.jsonPrimitive?.doubleOrNull ?: error(...)
    val height = arguments["height"]?.jsonPrimitive?.doubleOrNull ?: error(...)
    val fillColor = arguments["fillColor"]?.jsonPrimitive?.contentOrNull

    // Send to Figma plugin
    val result = figmaConnectionManager.sendCommand(
        method = "createNode",
        params = buildJsonObject {
            put("type", "RECTANGLE")
            put("width", width)
            put("height", height)
            put("fillColor", fillColor)
        }
    )

    return CallToolResult(
        content = listOf(
            ToolContent.TextContent(
                text = "Successfully created rectangle (200x100) with fill color #FF0000"
            )
        ),
        isError = false
    )
}
```

**6. FigmaConnectionManager sends WebSocket command**

`FigmaConnectionManager.kt:90`
```kotlin
val command = buildJsonObject {
    put("id", "req_123_1234567890")
    put("method", "createNode")
    put("params", params)
}

connection.send(Frame.Text(command.toString()))

// Wait for response
return withTimeout(5000) {
    responseChannel.receive()
}
```

**7. Figma Plugin receives and executes**

```javascript
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);

    if (message.method === 'createNode') {
        const rect = figma.createRectangle();
        rect.resize(message.params.width, message.params.height);
        rect.fills = [{
            type: 'SOLID',
            color: hexToRgb(message.params.fillColor)
        }];

        // Send response
        ws.send(JSON.stringify({
            id: message.id,
            result: { success: true, nodeId: rect.id }
        }));
    }
};
```

**8. Response flows back**

WebSocket → ConnectionManager → ToolExecutor → CallToolCommand → StdioTransport → stdout

**9. Claude receives result**

```json
{
  "jsonrpc": "2.0",
  "id": "5",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Successfully created rectangle (200x100) with fill color #FF0000"
      }
    ],
    "isError": false
  }
}
```

**10. Claude responds to user**

```
I've created a red rectangle in Figma with dimensions 200x100 pixels.
```

---

### Example 2: Get Selection

#### User Input
```
"What's currently selected in Figma?"
```

#### Simplified Flow

```
User
  → Claude Code
    → tools/call: figma_get_selection
      → CallToolCommand
        → FigmaToolExecutor.getSelection()
          → FigmaConnectionManager.sendCommand("getInfo")
            → WebSocket to Figma Plugin
              → figma.currentPage.selection
                → WebSocket response
                  → ToolExecutor formats result
                    → Claude presents to user
```

---

## Code Walkthrough

### Critical Path: Tool Execution

**File**: `StdioTransport.kt`
**Function**: `processMessage(message: String)` (Line 88)

```kotlin
private suspend fun processMessage(message: String) {
    try {
        // 1. Parse JSON-RPC
        val request = json.decodeFromString<MCPRequest>(message)

        // 2. Validate
        if (request.id.isEmpty() || request.method.isEmpty()) {
            sendError(request.id, ErrorCode.INVALID_REQUEST, "Missing id or method")
            return
        }

        // 3. Get handler
        val handler = commandRegistry.get(request.method)
        if (handler == null) {
            sendError(request.id, ErrorCode.METHOD_NOT_FOUND, "Method '${request.method}' not found")
            return
        }

        // 4. Validate params
        val validationError = handler.validate(request.params)
        if (validationError != null) {
            sendError(request.id, ErrorCode.INVALID_PARAMS, validationError)
            return
        }

        // 5. Create context
        val context = CommandContext(
            requestId = request.id,
            clientId = "stdio",
            params = request.params
        )

        // 6. Execute
        val result = handler.execute(context)

        // 7. Send response
        if (result.success) {
            sendResponse(request.id, result.data)
        } else {
            sendError(request.id, result.error?.code ?: -32603, result.error?.message ?: "Failed")
        }
    } catch (e: Exception) {
        logger.error("Error processing message", e)
        sendError("", ErrorCode.INTERNAL_ERROR, "Internal error: ${e.message}")
    }
}
```

**Key Points**:
- Every error path sends proper JSON-RPC error
- Validation happens before execution
- Context carries request metadata
- All handlers return CommandResult

---

### WebSocket Connection Management

**File**: `FigmaConnectionManager.kt`
**Function**: `sendCommand()` (Line 75)

```kotlin
suspend fun sendCommand(
    method: String,
    params: JsonObject,
    timeout: Long = 5000
): JsonElement? {
    // 1. Get connection
    val connection = connections.values.firstOrNull()
        ?: throw IllegalStateException("No Figma plugin connected")

    // 2. Generate unique request ID
    val requestId = generateRequestId()  // "req_123_1234567890"

    // 3. Create response channel
    val responseChannel = Channel<JsonElement?>(1)
    pendingRequests[requestId] = responseChannel

    try {
        // 4. Build command
        val command = buildJsonObject {
            put("id", requestId)
            put("method", method)
            put("params", params)
        }

        // 5. Send via WebSocket
        connection.send(Frame.Text(command.toString()))

        // 6. Wait for response with timeout
        return withTimeout(timeout) {
            responseChannel.receive()  // Blocks until response
        }
    } finally {
        // 7. Cleanup
        pendingRequests.remove(requestId)
        responseChannel.close()
    }
}
```

**Key Points**:
- Async request/response matching via channels
- Timeout protection (default 5s)
- Automatic cleanup
- Thread-safe with ConcurrentHashMap

---

## Error Handling

### JSON-RPC Error Codes

| Code | Name | When It Happens |
|------|------|-----------------|
| -32700 | Parse Error | Invalid JSON in message |
| -32600 | Invalid Request | Missing `id` or `method` |
| -32601 | Method Not Found | Unknown method name |
| -32602 | Invalid Params | Missing/invalid parameters |
| -32603 | Internal Error | Server-side exception |

### Error Flow Example

**Bad Request**:
```json
{"method":"tools/call"}  // Missing "id"
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": "",
  "error": {
    "code": -32600,
    "message": "Missing id or method"
  }
}
```

### Tool Execution Errors

If tool execution fails:

```kotlin
CallToolResult(
    content = listOf(
        ToolContent.TextContent(text = "Error: No Figma plugin connected")
    ),
    isError = true  // ⭐ Error flag
)
```

Claude sees this and can retry or inform user.

---

## Performance Considerations

### Concurrency Model

- **Stdio**: Single coroutine reading stdin sequentially
- **WebSocket**: One coroutine per connection
- **Command Execution**: Async with coroutines
- **Request Matching**: ConcurrentHashMap for thread safety

### Bottlenecks

1. **WebSocket Timeout**: Default 5s per command
   - Can be adjusted per tool
   - Long-running operations should increase timeout

2. **Single Plugin Connection**: Current implementation uses first available
   - Future: Route to specific plugin by document ID

3. **Stdio Blocking**: Each message processed sequentially
   - Not an issue for normal usage (Claude sends one request at a time)

### Scalability

**Current**:
- Single Claude Code instance
- Multiple Figma plugins supported
- Commands queued per plugin

**Future**:
- Multiple MCP clients (different Claude instances)
- Load balancing across plugins
- Connection pooling

---

## Summary

The integration works through a carefully orchestrated flow:

1. **Claude Code** launches the server as a subprocess
2. **Server** starts both HTTP (WebSocket) and stdio transports
3. **Initialize** handshake establishes MCP session
4. **Figma Plugin** connects via WebSocket (independent of MCP)
5. **Tool calls** flow: Claude → stdio → Server → WebSocket → Figma
6. **Responses** flow back through the same chain
7. **Error handling** at every layer ensures robustness

The key insight is the **dual-transport bridge**: the server translates between MCP protocol (stdio) and Figma's WebSocket API, enabling Claude to control Figma seamlessly.

---

**End of Integration Flow Documentation**

For more details, see:
- `SETUP_GUIDE.md` - Complete setup instructions
- `QUICK_START.md` - 5-minute quick start
- `IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- `MCP_IMPLEMENTATION.md` - MCP protocol specifics