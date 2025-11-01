package com.figma.mcp.plugins

import com.figma.mcp.routes.McpRoutes
import com.figma.mcp.routes.WebSocketRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import org.koin.ktor.ext.inject

/**
 * Configures application routing including WebSocket endpoints, MCP endpoints, and REST endpoints
 */
fun Application.configureRouting() {
    // Inject routes from Koin
    val webSocketRoutes: WebSocketRoutes by inject()
    val mcpRoutes: McpRoutes by inject()

    routing {
        // MCP HTTP/SSE endpoints (http://localhost:8080/mcp/*)
        with(mcpRoutes) {
            configure(this@routing)
        }


        // WebSocket endpoint (ws://localhost:8080/)
        with(webSocketRoutes) {
            configureWebSocketRoutes()
        }

        // REST endpoints
        route("/api") {
            // Health check endpoint for monitoring
            get("/health") {
                call.respond(mapOf(
                    "status" to "healthy",
                    "service" to "Figma MCP Server",
                    "timestamp" to System.currentTimeMillis()
                ))
            }

            // Version endpoint
            get("/version") {
                call.respond(mapOf(
                    "version" to "1.0.0",
                    "framework" to "Ktor",
                    "language" to "Kotlin"
                ))
            }
        }

    }
}
