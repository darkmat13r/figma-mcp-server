package com.figma.mcp

import com.figma.mcp.infrastructure.appModule
import com.figma.mcp.transport.StdioTransport
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * MCP Stdio Server Entry Point
 *
 * ## Purpose
 * This is the main entry point for running the server in stdio mode
 * for Claude Code integration.
 *
 * ## How to Run
 * ```bash
 * # Build the JAR
 * ./gradlew build
 *
 * # Run in stdio mode
 * java -jar build/libs/figma-mcp-server.jar stdio
 * ```
 *
 * ## Claude Code Integration
 * Claude Code will launch this server as a subprocess and communicate via stdin/stdout.
 * The configuration is in ~/.config/claude/mcp_config.json
 *
 * ## Architecture
 * ```
 * Claude Code (MCP Client)
 *     ↓ stdio (JSON-RPC messages)
 * This Server (StdioTransport)
 *     ↓ Command Registry
 * MCP Command Handlers (initialize, tools/list, tools/call)
 *     ↓ Figma Tool Executor
 * Figma Connection Manager
 *     ↓ WebSocket
 * Figma Plugin
 * ```
 */
object MCPStdioServer : KoinComponent {

    private val stdioTransport: StdioTransport by inject()

    @JvmStatic
    fun main(args: Array<String>) {
        // Initialize Koin dependency injection
        startKoin {
            modules(appModule)
        }

        println("Starting Figma MCP Server in stdio mode...")
        println("Waiting for MCP client (Claude Code) to connect...")

        // Start the stdio transport
        // This will block and handle messages until stdin closes
        runBlocking {
            stdioTransport.start()

            // Keep the application running
            // The transport will handle shutdown when stdin closes
            while (true) {
                Thread.sleep(1000)
            }
        }
    }
}