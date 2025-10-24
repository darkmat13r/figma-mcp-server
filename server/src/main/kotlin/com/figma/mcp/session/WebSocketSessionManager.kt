package com.figma.mcp.session

import com.figma.mcp.core.ILogger
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket Connection Wrapper
 *
 * ## Purpose
 * Wraps a WebSocket session with its sender actor for thread-safe communication.
 *
 * ## Architecture
 * Each Figma plugin connection has:
 * - A WebSocket session for receiving messages
 * - A sender actor channel for thread-safe sending
 */
data class WebSocketConnectionWrapper(
    val session: DefaultWebSocketServerSession,
    val senderActor: SendChannel<WebSocketMessage>,
    val fileId: String
)

/**
 * Actor messages for thread-safe WebSocket sending
 */
sealed class WebSocketMessage {
    data class Send(val frame: Frame.Text, val result: kotlinx.coroutines.CompletableDeferred<Unit>) : WebSocketMessage()
    object Close : WebSocketMessage()
}

/**
 * WebSocket Session Manager
 *
 * ## Purpose
 * Manages WebSocket sessions from Figma plugins and their association with files.
 * Each WebSocket session represents a Figma plugin instance running in a specific file.
 *
 * ## Thread Safety
 * Uses ConcurrentHashMap for thread-safe operations and actor pattern for sending.
 *
 * ## Architecture
 * ```
 * Figma File abc123 → WebSocket Session A → Plugin Instance 1
 * Figma File def456 → WebSocket Session B → Plugin Instance 2
 * ```
 *
 * ## SOLID Principles
 * - Single Responsibility: Only manages WebSocket sessions
 * - Dependency Inversion: Depends on ILogger abstraction
 */
class WebSocketSessionManager(
    private val logger: ILogger
) : ISessionManager<WebSocketConnectionWrapper> {

    // Map: Session ID → WebSocket Connection Wrapper
    private val sessions = ConcurrentHashMap<String, WebSocketConnectionWrapper>()

    // Map: Session ID → File ID
    private val sessionToFileId = ConcurrentHashMap<String, String>()

    // Map: File ID → Session ID (for reverse lookup)
    private val fileIdToSession = ConcurrentHashMap<String, String>()

    override fun registerSession(
        sessionId: String,
        fileId: String,
        session: WebSocketConnectionWrapper
    ) {
        // Remove any existing session for this file (only one plugin instance per file)
        fileIdToSession[fileId]?.let { oldSessionId ->
            logger.warn(
                "Replacing existing WebSocket session for file",
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
            "WebSocket session registered",
            "sessionId" to sessionId,
            "fileId" to fileId,
            "totalSessions" to sessions.size
        )
    }

    override fun unregisterSession(sessionId: String) {
        val wrapper = sessions.remove(sessionId)
        val fileId = sessionToFileId.remove(sessionId)

        if (fileId != null) {
            fileIdToSession.remove(fileId)
        }

        // Close the sender actor
        wrapper?.let {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                try {
                    it.senderActor.send(WebSocketMessage.Close)
                    it.senderActor.close()
                } catch (e: Exception) {
                    logger.warn("Error closing sender actor", "sessionId" to sessionId, "error" to e.message)
                }
            }
        }

        logger.info(
            "WebSocket session unregistered",
            "sessionId" to sessionId,
            "fileId" to fileId,
            "totalSessions" to sessions.size
        )
    }

    override fun getSession(sessionId: String): WebSocketConnectionWrapper? {
        return sessions[sessionId]
    }

    override fun getSessionByFileId(fileId: String): WebSocketConnectionWrapper? {
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
            "type" to "WebSocket",
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
     * Get session ID for a file
     *
     * @param fileId The Figma file ID
     * @return The session ID, or null if no session exists
     */
    fun getSessionIdForFile(fileId: String): String? {
        return fileIdToSession[fileId]
    }

    /**
     * Clear all sessions (for testing/cleanup)
     */
    fun clear() {
        val count = sessions.size
        sessions.values.forEach { wrapper ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                try {
                    wrapper.senderActor.send(WebSocketMessage.Close)
                    wrapper.senderActor.close()
                } catch (e: Exception) {
                    logger.warn("Error closing sender actor during clear", "error" to e.message)
                }
            }
        }
        sessions.clear()
        sessionToFileId.clear()
        fileIdToSession.clear()
        logger.info("All WebSocket sessions cleared", "clearedCount" to count)
    }
}
