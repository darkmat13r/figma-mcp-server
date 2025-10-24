package com.figma.mcp.session

/**
 * Session Manager Interface
 *
 * ## Purpose
 * Defines the contract for managing sessions with file-specific routing.
 * Follows Interface Segregation Principle - focused on session management only.
 *
 * ## Design Pattern
 * Strategy Pattern - different implementations for SSE and WebSocket sessions
 *
 * @param T The type of session being managed (e.g., WebSocketSession, SSE Call)
 */
interface ISessionManager<T> {
    /**
     * Register a new session with its associated file ID
     *
     * @param sessionId Unique identifier for the session
     * @param fileId Figma file ID this session is associated with
     * @param session The actual session object (WebSocket/SSE)
     */
    fun registerSession(sessionId: String, fileId: String, session: T)

    /**
     * Unregister a session when it disconnects
     *
     * @param sessionId The session ID to remove
     */
    fun unregisterSession(sessionId: String)

    /**
     * Get a session by its ID
     *
     * @param sessionId The session ID to look up
     * @return The session object, or null if not found
     */
    fun getSession(sessionId: String): T?

    /**
     * Get a session by file ID
     *
     * @param fileId The Figma file ID
     * @return The session object, or null if no session for this file
     */
    fun getSessionByFileId(fileId: String): T?

    /**
     * Get the file ID associated with a session
     *
     * @param sessionId The session ID
     * @return The file ID, or null if session not found
     */
    fun getFileIdForSession(sessionId: String): String?

    /**
     * Get all active file IDs
     *
     * @return Set of all file IDs with active sessions
     */
    fun getActiveFileIds(): Set<String>

    /**
     * Get the number of active sessions
     *
     * @return Count of active sessions
     */
    fun getSessionCount(): Int

    /**
     * Check if a file has an active session
     *
     * @param fileId The Figma file ID
     * @return True if there's an active session for this file
     */
    fun hasSessionForFile(fileId: String): Boolean

    /**
     * Get diagnostic information about sessions
     *
     * @return Map with diagnostic data
     */
    fun getDiagnostics(): Map<String, Any>
}
