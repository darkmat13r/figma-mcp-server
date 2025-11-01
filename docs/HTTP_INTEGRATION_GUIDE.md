# HTTP/SSE Integration Guide for Figma MCP Server

## Overview

Your Figma MCP Server now supports **HTTP/SSE (Server-Sent Events)** transport, allowing Claude Code to connect via HTTP instead of stdio. This means you can run the server as a standard HTTP service and Claude Code will connect to it via the URL.

---

## Architecture

```
Claude Code (MCP Client)
    ↓ HTTP/SSE (http://localhost:8080/mcp/sse)
MCP Server (Official Kotlin SDK v0.7.2)
    ↓ Delegates to
FigmaToolExecutor
    ↓ WebSocket (ws://localhost:8080/figma)
Figma Plugin
    ↓ Figma Plugin API
Figma Document
```

---

## Quick Start

### 1. Start the Ktor Server

```bash
cd /Volumes/ExtStorage/Projects/FigmaMcp/server

# Start the server
java -jar build/libs/server-all.jar

# Server will start on http://localhost:8080
```

### 2. Verify Server is Running

```bash
# Check health
curl http://localhost:8080/mcp/health

# Expected response:
# {
#   "status": "healthy",
#   "server": "figma-mcp-server",
#   "version": "1.0.0",
#   "transport": "sse",
#   "sse_endpoint": "/mcp/sse",
#   "message_endpoint": "/mcp/message"
# }
```

### 3. Configure Claude Code

Copy the HTTP MCP config to Claude Code:

```bash
# Create Claude config directory
mkdir -p ~/.config/claude

# Copy the HTTP MCP configuration
cp server/claude-code-http-mcp-config.json ~/.config/claude/mcp_config.json
```

Configuration file content:
```json
{
  "$schema": "https://modelcontextprotocol.io/schema/mcp-config.json",
  "mcpServers": {
    "figma": {
      "url": "http://localhost:8080/mcp/sse",
      "transport": "sse"
    }
  }
}
```

### 4. Start Figma Plugin

```bash
cd /Volumes/ExtStorage/Projects/FigmaMcp/figma-plugin

# Build the plugin
npm run build

# Then in Figma Desktop:
# Plugins → Development → Import plugin from manifest
# Select: figma-plugin/manifest.json
# Run the plugin
```

### 5. Restart Claude Code

- Quit Claude Code completely
- Restart Claude Code
- Claude Code will automatically connect to your MCP server via HTTP

---

## Available Endpoints

### MCP Endpoints

#### 1. SSE Endpoint (Primary)
```
GET http://localhost:8080/mcp/sse
```
- **Purpose**: Server-Sent Events endpoint for bidirectional MCP communication
- **Usage**: Claude Code connects here for MCP protocol
- **Transport**: SSE (Server-Sent Events)

#### 2. Message Endpoint
```
POST http://localhost:8080/mcp/message
```
- **Purpose**: HTTP POST endpoint for sending messages
- **Usage**: For simple request/response patterns
- **Note**: Use SSE endpoint for full MCP protocol support

#### 3. Health Check
```
GET http://localhost:8080/mcp/health
```
- **Purpose**: Health check for MCP server
- **Response**: Server status and configuration

### Other Endpoints

#### 4. Figma WebSocket
```
ws://localhost:8080/figma
```
- **Purpose**: WebSocket connection for Figma plugin
- **Usage**: Figma plugin connects here

#### 5. General Health
```
GET http://localhost:8080/health
```
- **Purpose**: General server health check

---

## MCP Tools Available

Once Claude Code connects, all 5 Figma tools are available:

1. **figma_create_rectangle**
   - Create rectangles with dimensions, position, and fill color
   - Parameters: width, height, x, y, fillColor

2. **figma_create_text**
   - Create text nodes with content and styling
   - Parameters: text, fontSize, fontFamily, color

3. **figma_get_selection**
   - Get currently selected nodes in Figma
   - Parameters: none

4. **figma_set_properties**
   - Modify node properties by ID
   - Parameters: nodeId, properties

5. **figma_get_node_info**
   - Get detailed node information
   - Parameters: nodeId

---

## Testing the Integration

### Test 1: Server Health
```bash
curl http://localhost:8080/mcp/health
```

### Test 2: General Health
```bash
curl http://localhost:8080/health
```

### Test 3: In Claude Code

