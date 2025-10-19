package com.figma.mcp.plugins

import com.figma.mcp.routes.WebSocketRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Configures application routing including WebSocket endpoints and REST endpoints
 */
fun Application.configureRouting() {
    // Inject WebSocket routes from Koin
    val webSocketRoutes: WebSocketRoutes by inject()

    routing {
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

        // Legacy health check (kept for backwards compatibility)
        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }
    }
}
