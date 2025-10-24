package com.figma.mcp.session

import com.figma.mcp.core.ILogger
import io.ktor.server.application.*
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE Session Manager
 *
 * ## Purpose
 * Manages Server-Sent Events (SSE) sessions and their association with Figma files.
 * Each SSE session represents a Claude Code instance connected via MCP.
 *
 * ## Thread Safety
 * Uses ConcurrentHashMap for thread-safe operations across multiple concurrent connections.
 *
 * ## Architecture
 * ```
 * Claude Code Session 1 → SSE Session A → fileId: abc123
 * Claude Code Session 2 → SSE Session B → fileId: def456
 * ```
 *
 * ## SOLID Principles
 * - Single Responsibility: Only manages SSE sessions
 * - Dependency Inversion: Depends on ILogger abstraction
 */
class SseSessionManager(
    private val logger: ILogger
) : ISessionManager<ApplicationCall> {

    // Map: Session ID → ApplicationCall (SSE connection)
    private val sessions = ConcurrentHashMap<String, ApplicationCall>()

    // Map: Session ID → File ID
    private val sessionToFileId = ConcurrentHashMap<String, String>()

    // Map: File ID → Session ID (for reverse lookup)
    private val fileIdToSession = ConcurrentHashMap<String, String>()

    override fun registerSession(sessionId: String, fileId: String, session: ApplicationCall) {
        // Remove any existing session for this file (only one Claude session per file)
        fileIdToSession[fileId]?.let { oldSessionId ->
            logger.warn(
                "Replacing existing SSE session for file",
                "fileId" to fileId,
                "oldSessionId" to oldSessionId,
                "newSessionId" to sessionId
            )
            unregisterSession(oldSessionId)
        }

        sessions[sessionId] = session
        sessionToFileId[sessionId] = fileId
        fileIdToSession[fileId] = sessionId

        logger.info(
            "SSE session registered",
            "sessionId" to sessionId,
            "fileId" to fileId,
            "totalSessions" to sessions.size
        )
    }

    override fun unregisterSession(sessionId: String) {
        val fileId = sessionToFileId.remove(sessionId)
        sessions.remove(sessionId)

        if (fileId != null) {
            fileIdToSession.remove(fileId)
        }

        logger.info(
            "SSE session unregistered",
            "sessionId" to sessionId,
            "fileId" to fileId,
            "totalSessions" to sessions.size
        )
    }

    override fun getSession(sessionId: String): ApplicationCall? {
        return sessions[sessionId]
    }

    override fun getSessionByFileId(fileId: String): ApplicationCall? {
        val sessionId = fileIdToSession[fileId] ?: return null
        return sessions[sessionId]
    }

    override fun getFileIdForSession(sessionId: String): String? {
        return sessionToFileId[sessionId]
    }

    override fun getActiveFileIds(): Set<String> {
        return fileIdToSession.keys.toSet()
    }

    override fun getSessionCount(): Int {
        return sessions.size
    }

    override fun hasSessionForFile(fileId: String): Boolean {
        return fileIdToSession.containsKey(fileId)
    }

    override fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "type" to "SSE",
            "totalSessions" to sessions.size,
            "activeFileIds" to fileIdToSession.keys.toList(),
            "sessionMappings" to sessionToFileId.mapValues { (_, fileId) -> fileId }
        )
    }

    /**
     * Get all session IDs
     *
     * @return Set of all active session IDs
     */
    fun getAllSessionIds(): Set<String> {
        return sessions.keys.toSet()
    }

    /**
     * Clear all sessions (for testing/cleanup)
     */
    fun clear() {
        val count = sessions.size
        sessions.clear()
        sessionToFileId.clear()
        fileIdToSession.clear()
        logger.info("All SSE sessions cleared", "clearedCount" to count)
    }
}
