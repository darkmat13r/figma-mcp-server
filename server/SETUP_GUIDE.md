# Figma MCP Server - Complete Setup Guide

This guide walks you through setting up the Figma MCP Server to work with Claude Code.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Server Build & Configuration](#server-build--configuration)
4. [Claude Code Configuration](#claude-code-configuration)
5. [Figma Plugin Setup](#figma-plugin-setup)
6. [Testing the Integration](#testing-the-integration)
7. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

The Figma MCP Server enables Claude Code to interact with Figma designs through a multi-layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                      Claude Code                            │
│                    (MCP Client)                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ stdio (JSON-RPC 2.0 over stdin/stdout)
                     │
┌────────────────────▼────────────────────────────────────────┐
│                 MCP Server (Ktor)                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │  StdioTransport (Application.kt:73-87)             │    │
│  │  - Reads JSON-RPC from stdin                       │    │
│  │  - Writes responses to stdout                      │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│  ┌────────────▼───────────────────────────────────────┐    │
│  │  Command Registry & MCP Commands                   │    │
│  │  - InitializeCommand (protocol handshake)          │    │
│  │  - ListToolsCommand (available tools)              │    │
│  │  - CallToolCommand (execute tool)                  │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│  ┌────────────▼───────────────────────────────────────┐    │
│  │  FigmaToolExecutor                                 │    │
│  │  - Translates MCP tool calls to Figma commands     │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│  ┌────────────▼───────────────────────────────────────┐    │
│  │  FigmaConnectionManager                            │    │
│  │  - Manages WebSocket connections to plugins        │    │
│  │  - Request/response matching                       │    │
│  └────────────┬───────────────────────────────────────┘    │
└───────────────┼─────────────────────────────────────────────┘
                │
                │ WebSocket (ws://localhost:8080/figma)
                │
┌───────────────▼─────────────────────────────────────────────┐
│              Figma Plugin                                   │
│              (TypeScript/JavaScript)                        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Figma Plugin API
                 │
┌────────────────▼────────────────────────────────────────────┐
│              Figma Document                                 │
│              (Design Nodes, Layers, etc.)                   │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

1. **StdioTransport** (server/src/main/kotlin/com/figma/mcp/transport/StdioTransport.kt:1)
   - Handles stdio communication with Claude Code
   - Parses JSON-RPC 2.0 messages
   - Routes to command handlers

2. **MCP Commands** (server/src/main/kotlin/com/figma/mcp/commands/mcp/):
   - `InitializeCommand.kt` - Protocol handshake
   - `ListToolsCommand.kt` - Returns available tools
   - `CallToolCommand.kt` - Executes tool calls

3. **FigmaToolExecutor** (server/src/main/kotlin/com/figma/mcp/services/FigmaToolExecutor.kt:1)
   - Implements Figma-specific tools
   - Translates MCP calls to Figma commands

4. **FigmaConnectionManager** (server/src/main/kotlin/com/figma/mcp/services/FigmaConnectionManager.kt:1)
   - Manages WebSocket connections
   - Handles async request/response

---

## Prerequisites

Before you begin, ensure you have:

- **Java 17 or higher**
  ```bash
  java -version
  # Should show version 17+
  ```

- **Gradle 8.x** (included via wrapper)
  ```bash
  cd server
  ./gradlew --version
  ```

- **Claude Code installed**
  - Download from: https://claude.ai/download

- **Figma Desktop App**
  - Required for plugin execution
  - Download from: https://www.figma.com/downloads/

---

## Server Build & Configuration

### Step 1: Build the Server

The server has already been built and is ready to use:

```bash
cd server

# If you need to rebuild:
./gradlew clean assemble shadowJar -x test

# Verify the JAR exists:
ls -lh build/libs/server-all.jar
# Should show ~25MB file
```

**Output Location**: `server/build/libs/server-all.jar`

### Step 2: Understand the Startup Modes

The server supports **dual-mode operation** configured in `Application.kt`:

#### Mode 1: WebSocket Only (Direct Plugin)
```bash
# Not typical for Claude Code integration
java -jar build/libs/server-all.jar
```
- Starts HTTP server on port 8080
- Figma plugin connects via WebSocket
- No MCP/stdio transport

#### Mode 2: Full Integration (WebSocket + Stdio) **[RECOMMENDED]**
```bash
# This is what Claude Code uses
java -jar build/libs/server-all.jar
```
- Starts HTTP server on port 8080 (for Figma plugin)
- **Also starts stdio transport** (Application.kt:76-81)
- Listens on stdin for MCP protocol messages
- Perfect for Claude Code integration

**Key Code** (Application.kt:73-87):
```kotlin
val stdioTransport: StdioTransport by inject()

environment.monitor.subscribe(ApplicationStarted) {
    launch {
        log.info("Starting MCP stdio transport for Claude Code integration...")
        stdioTransport.start()
    }
}
```

This means the server automatically supports both:
- Claude Code → (stdio) → Server
- Server → (WebSocket) → Figma Plugin

---

## Claude Code Configuration

### Step 1: Locate MCP Config File

Claude Code reads MCP server configuration from:

**macOS/Linux**:
```bash
~/.config/claude/mcp_config.json
```

**Windows**:
```
%APPDATA%\Claude\mcp_config.json
```

### Step 2: Create/Edit Configuration

Create or edit the file with this configuration:

```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [
        "-jar",
        "/ABSOLUTE/PATH/TO/FigmaMcp/server/build/libs/server-all.jar"
      ],
      "env": {
        "LOG_LEVEL": "info"
      }
    }
  }
}
```

**⚠️ IMPORTANT**: Replace `/ABSOLUTE/PATH/TO/FigmaMcp/` with your actual path!

**Example**:
```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/darkmat13r/Projects/FigmaMcp/server/build/libs/server-all.jar"
      ],
      "env": {
        "LOG_LEVEL": "info"
      }
    }
  }
}
```

### Step 3: Get Absolute Path

To find your absolute path:

```bash
cd /path/to/FigmaMcp/server
pwd
# Copy this output and use it in the config
# Example output: /Users/username/Projects/FigmaMcp/server
```

### Step 4: Restart Claude Code

After editing the config:
1. Quit Claude Code completely
2. Restart Claude Code
3. The MCP server will auto-start when Claude Code launches

---

## Figma Plugin Setup

The Figma plugin connects to the server via WebSocket.

### Step 1: Build the Plugin

```bash
cd figma-plugin
npm install
npm run build
```

### Step 2: Load Plugin in Figma

1. Open Figma Desktop App
2. Go to **Plugins → Development → Import plugin from manifest...**
3. Navigate to `figma-plugin/manifest.json`
4. Select it

### Step 3: Run the Plugin

1. Open any Figma file
2. Go to **Plugins → Development → [Your Plugin Name]**
3. The plugin UI will open
4. Plugin automatically connects to `ws://localhost:8080/figma`

### Step 4: Verify Connection

Check the plugin UI or server logs for:
```
Figma plugin connected, clientId=..., totalConnections=1
```

---

## Testing the Integration

### Test 1: Verify MCP Server Starts

In Claude Code, check if the Figma MCP server appears in available servers:

1. Open Claude Code
2. Check MCP servers status
3. You should see "figma" listed

### Test 2: List Available Tools

In Claude Code chat, ask:
```
What Figma tools do you have available?
```

Claude should list:
- `figma_create_rectangle` - Create rectangle nodes
- `figma_create_text` - Create text nodes
- `figma_get_selection` - Get selected nodes
- `figma_set_properties` - Modify node properties
- `figma_get_node_info` - Get node details

### Test 3: Create a Rectangle

**Prerequisites**: Figma plugin must be running!

In Claude Code chat:
```
Create a red rectangle in Figma that is 200x100 pixels
```

Expected flow:
1. Claude Code → MCP Server (tools/call with figma_create_rectangle)
2. MCP Server → Figma Plugin (WebSocket command)
3. Figma Plugin → Creates rectangle
4. Response back to Claude

### Test 4: Get Selection Info

1. Select something in Figma
2. In Claude Code:
   ```
   What is currently selected in Figma?
   ```

Claude should return information about the selected node(s).

---

## Troubleshooting

### Problem: Claude Code doesn't see the MCP server

**Symptoms**: "figma" doesn't appear in MCP servers list

**Solutions**:
1. Check config file path is correct:
   ```bash
   cat ~/.config/claude/mcp_config.json
   ```

2. Verify absolute path in config:
   ```bash
   # Path should match exactly
   ls /ABSOLUTE/PATH/TO/FigmaMcp/server/build/libs/server-all.jar
   ```

3. Check Java is installed:
   ```bash
   java -version
   ```

4. Restart Claude Code completely

5. Check Claude Code logs (macOS):
   ```bash
   tail -f ~/Library/Logs/Claude\ Code/*.log
   ```

### Problem: Server starts but tools don't work

**Symptoms**: Tools are listed but execution fails

**Solutions**:
1. **Ensure Figma plugin is running!**
   - Open Figma Desktop App
   - Run the plugin

2. Check WebSocket connection:
   - Server logs should show: `Figma plugin connected`

3. Verify server port:
   ```bash
   lsof -i :8080
   # Should show java process
   ```

### Problem: "No Figma plugin connected" error

**Symptoms**: Tool calls fail with connection error

**Cause**: Figma plugin not running

**Solution**:
1. Open Figma Desktop App
2. Open a design file
3. Run: **Plugins → Development → [Your Plugin]**
4. Wait for "Connected to server" message

### Problem: JSON-RPC errors

Common error codes:

| Code | Meaning | Solution |
|------|---------|----------|
| -32700 | Parse Error | Check JSON formatting in messages |
| -32600 | Invalid Request | Missing `id` or `method` in request |
| -32601 | Method Not Found | Check method name spelling |
| -32602 | Invalid Params | Verify parameter types and requirements |
| -32603 | Internal Error | Check server logs for details |

### Enable Debug Logging

For more detailed logs, update `mcp_config.json`:

```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [...],
      "env": {
        "LOG_LEVEL": "debug"
      }
    }
  }
}
```

### Manual Testing with stdio

You can test the MCP protocol manually:

```bash
# Run the server
cd server
java -jar build/libs/server-all.jar

# In another terminal, send JSON-RPC messages:
echo '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"},"capabilities":{}}}' | java -jar build/libs/server-all.jar

# You should get an initialize response
```

---

## Available MCP Tools

### 1. figma_create_rectangle

Creates a rectangle node in Figma.

**Parameters**:
- `width` (number, required): Width in pixels
- `height` (number, required): Height in pixels
- `x` (number, optional): X position, defaults to 0
- `y` (number, optional): Y position, defaults to 0
- `fillColor` (string, optional): Hex color (e.g., "#FF0000")

**Example**:
```json
{
  "width": 200,
  "height": 100,
  "fillColor": "#3B82F6"
}
```

### 2. figma_create_text

Creates a text node.

**Parameters**:
- `text` (string, required): Text content
- `fontSize` (number, optional): Font size, defaults to 16
- `fontFamily` (string, optional): Font family name
- `color` (string, optional): Text color hex

**Example**:
```json
{
  "text": "Hello, Figma!",
  "fontSize": 24,
  "color": "#000000"
}
```

### 3. figma_get_selection

Gets current selection information. No parameters required.

### 4. figma_set_properties

Sets properties on a node.

**Parameters**:
- `nodeId` (string, required): Node ID
- `properties` (object, required): Properties to set

**Example**:
```json
{
  "nodeId": "123:456",
  "properties": {
    "width": 300,
    "height": 200
  }
}
```

### 5. figma_get_node_info

Gets node information.

**Parameters**:
- `nodeId` (string, required): Node ID

---

## Next Steps

1. ✅ Build the server: `./gradlew assemble shadowJar -x test`
2. ✅ Configure Claude Code: Edit `~/.config/claude/mcp_config.json`
3. ✅ Start Figma plugin: Launch in Figma Desktop App
4. ✅ Test: Ask Claude Code to create something in Figma

---

## Additional Resources

- **MCP Specification**: https://spec.modelcontextprotocol.io/
- **Claude Code Docs**: https://docs.claude.com/claude-code/mcp
- **JSON-RPC 2.0 Spec**: https://www.jsonrpc.org/specification
- **Ktor Documentation**: https://ktor.io/

---

## File References

Key implementation files:

- `Application.kt:73-87` - Stdio transport startup
- `StdioTransport.kt:54` - MCP message handling
- `InitializeCommand.kt:40` - Protocol handshake
- `ListToolsCommand.kt:51` - Tool listing
- `CallToolCommand.kt:65` - Tool execution
- `FigmaToolExecutor.kt:56` - Tool implementations
- `MCPTypes.kt:1` - Protocol type definitions

---

**Happy coding with Claude Code + Figma!** 🎨🤖