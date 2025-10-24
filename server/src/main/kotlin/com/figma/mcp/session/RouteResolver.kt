package com.figma.mcp.session

import com.figma.mcp.core.ILogger
import io.ktor.server.application.*

/**
 * Route Resolver Implementation
 *
 * ## Purpose
 * Implements the routing logic that connects SSE sessions (Claude Code)
 * to WebSocket sessions (Figma Plugin) based on file IDs.
 *
 * ## Architecture
 * ```
 * SSE Session (Claude) → fileId → WebSocket Session (Figma Plugin)
 *     sessionId: sse_123      abc123      sessionId: ws_456
 * ```
 *
 * ## Routing Logic
 * 1. Look up which file ID the SSE session is associated with
 * 2. Find the WebSocket session for that file ID
 * 3. Return the WebSocket session to use for command execution
 *
 * ## Error Handling
 * Returns detailed error information if routing fails at any step.
 *
 * ## SOLID Principles
 * - Single Responsibility: Only handles routing logic
 * - Dependency Inversion: Depends on ISessionManager abstractions
 * - Open-Closed: Can be extended with new routing strategies
 */
class RouteResolver(
    private val sseSessionManager: SseSessionManager,
    private val webSocketSessionManager: WebSocketSessionManager,
    private val logger: ILogger
) : IRouteResolver {

    override fun resolveRoute(sseSessionId: String): RouteResolutionResult {
        logger.debug("Resolving route", "sseSessionId" to sseSessionId)

        // Step 1: Get the file ID for this SSE session
        val fileId = sseSessionManager.getFileIdForSession(sseSessionId)
        if (fileId == null) {
            logger.warn(
                "SSE session not found or has no file ID",
                "sseSessionId" to sseSessionId
            )
            return RouteResolutionResult.Failure(
                error = "SSE session not found or has no associated file ID: $sseSessionId",
                errorCode = RouteErrorCode.NO_FILE_ID_FOR_SESSION
            )
        }

        logger.debug("Found file ID for SSE session", "fileId" to fileId, "sseSessionId" to sseSessionId)

        // Step 2: Resolve the route using the file ID
        return resolveRouteByFileId(fileId)
    }

    override fun resolveRouteByFileId(fileId: String): RouteResolutionResult {
        logger.debug("Resolving route by file ID", "fileId" to fileId)

        // Validate file ID format (basic validation)
        if (fileId.isBlank()) {
            logger.warn("Invalid file ID: blank or empty")
            return RouteResolutionResult.Failure(
                error = "Invalid file ID: must not be blank",
                errorCode = RouteErrorCode.INVALID_FILE_ID
            )
        }

        // Step 1: Get the WebSocket session ID for this file
        val wsSessionId = webSocketSessionManager.getSessionIdForFile(fileId)
        if (wsSessionId == null) {
            logger.warn(
                "No WebSocket session found for file",
                "fileId" to fileId,
                "availableFiles" to webSocketSessionManager.getActiveFileIds().joinToString(", ")
            )
            return RouteResolutionResult.Failure(
                error = "No active WebSocket connection for file: $fileId. " +
                        "Please ensure the Figma plugin is running in this file.",
                errorCode = RouteErrorCode.NO_WEBSOCKET_FOR_FILE
            )
        }

        logger.debug(
            "Found WebSocket session for file",
            "fileId" to fileId,
            "wsSessionId" to wsSessionId
        )

        // Step 2: Verify the WebSocket session still exists
        val wsSession = webSocketSessionManager.getSession(wsSessionId)
        if (wsSession == null) {
            logger.error(
                "WebSocket session ID found but session not in registry",
                null,
                "fileId" to fileId,
                "wsSessionId" to wsSessionId
            )
            return RouteResolutionResult.Failure(
                error = "WebSocket session not found: $wsSessionId",
                errorCode = RouteErrorCode.WEBSOCKET_SESSION_NOT_FOUND
            )
        }

        logger.info(
            "Route resolved successfully",
            "fileId" to fileId,
            "wsSessionId" to wsSessionId
        )

        return RouteResolutionResult.Success(
            sessionId = wsSessionId,
            fileId = fileId
        )
    }

    override fun getDiagnostics(): Map<String, Any> {
        val sseFileIds = sseSessionManager.getActiveFileIds()
        val wsFileIds = webSocketSessionManager.getActiveFileIds()

        val routingMatrix = mutableMapOf<String, Map<String, Any>>()
        sseFileIds.forEach { fileId ->
            val hasWebSocket = wsFileIds.contains(fileId)
            routingMatrix[fileId] = mapOf(
                "hasSSE" to true,
                "hasWebSocket" to hasWebSocket,
                "routable" to hasWebSocket
            )
        }

        wsFileIds.forEach { fileId ->
            if (!routingMatrix.containsKey(fileId)) {
                routingMatrix[fileId] = mapOf(
                    "hasSSE" to false,
                    "hasWebSocket" to true,
                    "routable" to false
                )
            }
        }

        return mapOf(
            "type" to "RouteResolver",
            "totalSSESessions" to sseSessionManager.getSessionCount(),
            "totalWebSocketSessions" to webSocketSessionManager.getSessionCount(),
            "sseFileIds" to sseFileIds.toList(),
            "wsFileIds" to wsFileIds.toList(),
            "routableFiles" to routingMatrix.filter { it.value["routable"] as Boolean }.keys.toList(),
            "routingMatrix" to routingMatrix
        )
    }

    /**
     * Get a summary of current routing state
     *
     * @return Human-readable routing summary
     */
    fun getRoutingSummary(): String {
        val sseCount = sseSessionManager.getSessionCount()
        val wsCount = webSocketSessionManager.getSessionCount()
        val sseFiles = sseSessionManager.getActiveFileIds()
        val wsFiles = webSocketSessionManager.getActiveFileIds()
        val routableFiles = sseFiles.intersect(wsFiles)

        return buildString {
            appendLine("Routing Summary:")
            appendLine("  SSE Sessions: $sseCount")
            appendLine("  WebSocket Sessions: $wsCount")
            appendLine("  Routable Files: ${routableFiles.size}")
            if (routableFiles.isNotEmpty()) {
                appendLine("  Files: ${routableFiles.joinToString(", ")}")
            }
            if (sseFiles.subtract(wsFiles).isNotEmpty()) {
                appendLine("  SSE-only files (no plugin): ${sseFiles.subtract(wsFiles).joinToString(", ")}")
            }
            if (wsFiles.subtract(sseFiles).isNotEmpty()) {
                appendLine("  WebSocket-only files (no Claude): ${wsFiles.subtract(sseFiles).joinToString(", ")}")
            }
        }
    }
}
