package com.figma.mcp.plugins

import io.ktor.server.application.*
import io.ktor.server.sse.*

/**
 * Configures Server-Sent Events (SSE) support for MCP protocol
 */
fun Application.configureSSE() {
    install(SSE)
}
