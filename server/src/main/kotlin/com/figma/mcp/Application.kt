package com.figma.mcp

import com.figma.mcp.plugins.*
import com.figma.mcp.transport.StdioTransport
import io.ktor.server.application.*
import kotlinx.coroutines.launch
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

    // Configure JSON serialization
    configureSerialization()

    // Configure CORS and HTTP settings
    configureHTTP()

    // Configure request/response logging
    configureMonitoring()

    // Configure routing (WebSocket + REST endpoints)
    configureRouting()

    // ========================================
    // Start Stdio Transport for Claude Code
    // ========================================
    // The stdio transport allows Claude Code to communicate with this server
    // via stdin/stdout using the MCP protocol. This runs in parallel with
    // the WebSocket transport for the Figma plugin.
    val stdioTransport: StdioTransport by inject()

    // Start stdio transport in a background coroutine
    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            log.info("Starting MCP stdio transport for Claude Code integration...")
            stdioTransport.start()
        }
    }

    // Graceful shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        log.info("Stopping MCP stdio transport...")
        stdioTransport.stop()
    }
}
