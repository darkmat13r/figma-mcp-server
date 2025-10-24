package com.figma.mcp.session

/**
 * Route Resolver Interface
 *
 * ## Purpose
 * Determines which WebSocket session to route commands to based on
 * the SSE session that originated the request.
 *
 * ## Architecture
 * This is the key component that connects SSE sessions to WebSocket sessions.
 * It implements the routing logic for file-specific command execution.
 *
 * ## Design Principles
 * - Dependency Inversion: Depends on abstractions (ISessionManager) not concrete types
 * - Single Responsibility: Only handles routing logic
 */
interface IRouteResolver {
    /**
     * Resolve which WebSocket session to use for a given SSE session
     *
     * @param sseSessionId The SSE session ID making the request
     * @return RouteResolutionResult with either a WebSocket session or error
     */
    fun resolveRoute(sseSessionId: String): RouteResolutionResult

    /**
     * Resolve route by file ID directly
     *
     * @param fileId The Figma file ID
     * @return RouteResolutionResult with either a WebSocket session or error
     */
    fun resolveRouteByFileId(fileId: String): RouteResolutionResult

    /**
     * Get diagnostic information about routing
     *
     * @return Map with routing diagnostics
     */
    fun getDiagnostics(): Map<String, Any>
}

/**
 * Result of route resolution
 *
 * Sealed class ensures type-safe handling of success and failure cases
 */
sealed class RouteResolutionResult {
    /**
     * Route successfully resolved
     *
     * @param sessionId The WebSocket session ID to use
     * @param fileId The file ID associated with this route
     */
    data class Success(
        val sessionId: String,
        val fileId: String
    ) : RouteResolutionResult()

    /**
     * Route resolution failed
     *
     * @param error Error message describing why resolution failed
     * @param errorCode Error code for programmatic handling
     */
    data class Failure(
        val error: String,
        val errorCode: RouteErrorCode
    ) : RouteResolutionResult()
}

/**
 * Route Resolution Error Codes
 *
 * Enumeration of possible routing errors for type-safe error handling
 */
enum class RouteErrorCode {
    SSE_SESSION_NOT_FOUND,
    NO_FILE_ID_FOR_SESSION,
    NO_WEBSOCKET_FOR_FILE,
    WEBSOCKET_SESSION_NOT_FOUND,
    INVALID_FILE_ID,
    MULTIPLE_SESSIONS_FOR_FILE
}
