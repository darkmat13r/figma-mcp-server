package com.figma.mcp.infrastructure

import com.figma.mcp.commands.*
import com.figma.mcp.commands.mcp.*
import com.figma.mcp.core.CommandRegistry
import com.figma.mcp.core.ILogger
import com.figma.mcp.routes.WebSocketRoutes
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.services.FigmaToolExecutor
import com.figma.mcp.transport.McpServer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Dependency Injection Module
 *
 * Defines all dependencies and their lifecycles
 * Follows Dependency Inversion Principle
 *
 * ## Module Organization
 * - Core Services: Logger, JSON, etc.
 * - MCP Services: Connection manager, tool executor
 * - Command Handlers: Both legacy and MCP protocol commands
 * - Transport: WebSocket and Stdio
 */
val appModule = module {
    // ========================================
    // Core Services
    // ========================================

    // Logger (Singleton)
    single<ILogger> {
        Slf4jLogger("FigmaMCPServer")
    }

    // JSON Configuration (Singleton)
    single {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    // ========================================
    // MCP Services
    // ========================================

    // Figma Connection Manager (Singleton)
    // Manages WebSocket connections to Figma plugins
    single {
        FigmaConnectionManager(get())
    }

    // Figma Tool Executor (Singleton)
    // Executes Figma-specific MCP tools
    single {
        FigmaToolExecutor(
            logger = get(),
            figmaConnectionManager = get()
        )
    }

    // ========================================
    // Command Registry
    // ========================================

    // Command Registry (Singleton)
    single {
        val registry = CommandRegistry(get())

        // Register legacy commands (for backward compatibility)
        registry.registerAll(
            PingCommand(get()),
            EchoCommand(get()),
            CreateNodeCommand(get()),
            GetInfoCommand(get(), registry)
        )

        // Register MCP protocol commands
        registry.registerAll(
            InitializeCommand(get(), get()),
            ListToolsCommand(get(), get()),
            CallToolCommand(get(), get(), get())
        )

        registry
    }

    // ========================================
    // Transport Layers
    // ========================================

    // WebSocket Routes (Singleton)
    // For WebSocket communication with Figma plugin
    single {
        WebSocketRoutes(
            commandRegistry = get(),
            logger = get(),
            json = get()
        )
    }

    // MCP Server (Singleton)
    // Uses official Kotlin SDK for MCP communication with Claude Code
    single {
        McpServer(
            logger = get(),
            figmaToolExecutor = get()
        )
    }
}
