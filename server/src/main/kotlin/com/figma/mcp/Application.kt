package com.figma.mcp

import com.figma.mcp.plugins.*
import com.figma.mcp.transport.McpServer
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.config.tryGetString
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

/**
 * Main application entry point
 *
 * ## Modes of Operation
 * This server supports two modes:
 *
 * 1. **WebSocket Mode** (default):
 *    - Starts HTTP server on port 8080
 *    - Figma plugin connects via WebSocket
 *    - For direct plugin communication
 *
 * 2. **Stdio Mode** (for Claude Code):
 *    - Also starts HTTP server for Figma plugin
 *    - Additionally starts stdio transport for Claude Code
 *    - Both transports run concurrently
 *
 * The stdio transport starts automatically and runs in the background,
 * allowing Claude Code to connect via stdin/stdout while the Figma
 * plugin connects via WebSocket.
 */
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)


}

/**
 * Application module configuration
 *
 * All plugins are configured in separate files in the plugins package
 * This follows the Single Responsibility Principle - each plugin file
 * has one specific concern
 *
 * ## Dual Transport Layer
 * This application supports both:
 * - WebSocket (for Figma plugin connection)
 * - Stdio (for Claude Code MCP connection)
 *
 * Both run concurrently to enable the full communication chain:
 * Claude Code ←→ (stdio) ←→ MCP Server ←→ (WebSocket) ←→ Figma Plugin
 */
fun Application.module() {
    // Configure dependency injection (Koin)
    configureDependencyInjection()

    // Configure WebSocket support
    configureWebSockets()

    // Configure Server-Sent Events (SSE) for MCP
    configureSSE()

    // Configure JSON serialization
    configureSerialization()

    // Configure CORS and HTTP settings
    configureHTTP()

    // Configure request/response logging
    configureMonitoring()

    // Configure routing (WebSocket + MCP + REST endpoints)
    configureRouting()


    launch {
        val config = this@module.environment.config.config("mcp")
        val port = config.tryGetString("port")?.toIntOrNull() ?: 1234
        val host = config.tryGetString("host") ?: "0.0.0.0"
        embeddedServer(CIO, host = host, port = port) {
            mcp {
                val mcpServer by inject<McpServer>()
                return@mcp mcpServer.getServer()
            }
        }.startSuspend(wait = true)
    }
}
