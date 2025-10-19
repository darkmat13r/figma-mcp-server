package com.figma.mcp.services

import com.figma.mcp.core.ILogger
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Figma Connection Manager
 *
 * ## Purpose
 * Manages WebSocket connections to Figma plugins and handles
 * request/response matching for tool execution.
 *
 * ## Architecture
 * This service maintains a pool of active connections to Figma plugins.
 * When a tool needs to execute:
 * 1. We generate a unique request ID
 * 2. We send the command via WebSocket to the plugin
 * 3. We wait for a response with matching ID
 * 4. We return the result to the tool executor
 *
 * ## Connection Lifecycle
 * - Figma plugin connects via WebSocket to this server
 * - Connection is registered here
 * - Commands can be sent to the plugin
 * - When plugin disconnects, we clean up
 */
class FigmaConnectionManager(
    private val logger: ILogger
) {
    // Active Figma plugin connections
    private val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, Channel<JsonElement?>>()

    // Request ID generator
    private val requestIdCounter = AtomicLong(0)

    /**
     * Register a new Figma plugin connection
     */
    fun registerConnection(clientId: String, session: DefaultWebSocketServerSession) {
        connections[clientId] = session
        logger.info("Figma plugin connected", "clientId" to clientId, "totalConnections" to connections.size)
    }

    /**
     * Unregister a Figma plugin connection
     */
    fun unregisterConnection(clientId: String) {
        connections.remove(clientId)
        logger.info("Figma plugin disconnected", "clientId" to clientId, "totalConnections" to connections.size)
    }

    /**
     * Handle a response from the Figma plugin
     */
    suspend fun handleResponse(requestId: String, result: JsonElement?) {
        pendingRequests[requestId]?.send(result)
    }

    /**
     * Send a command to the Figma plugin and wait for response
     *
     * @param method The method to call (e.g., "createNode", "getInfo")
     * @param params The parameters for the method
     * @param timeout Timeout in milliseconds (default 5000ms)
     * @return The result from the plugin, or null if timeout/error
     */
    suspend fun sendCommand(
        method: String,
        params: JsonObject,
        timeout: Long = 5000
    ): JsonElement? {
        // Get the first available connection (in production, you might want smarter routing)
        val connection = connections.values.firstOrNull()
            ?: throw IllegalStateException("No Figma plugin connected")

        val requestId = generateRequestId()
        val responseChannel = Channel<JsonElement?>(1)
        pendingRequests[requestId] = responseChannel

        try {
            // Send the command
            val command = buildJsonObject {
                put("id", requestId)
                put("method", method)
                put("params", params)
            }

            connection.send(Frame.Text(command.toString()))

            logger.debug(
                "Sent command to Figma plugin",
                "requestId" to requestId,
                "method" to method
            )

            // Wait for response with timeout
            return withTimeout(timeout) {
                responseChannel.receive()
            }
        } catch (e: Exception) {
            logger.error("Failed to send command to Figma plugin", e, "method" to method)
            throw e
        } finally {
            pendingRequests.remove(requestId)
            responseChannel.close()
        }
    }

    /**
     * Check if any Figma plugin is connected
     */
    fun hasConnections(): Boolean = connections.isNotEmpty()

    /**
     * Get the number of active connections
     */
    fun getConnectionCount(): Int = connections.size

    /**
     * Generate a unique request ID
     */
    private fun generateRequestId(): String {
        return "req_${requestIdCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
}