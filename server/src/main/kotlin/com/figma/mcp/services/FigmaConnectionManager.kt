package com.figma.mcp.services

import com.figma.mcp.core.ILogger
import io.ktor.websocket.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Actor messages for thread-safe WebSocket sending
 */
private sealed class WebSocketMessage {
    data class Send(val frame: Frame.Text, val result: CompletableDeferred<Unit>) : WebSocketMessage()
    object Close : WebSocketMessage()
}

/**
 * Wrapper for WebSocket connection with dedicated sender actor
 */
private class ConnectionWrapper(
    val session: DefaultWebSocketServerSession,
    val senderActor: kotlinx.coroutines.channels.SendChannel<WebSocketMessage>
)

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
    // Active Figma plugin connections with their sender actors
    private val connections = ConcurrentHashMap<String, ConnectionWrapper>()

    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, Channel<JsonElement?>>()

    // Request ID generator
    private val requestIdCounter = AtomicLong(0)

    // Coroutine scope for actors
    private val actorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Create a sender actor for thread-safe WebSocket sending
     *
     * This actor ensures that all sends to a WebSocket are serialized,
     * preventing race conditions when multiple coroutines try to send
     * messages concurrently.
     */
    private fun createSenderActor(
        clientId: String,
        session: DefaultWebSocketServerSession
    ): kotlinx.coroutines.channels.SendChannel<WebSocketMessage> {
        val channel = Channel<WebSocketMessage>(Channel.UNLIMITED)

        actorScope.launch {
            logger.debug("Sender actor started", "clientId" to clientId)
            try {
                for (msg in channel) {
                    when (msg) {
                        is WebSocketMessage.Send -> {
                            try {
                                session.send(msg.frame)
                                msg.result.complete(Unit)
                            } catch (e: Exception) {
                                logger.error("Actor failed to send frame", e, "clientId" to clientId)
                                msg.result.completeExceptionally(e)
                            }
                        }
                        is WebSocketMessage.Close -> {
                            logger.debug("Sender actor closing", "clientId" to clientId)
                            break
                        }
                    }
                }
            } finally {
                logger.debug("Sender actor stopped", "clientId" to clientId)
            }
        }

        return channel
    }

    /**
     * Register a new Figma plugin connection
     */
    fun registerConnection(clientId: String, session: DefaultWebSocketServerSession) {
        val actor = createSenderActor(clientId, session)
        val wrapper = ConnectionWrapper(session, actor)
        connections[clientId] = wrapper
        logger.info("Figma plugin connected", "clientId" to clientId, "totalConnections" to connections.size)
    }

    /**
     * Unregister a Figma plugin connection
     */
    fun unregisterConnection(clientId: String) {
        val wrapper = connections.remove(clientId)
        if (wrapper != null) {
            // Send close message to actor and close the channel
            actorScope.launch {
                try {
                    wrapper.senderActor.send(WebSocketMessage.Close)
                    wrapper.senderActor.close()
                } catch (e: Exception) {
                    logger.warn("Error closing sender actor", "clientId" to clientId, "error" to e.message)
                }
            }
        }
        logger.info("Figma plugin disconnected", "clientId" to clientId, "totalConnections" to connections.size)
    }

    /**
     * Handle a response from the Figma plugin
     */
    suspend fun handleResponse(requestId: String, result: JsonElement?) {
        val channel = pendingRequests[requestId]
        if (channel != null) {
            logger.debug(
                "Received response from Figma plugin",
                "requestId" to requestId,
                "hasResult" to (result != null)
            )
            channel.send(result)
        } else {
            logger.warn(
                "Received response for unknown request (may have timed out)",
                "requestId" to requestId
            )
        }
    }

    /**
     * Send a command to the Figma plugin and wait for response
     *
     * @param method The method to call (e.g., "createNode", "getInfo")
     * @param params The parameters for the method
     * @param timeout Timeout in milliseconds (default 30000ms)
     * @return The result from the plugin, or null if timeout/error
     */
    suspend fun sendCommand(
        method: String,
        params: JsonObject,
        timeout: Long = 30000
    ): JsonElement? {
        val startTime = System.currentTimeMillis()

        // Get the first available connection (in production, you might want smarter routing)
        val connection = connections.values.firstOrNull()
            ?: throw IllegalStateException("No Figma plugin connected")

        val requestId = generateRequestId()
        val responseChannel = Channel<JsonElement?>(1)
        pendingRequests[requestId] = responseChannel

        logger.info(
            "→ SENDING command to Figma plugin",
            "requestId" to requestId,
            "method" to method,
            "pendingCount" to pendingRequests.size,
            "params" to params.toString()
        )

        try {
            // Send the command via actor to ensure thread-safe sending
            val command = buildJsonObject {
                put("id", requestId)
                put("method", method)
                put("params", params)
            }

            val frame = Frame.Text(command.toString())
            val sendResult = CompletableDeferred<Unit>()
            val message = WebSocketMessage.Send(frame, sendResult)

            connection.senderActor.send(message)
            sendResult.await() // Wait for actor to confirm send

            val sendTime = System.currentTimeMillis() - startTime
            logger.debug(
                "  ✓ Command sent to WebSocket via actor",
                "requestId" to requestId,
                "sendTimeMs" to sendTime
            )

            // Wait for response with timeout
            return try {
                logger.debug(
                    "  ⏳ Waiting for response...",
                    "requestId" to requestId,
                    "timeoutMs" to timeout
                )

                val result = withTimeout(timeout) {
                    responseChannel.receive()
                }

                val totalTime = System.currentTimeMillis() - startTime
                logger.info(
                    "← RECEIVED response from Figma plugin",
                    "requestId" to requestId,
                    "method" to method,
                    "totalTimeMs" to totalTime,
                    "hasResult" to (result != null)
                )

                result
            } catch (e: TimeoutCancellationException) {
                val totalTime = System.currentTimeMillis() - startTime
                logger.error(
                    "⚠ TIMEOUT waiting for Figma plugin response",
                    e,
                    "requestId" to requestId,
                    "method" to method,
                    "timeoutMs" to timeout,
                    "actualWaitMs" to totalTime,
                    "pendingCount" to pendingRequests.size
                )
                throw IllegalStateException(
                    "Figma plugin did not respond within ${timeout}ms. " +
                    "The plugin may be busy or disconnected. RequestId: $requestId, Method: $method"
                )
            }
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            logger.error(
                "✗ FAILED to send command to Figma plugin",
                e,
                "requestId" to requestId,
                "method" to method,
                "failedAfterMs" to totalTime
            )
            throw e
        } finally {
            pendingRequests.remove(requestId)
            responseChannel.close()

            logger.debug(
                "  Cleaned up request",
                "requestId" to requestId,
                "remainingPending" to pendingRequests.size
            )
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
     * Get diagnostic information about pending requests
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "activeConnections" to connections.size,
            "pendingRequests" to pendingRequests.size,
            "pendingRequestIds" to pendingRequests.keys.toList(),
            "totalRequestsSent" to requestIdCounter.get()
        )
    }

    /**
     * Generate a unique request ID
     */
    private fun generateRequestId(): String {
        return "req_${requestIdCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
}