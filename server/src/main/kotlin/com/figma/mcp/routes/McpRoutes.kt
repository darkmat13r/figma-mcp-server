package com.figma.mcp.routes

import com.figma.mcp.core.ILogger
import com.figma.mcp.session.SseSessionManager
import com.figma.mcp.session.SessionConstants
import com.figma.mcp.transport.McpServer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * MCP Routes for HTTP/SSE Transport with File-Specific Routing
 *
 * ## Purpose
 * Provides HTTP endpoints for Model Context Protocol communication with support
 * for file-specific routing via query parameters.
 *
 * ## Architecture
 * The MCP endpoint accepts a fileId query parameter:
 * - URL: http://localhost:1234/sse?fileId=abc123
 * - This associates the SSE session with a specific Figma file
 * - Commands from this session will be routed to the correct Figma plugin instance
 *
 * ## SOLID Principles
 * - Single Responsibility: Only handles MCP HTTP routing
 * - Dependency Inversion: Depends on abstractions (ILogger, McpServer, SseSessionManager)
 */
class McpRoutes(
    private val mcpServer: McpServer,
    private val sseSessionManager: SseSessionManager,
    private val logger: ILogger,
    private val json: Json
) {
    /**
     * Configure MCP routes for the application with file-specific routing
     */
    fun configure(routing: Routing) {
        with(routing) {
            // SSE endpoint with fileId support
            // URL: http://localhost:1234/sse?fileId=abc123
            route(SessionConstants.SSE_PATH_SSE) {
                get {
                    // First validate fileId before setting up MCP
                    val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]

                    if (fileId.isNullOrBlank()) {
                        logger.warn("SSE connection attempt without fileId")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to SessionConstants.ERROR_NO_FILE_ID,
                                "message" to "Please provide a fileId query parameter. Example: /sse?fileId=your-file-id"
                            )
                        )
                        return@get
                    }
                }

                mcp {
                    val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]!!
                    val sessionId = "${SessionConstants.SESSION_ID_PREFIX_SSE}${UUID.randomUUID()}"

                    // Register this SSE session with its file ID
                    sseSessionManager.registerSession(sessionId, fileId, call)

                    logger.info(
                        "MCP client connected via SSE",
                        "sessionId" to sessionId,
                        "fileId" to fileId
                    )

                    // Return the configured MCP server instance
                    mcpServer.getServer()
                }
            }

            // Legacy MCP endpoint (without fileId) - for backward compatibility
            route(SessionConstants.SSE_PATH_MCP) {
                get {
                    // First validate fileId before setting up MCP
                    val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]

                    if (fileId.isNullOrBlank()) {
                        logger.warn(
                            "MCP connection attempt without fileId - this is deprecated",
                            "remoteHost" to call.request.local.remoteHost
                        )
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to SessionConstants.ERROR_NO_FILE_ID,
                                "message" to "Please use /sse?fileId=your-file-id endpoint instead of /mcp",
                                "example" to "http://localhost:1234/sse?fileId=abc123"
                            )
                        )
                        return@get
                    }
                }

                mcp {
                    val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]!!
                    val sessionId = "${SessionConstants.SESSION_ID_PREFIX_SSE}${UUID.randomUUID()}"
                    sseSessionManager.registerSession(sessionId, fileId, call)

                    logger.info(
                        "MCP client connected via legacy /mcp endpoint",
                        "sessionId" to sessionId,
                        "fileId" to fileId
                    )

                    mcpServer.getServer()
                }
            }

            // Health check endpoint for MCP server
            get("/mcp/health") {
                call.respond(HttpStatusCode.OK, mapOf(
                    "status" to "healthy",
                    "server" to "figma-mcp-server",
                    "version" to "2.0.0",
                    "transport" to "sse",
                    "endpoints" to mapOf(
                        "sse" to "/sse?fileId=<your-file-id>",
                        "mcp" to "/mcp?fileId=<your-file-id> (deprecated)"
                    ),
                    "sessions" to sseSessionManager.getDiagnostics()
                ))
            }

            // Diagnostics endpoint
            get("/diagnostics/sessions") {
                call.respond(HttpStatusCode.OK, mapOf(
                    "sse" to sseSessionManager.getDiagnostics()
                ))
            }
        }
    }
}
