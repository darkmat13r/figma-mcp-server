package com.figma.mcp.session

/**
 * Session Management Constants
 *
 * ## Purpose
 * Centralized constants for session management.
 * No magic strings or numbers - everything is named and documented.
 *
 * ## SOLID Principles
 * - Single Responsibility: Only contains constant definitions
 * - Open-Closed: New constants can be added without modifying existing code
 */
object SessionConstants {
    // Query Parameters
    const val QUERY_PARAM_FILE_ID = "fileId"
    const val QUERY_PARAM_SESSION_ID = "sessionId"

    // WebSocket Paths
    const val WS_PATH_FIGMA = "/figma"
    const val WS_PATH_ROOT = "/"

    // SSE Paths
    const val SSE_PATH_MCP = "/mcp"
    const val SSE_PATH_SSE = "/sse"

    // Error Messages
    const val ERROR_NO_FILE_ID = "No fileId query parameter provided"
    const val ERROR_NO_SESSION_FOR_FILE = "No active WebSocket session for file"
    const val ERROR_SESSION_NOT_FOUND = "Session not found"
    const val ERROR_FILE_NOT_FOUND = "File not found"
    const val ERROR_INVALID_FILE_ID = "Invalid fileId format"

    // Session ID Prefixes
    const val SESSION_ID_PREFIX_SSE = "sse_"
    const val SESSION_ID_PREFIX_WS = "ws_"

    // Timeouts (in milliseconds)
    const val SESSION_TIMEOUT_MS = 300000L // 5 minutes
    const val RECONNECT_GRACE_PERIOD_MS = 30000L // 30 seconds

    // Limits
    const val MAX_SESSIONS_PER_FILE = 10
    const val MAX_TOTAL_SESSIONS = 100
}
