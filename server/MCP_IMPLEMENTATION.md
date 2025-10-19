# MCP Protocol Implementation for Figma Plugin

This document explains the Model Context Protocol (MCP) implementation for the Figma MCP Server, which enables Claude Code to interact with Figma.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Protocol Implementation](#protocol-implementation)
3. [Setup Instructions](#setup-instructions)
4. [Available Tools](#available-tools)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### System Components

```
┌─────────────────┐
│   Claude Code   │  (MCP Client)
│  AI Assistant   │
└────────┬────────┘
         │ stdio (JSON-RPC 2.0)
         │
┌────────▼────────┐
│  MCP Server     │  (This Ktor application)
│  (Kotlin/Ktor)  │
└────────┬────────┘
         │ WebSocket
         │
┌────────▼────────┐
│  Figma Plugin   │  (TypeScript/JavaScript)
│  (UI Extension) │
└────────┬────────┘
         │ Plugin API
         │
┌────────▼────────┐
│ Figma Document  │
└─────────────────┘
```

### Communication Flow

1. **Claude Code → MCP Server**: stdio transport using JSON-RPC 2.0
2. **MCP Server → Figma Plugin**: WebSocket connection
3. **Figma Plugin → Figma API**: Native plugin API calls

---

## Protocol Implementation

### Step 1: Initialize Handshake

The first message in any MCP session is the `initialize` request.

**Request from Claude Code:**
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

**Response from Server:**
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
      "tools": {
        "listChanged": true
      },
      "resources": {
        "listChanged": true,
        "subscribe": false
      },
      "prompts": {
        "listChanged": true
      }
    },
    "instructions": "Figma MCP Server..."
  }
}
```

**Implementation:**
- Handler: `InitializeCommand.kt` (server/src/main/kotlin/com/figma/mcp/commands/mcp/InitializeCommand.kt:1)
- Protocol Types: `MCPTypes.kt` (server/src/main/kotlin/com/figma/mcp/protocol/MCPTypes.kt:1)

---

### Step 2: List Available Tools

Claude asks what tools are available using `tools/list`.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {}
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "result": {
    "tools": [
      {
        "name": "figma_create_rectangle",
        "description": "Creates a rectangle node in the current Figma selection",
        "inputSchema": {
          "type": "object",
          "properties": {
            "width": {
              "type": "number",
              "description": "Width of the rectangle in pixels"
            },
            "height": {
              "type": "number",
              "description": "Height of the rectangle in pixels"
            },
            "fillColor": {
              "type": "string",
              "description": "Fill color as hex code"
            }
          },
          "required": ["width", "height"]
        }
      }
    ]
  }
}
```

**Implementation:**
- Handler: `ListToolsCommand.kt` (server/src/main/kotlin/com/figma/mcp/commands/mcp/ListToolsCommand.kt:1)

---

### Step 3: Execute a Tool

