package com.figma.mcp.routes

import com.figma.mcp.core.ILogger
import com.figma.mcp.transport.McpServer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.serialization.json.Json

/**
 * MCP Routes for HTTP/SSE Transport
 *
 * Provides HTTP endpoints for Model Context Protocol communication using the official SDK's mcp routing.
 * The SDK automatically handles both GET (SSE) and POST requests at the /mcp endpoint.
 *
 * This allows Claude Code to connect via HTTP instead of stdio.
 */
class McpRoutes(
    private val mcpServer: McpServer,
    private val logger: ILogger,
    private val json: Json
) {
    /**
     * Configure MCP routes for the application using the official SDK's mcp routing
     */
    fun configure(routing: Routing) {
        with(routing) {
            // MCP endpoint using official SDK routing
            // This automatically handles both GET (SSE) and POST at /mcp
            route("/mcp") {
                mcp {
                    logger.info("MCP client connected via SSE")
                    // Return the configured MCP server instance
                    mcpServer.getServer()
                }
            }

            // Health check endpoint for MCP server
            get("/mcp/health") {
                call.respond(HttpStatusCode.OK, mapOf(
                    "status" to "healthy",
                    "server" to "figma-mcp-server",
                    "version" to "1.0.0",
                    "transport" to "sse",
                    "endpoint" to "/mcp"
                ))
            }
        }
    }
}
