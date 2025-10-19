package com.figma.mcp.infrastructure

import com.figma.mcp.commands.*
import com.figma.mcp.commands.mcp.*
import com.figma.mcp.core.CommandRegistry
import com.figma.mcp.core.ILogger
import com.figma.mcp.routes.McpRoutes
import com.figma.mcp.routes.WebSocketRoutes
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.services.FigmaToolExecutor
import com.figma.mcp.tools.FigmaToolRegistry
import com.figma.mcp.tools.impl.*
import com.figma.mcp.transport.McpServer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Dependency Injection Module - REFACTORED
 *
 * ## Purpose
 * Defines all dependencies and their lifecycles following Dependency Inversion Principle.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Each component has ONE responsibility
 * - Separated tool registry from tool executor
 * - Each tool is a separate class with single responsibility
 *
 * ### Open-Closed Principle (OCP)
 * - **Adding new tools is EASY**: Just add one line to register it
 * - **No modification needed**: Core components don't change
 *
 * ### Dependency Inversion Principle (DIP)
 * - All components depend on abstractions (ILogger, IFigmaTool, etc.)
 * - High-level modules don't depend on low-level modules
 *
 * ## Module Organization
 * - Core Services: Logger, JSON, etc.
 * - Figma Tool Registry: Centralized tool management
 * - Figma Tools: All individual tool implementations (Category 1: Node Creation)
 * - MCP Services: Connection manager, legacy tool executor
 * - Command Handlers: MCP protocol commands
 * - Transport: WebSocket and HTTP/SSE
 *
 * ## Adding New Tools (Example for Category 2)
 * 1. Create tool class: `class SetAutoLayoutTool : BaseFigmaTool(...)`
 * 2. Add to registry: `toolRegistry.register(SetAutoLayoutTool(get(), get()))`
 * 3. Done! Tool automatically appears in tools/list and is callable
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
    // Figma Connection Manager
    // ========================================

    // Figma Connection Manager (Singleton)
    // Manages WebSocket connections to Figma plugins
    single {
        FigmaConnectionManager(get())
    }

    // ========================================
    // Figma Tool Registry (NEW - Extensible Architecture)
    // ========================================

    // Figma Tool Registry (Singleton)
    // Central registry for all Figma tools using Strategy Pattern
    single {
        val toolRegistry = FigmaToolRegistry(get())

        // ✅ REFACTORED: Register all Category 1 tools (Node Creation)
        // Each tool is self-contained and follows SOLID principles
        // Adding new tools: Just add one line here - no other code changes needed!

        toolRegistry.registerAll(
            // Category 1: Node Creation Tools (12 tools)
            CreateFrameTool(get(), get()),
            CreateComponentTool(get(), get()),
            CreateInstanceTool(get(), get()),
            CreateRectangleTool(get(), get()),
            CreateEllipseTool(get(), get()),
            CreateTextTool(get(), get()),
            CreatePolygonTool(get(), get()),
            CreateStarTool(get(), get()),
            CreateLineTool(get(), get()),
            CreateGroupTool(get(), get()),
            CreateSectionTool(get(), get()),
            CreateBooleanOperationTool(get(), get())

            // Future categories can be added here:
            // Category 2: Layout Management Tools
            // SetAutoLayoutTool(get(), get()),
            // SetConstraintsTool(get(), get()),
            // ...

            // Category 3: Styling Tools
            // SetFillsTool(get(), get()),
            // SetStrokesTool(get(), get()),
            // ...
        )

        toolRegistry
    }

    // ========================================
    // Legacy Figma Tool Executor (Kept for backward compatibility)
    // ========================================

    // Figma Tool Executor (Singleton)
    // DEPRECATED: Will be removed once all tools migrated to registry
    // Currently kept for legacy tools like get_selection, set_properties, get_node_info
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
        // ✅ REFACTORED: Now use FigmaToolRegistry instead of FigmaToolExecutor
        registry.registerAll(
            InitializeCommand(get(), get()),
            ListToolsCommand(get(), get(), get()),  // ✅ Added toolRegistry parameter
            CallToolCommand(get(), get(), get())     // ✅ Added toolRegistry parameter
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
            json = get(),
            figmaConnectionManager = get()
        )
    }

    // MCP Routes (Singleton)
    // For HTTP/SSE communication with Claude Code
    single {
        McpRoutes(
            mcpServer = get(),
            logger = get(),
            json = get()
        )
    }

    // MCP Server (Singleton)
    // Uses official Kotlin SDK for MCP communication with Claude Code
    // ✅ REFACTORED: Now uses FigmaToolRegistry instead of FigmaToolExecutor
    single {
        McpServer(
            logger = get(),
            toolRegistry = get()
        )
    }
}

