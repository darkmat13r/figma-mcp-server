# McpServer

**Purpose**: MCP protocol server using official Kotlin SDK
**Location**: `server/src/main/kotlin/com/figma/mcp/transport/McpServer.kt`
**Dependencies**: io.modelcontextprotocol.kotlin.sdk

## Overview

Wraps the official Model Context Protocol Kotlin SDK to create a Figma-specific MCP server with 5 core tools.

## Tools Provided

1. **figma_create_rectangle** - Create rectangle nodes
   - Required: `width: number`, `height: number`
   - Optional: `x: number`, `y: number`, `fillColor: string`

2. **figma_create_text** - Create text nodes
   - Required: `text: string`
   - Optional: `fontSize: number`, `fontFamily: string`, `color: string`

3. **figma_get_selection** - Get current selection
   - No parameters

4. **figma_set_properties** - Set node properties
   - Required: `nodeId: string`, `properties: object`

5. **figma_get_node_info** - Get node information
   - Required: `nodeId: string`

## Public API

- `getServer(): Server` - Returns SDK Server instance for HTTP/SSE transport
- `start()` - Initialize server (SSE transport handled by Ktor routing)
- `stop()` - Cleanup server resources