Ask Claude:
```
"What Figma tools do you have available?"
```

Expected response: Claude lists all 5 tools and can use them.

### Test 4: Create a Rectangle

Ask Claude:
```
"Create a red rectangle that is 200x100 pixels in Figma"
```

Claude should:
1. Use the `figma_create_rectangle` tool
2. Send the request via HTTP/SSE to your server
3. Server forwards to Figma plugin via WebSocket
4. Figma plugin creates the rectangle
5. Response flows back to Claude

---

## Server Logs

View server logs to monitor activity:

```bash
# If running in background
tail -f /tmp/figma-mcp-server.log

# Look for these log entries:
# - "MCP server initialized and ready for SSE transport"
# - "Server will accept connections via HTTP/SSE endpoints"
# - "Client connected to MCP SSE endpoint"
# - "Figma plugin connected"
```

---

## Troubleshooting

### Problem: Claude doesn't see the server

**Solution**:
1. Check config file location:
   ```bash
   cat ~/.config/claude/mcp_config.json
   ```
2. Verify server is running:
   ```bash
   curl http://localhost:8080/mcp/health
   ```
3. Check URL in config is correct: `http://localhost:8080/mcp/sse`
4. Restart Claude Code completely

### Problem: Tools don't execute

**Solution**:
1. Ensure Figma plugin is running in Figma Desktop
2. Check WebSocket connection in server logs
3. Verify server logs show "Figma plugin connected"

### Problem: Server won't start

**Solution**:
1. Check if port 8080 is already in use:
   ```bash
   lsof -i :8080
   ```
2. Kill existing processes:
   ```bash
   pkill -f server-all.jar
   ```
3. Start server again

### Problem: Connection refused

**Solution**:
1. Verify server is actually running:
   ```bash
   ps aux | grep server-all.jar
   ```
2. Check server logs for startup errors
3. Try accessing from browser: `http://localhost:8080/health`

---

## Advantages of HTTP/SSE Transport

✅ **Standard HTTP**: No special stdio setup needed
✅ **Easy Debugging**: Can test with curl, browser, Postman
✅ **Monitoring**: Standard HTTP health checks
✅ **Scalability**: Can run behind reverse proxy, load balancer
✅ **Cloud Ready**: Can deploy to cloud services
✅ **Port Forwarding**: Easy to expose remotely if needed

---

## Port Configuration

Default ports:
- **HTTP Server**: 8080
- **MCP SSE Endpoint**: http://localhost:8080/mcp/sse
- **Figma WebSocket**: ws://localhost:8080/figma

To change the port, edit `server/src/main/resources/application.conf`:
```hocon
ktor {
    deployment {
        port = 8080  # Change this
    }
}
```

---

## Security Notes

⚠️ **localhost only**: Server binds to 0.0.0.0 but should only be accessed locally
⚠️ **No authentication**: Current implementation has no auth (fine for local development)
⚠️ **CORS enabled**: Cross-origin requests are allowed for development

For production deployment, consider adding:
- Authentication/Authorization
- HTTPS/TLS
- Rate limiting
- API keys

---

## Implementation Details

### File Structure

```
server/src/main/kotlin/com/figma/mcp/
├── transport/
│   └── McpServer.kt          # MCP server using official SDK
├── routes/
│   ├── McpRoutes.kt          # HTTP/SSE endpoints for MCP
│   └── WebSocketRoutes.kt    # WebSocket for Figma plugin
├── plugins/
│   ├── SSE.kt                # SSE plugin configuration
│   └── Routing.kt            # Routes configuration
└── Application.kt            # Main application
```

### Key Components

1. **McpServer.kt**: Uses official Kotlin SDK, exposes `getServer()` for HTTP routing
2. **McpRoutes.kt**: Configures SSE endpoint and connects to MCP server
3. **SSE.kt**: Installs SSE plugin for Ktor
4. **Application.kt**: Initializes all plugins including SSE

---

## Next Steps

1. ✅ Server running on http://localhost:8080
2. ✅ MCP endpoints available at /mcp/sse
3. ✅ All 5 Figma tools registered
4. ⏳ Copy config to ~/.config/claude/mcp_config.json
5. ⏳ Restart Claude Code
6. ⏳ Test integration

---

**Date**: October 19, 2025
**Version**: 1.0.0
**Transport**: HTTP/SSE
**Status**: READY FOR INTEGRATION