When Claude wants to use a tool, it calls `tools/call`.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": "3",
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

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Successfully created rectangle (200x100) at position (0, 0) with fill color #FF0000"
      }
    ],
    "isError": false
  }
}
```

**Implementation:**
- Handler: `CallToolCommand.kt` (server/src/main/kotlin/com/figma/mcp/commands/mcp/CallToolCommand.kt:1)
- Tool Executor: `FigmaToolExecutor.kt` (server/src/main/kotlin/com/figma/mcp/services/FigmaToolExecutor.kt:1)

---

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Gradle 8.x
- Claude Code installed
- Figma desktop app with plugin installed

### Step 1: Build the Server

```bash
cd server
./gradlew clean build
```

This creates a JAR file at `build/libs/figma-mcp-server-all.jar`

### Step 2: Configure Claude Code

1. Create or edit `~/.config/claude/mcp_config.json`:

```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/server/build/libs/figma-mcp-server-all.jar"
      ],
      "env": {
        "LOG_LEVEL": "info"
      }
    }
  }
}
```

**Important:** Replace `/absolute/path/to/server/` with the actual path!

### Step 3: Start the Figma Plugin

1. Open Figma desktop app
2. Open a design file
3. Go to Plugins → Development → Your Figma MCP Plugin
4. The plugin will connect to the server via WebSocket

### Step 4: Test with Claude Code

1. Open Claude Code
2. The Figma MCP server should automatically start
3. Try asking Claude:
   - "Create a red rectangle in Figma"
   - "What is currently selected in Figma?"
   - "Create a text node that says 'Hello World'"

---

## Available Tools

### 1. `figma_create_rectangle`

Creates a rectangle node in Figma.

**Parameters:**
- `width` (number, required): Width in pixels
- `height` (number, required): Height in pixels
- `x` (number, optional): X position, defaults to 0
- `y` (number, optional): Y position, defaults to 0
- `fillColor` (string, optional): Hex color code (e.g., "#FF0000")

**Example:**
```json
{
  "width": 200,
  "height": 100,
  "fillColor": "#3B82F6"
}
```

### 2. `figma_create_text`

Creates a text node in Figma.

**Parameters:**
- `text` (string, required): Text content
- `fontSize` (number, optional): Font size in pixels, defaults to 16
- `fontFamily` (string, optional): Font family name
- `color` (string, optional): Text color as hex code

**Example:**
```json
{
  "text": "Hello, Figma!",
  "fontSize": 24,
  "color": "#000000"
}
```

### 3. `figma_get_selection`

Gets information about currently selected nodes.

**Parameters:** None

**Returns:** JSON with selection details

### 4. `figma_set_properties`

Sets properties on a Figma node.

**Parameters:**
- `nodeId` (string, required): Node ID
- `properties` (object, required): Properties to set

**Example:**
```json
{
  "nodeId": "123:456",
  "properties": {
    "width": 300,
    "height": 200
  }
}
```

### 5. `figma_get_node_info`

Gets detailed information about a node.

**Parameters:**
- `nodeId` (string, required): Node ID

---

## Testing

### Manual Testing with stdio

You can test the server directly using stdio:

```bash
# Run the server
java -jar build/libs/figma-mcp-server-all.jar

# Type JSON-RPC messages (each on a single line):
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"},"capabilities":{}}}

{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}
```

### Unit Tests

Run the test suite:

```bash
./gradlew test
```

---

## Troubleshooting

### Claude Code doesn't see the server

**Check:**
1. Is the path in `mcp_config.json` absolute and correct?
2. Does the JAR file exist at that path?
3. Check Claude Code logs: `~/Library/Logs/Claude Code/`

### Server starts but tools don't work

**Check:**
1. Is the Figma plugin running?
2. Is the WebSocket connection established?
3. Check server logs for connection status

### JSON-RPC errors

**Common issues:**
- **Parse Error (-32700)**: Malformed JSON message
- **Invalid Request (-32600)**: Missing `id` or `method`
- **Method Not Found (-32601)**: Unknown method name
- **Invalid Params (-32602)**: Invalid or missing parameters

### Enable Debug Logging

Set `LOG_LEVEL=debug` in the MCP config:

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

---

## File Structure

```
server/
├── src/main/kotlin/com/figma/mcp/
│   ├── Application.kt                  # Main entry point (dual-mode: HTTP + stdio)
│   ├── protocol/
│   │   └── MCPTypes.kt                 # MCP protocol type definitions
│   ├── commands/
│   │   └── mcp/
│   │       ├── InitializeCommand.kt    # Handle initialize
│   │       ├── ListToolsCommand.kt     # Handle tools/list
│   │       └── CallToolCommand.kt      # Handle tools/call
│   ├── services/
│   │   ├── FigmaToolExecutor.kt        # Execute Figma operations
│   │   └── FigmaConnectionManager.kt   # Manage plugin connections
│   ├── transport/
│   │   └── StdioTransport.kt           # stdio communication
│   └── infrastructure/
│       └── Di Module.kt                # Dependency injection
├── build.gradle.kts
└── MCP_IMPLEMENTATION.md               # This file
```

---

## References

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [Claude Code MCP Documentation](https://docs.claude.com/claude-code/mcp)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)

---

## Next Steps

1. **Build the server**: `./gradlew build`
2. **Configure Claude Code**: Edit `~/.config/claude/mcp_config.json`
3. **Start Figma plugin**: Launch plugin in Figma app
4. **Test**: Ask Claude Code to create something in Figma

Enjoy using Claude Code with Figma! 🎨🤖