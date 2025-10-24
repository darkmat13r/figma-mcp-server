package com.figma.mcp.routes

import com.figma.mcp.core.*
import com.figma.mcp.session.SessionConstants
import com.figma.mcp.session.WebSocketSessionManager
import com.figma.mcp.session.WebSocketConnectionWrapper
import com.figma.mcp.session.WebSocketMessage
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

/**
 * WebSocket Routes Configuration with File-Specific Routing
 *
 * ## Purpose
 * Handles WebSocket connections from Figma plugins with file-specific routing.
 *
 * ## Architecture
 * WebSocket URL format: ws://localhost:8080/figma?fileId=abc123
 * - Extracts fileId from query parameters
 * - Registers the WebSocket session with its file ID
 * - Enables routing of commands to specific Figma file instances
 *
 * ## SOLID Principles
 * - Single Responsibility: Only handles WebSocket routing and message forwarding
 * - Dependency Inversion: Depends on abstractions (ILogger, CommandRegistry, etc.)
 */
class WebSocketRoutes(
    private val commandRegistry: CommandRegistry,
    private val logger: ILogger,
    private val json: Json,
    private val figmaConnectionManager: com.figma.mcp.services.FigmaConnectionManager,
    private val webSocketSessionManager: WebSocketSessionManager
) {
    // Coroutine scope for actors
    private val actorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun Route.configureWebSocketRoutes() {
        // Main Figma plugin WebSocket endpoint with fileId support
        // URL: ws://localhost:8080/figma?fileId=abc123
        webSocket(SessionConstants.WS_PATH_FIGMA) {
            handleFigmaPluginConnection(this)
        }

        // Legacy root endpoint for backward compatibility
        webSocket(SessionConstants.WS_PATH_ROOT) {
            val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]

            if (fileId.isNullOrBlank()) {
                logger.warn("WebSocket connection attempt without fileId on legacy endpoint")
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "Please use /figma?fileId=your-file-id endpoint"
                    )
                )
                return@webSocket
            }

            handleFigmaPluginConnection(this)
        }
    }

    /**
     * Create a sender actor for thread-safe WebSocket sending
     */
    private fun createSenderActor(
        sessionId: String,
        session: DefaultWebSocketServerSession
    ): kotlinx.coroutines.channels.SendChannel<WebSocketMessage> {
        val channel = Channel<WebSocketMessage>(Channel.UNLIMITED)

        actorScope.launch {
            logger.debug("Sender actor started", "sessionId" to sessionId)
            try {
                for (msg in channel) {
                    when (msg) {
                        is WebSocketMessage.Send -> {
                            try {
                                session.send(msg.frame)
                                msg.result.complete(Unit)
                            } catch (e: Exception) {
                                logger.error("Actor failed to send frame", e, "sessionId" to sessionId)
                                msg.result.completeExceptionally(e)
                            }
                        }
                        is WebSocketMessage.Close -> {
                            logger.debug("Sender actor closing", "sessionId" to sessionId)
                            break
                        }
                    }
                }
            } finally {
                logger.debug("Sender actor stopped", "sessionId" to sessionId)
            }
        }

        return channel
    }

    /**
     * Handle a Figma plugin WebSocket connection
     */
    private suspend fun handleFigmaPluginConnection(session: DefaultWebSocketServerSession) {
        with(session) {
            // Extract fileId from query parameters
            val fileId = call.request.queryParameters[SessionConstants.QUERY_PARAM_FILE_ID]

            if (fileId.isNullOrBlank()) {
                logger.warn("WebSocket connection attempt without fileId")
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "${SessionConstants.ERROR_NO_FILE_ID}. Please connect with: ws://localhost:8080/figma?fileId=your-file-id"
                    )
                )
                return
            }

            // Generate unique session ID for this WebSocket connection
            val sessionId = "${SessionConstants.SESSION_ID_PREFIX_WS}${UUID.randomUUID()}"

            // Create sender actor for thread-safe sending
            val senderActor = createSenderActor(sessionId, this)

            // Create connection wrapper
            val connectionWrapper = WebSocketConnectionWrapper(
                session = this,
                senderActor = senderActor,
                fileId = fileId
            )

            // Register with both managers
            webSocketSessionManager.registerSession(sessionId, fileId, connectionWrapper)
            figmaConnectionManager.registerConnection(sessionId, this)

            logger.info(
                "Figma plugin connected",
                "sessionId" to sessionId,
                "fileId" to fileId,
                "totalSessions" to webSocketSessionManager.getSessionCount()
            )

            try {
                // Send welcome message
                val welcomeResponse = MCPResponse(
                    id = UUID.randomUUID().toString(),
                    result = json.parseToJsonElement(
                        """
                        {
                            "status": "connected",
                            "sessionId": "$sessionId",
                            "fileId": "$fileId",
                            "availableCommands": ${json.encodeToString(commandRegistry.getCommandNames())}
                        }
                        """.trim()
                    )
                )
                send(Frame.Text(json.encodeToString(welcomeResponse)))

                // Handle incoming messages
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val receivedText = frame.readText()
                            handleMessage(sessionId, fileId, receivedText)
                        }
                        is Frame.Close -> {
                            logger.info("Client requested close", "sessionId" to sessionId, "fileId" to fileId)
                        }
                        else -> {
                            logger.debug("Received non-text frame", "sessionId" to sessionId)
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Client connection closed", "sessionId" to sessionId, "fileId" to fileId)
            } catch (e: Exception) {
                logger.error("WebSocket error", e, "sessionId" to sessionId, "fileId" to fileId)
            } finally {
                webSocketSessionManager.unregisterSession(sessionId)
                figmaConnectionManager.unregisterConnection(sessionId)
                logger.info(
                    "Client disconnected",
                    "sessionId" to sessionId,
                    "fileId" to fileId,
                    "totalSessions" to webSocketSessionManager.getSessionCount()
                )
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleMessage(
        sessionId: String,
        fileId: String,
        message: String
    ) {
        try {
            logger.debug("Received message", "sessionId" to sessionId, "fileId" to fileId)

            // Try to parse as JSON first
            val jsonElement = try {
                json.parseToJsonElement(message)
            } catch (e: Exception) {
                sendErrorResponse(sessionId, "", ErrorCode.PARSE_ERROR, "Invalid JSON: ${e.message}")
                return
            }

            // Check if this is a response to a tool command (has 'id' or 'requestId' and 'result')
            if (jsonElement is kotlinx.serialization.json.JsonObject) {
                // Try both "id" and "requestId" fields for backward compatibility
                val idElement = jsonElement["id"] ?: jsonElement["requestId"]
                if (idElement is kotlinx.serialization.json.JsonPrimitive && idElement.isString) {
                    val id = idElement.content
                    if (id.startsWith("req_")) {
                        // This is a response to a tool command from FigmaConnectionManager
                        val result = jsonElement["result"]
                        logger.info(
                            "← WebSocket received tool response",
                            "requestId" to id,
                            "sessionId" to sessionId,
                            "fileId" to fileId,
                            "hasResult" to (result != null),
                            "hasError" to (jsonElement["error"] != null)
                        )
                        figmaConnectionManager.handleResponse(id, result)
                        return
                    }
                }
            }

            // Parse as regular MCP request
            val request = try {
                json.decodeFromString<MCPRequest>(message)
            } catch (e: Exception) {
                sendErrorResponse(sessionId, "", ErrorCode.PARSE_ERROR, "Invalid JSON: ${e.message}")
                return
            }

            // Validate request structure
            if (request.id.isEmpty() || request.method.isEmpty()) {
                sendErrorResponse(
                    sessionId,
                    request.id,
                    ErrorCode.INVALID_REQUEST,
                    "Missing id or method"
                )
                return
            }

            // Process the command
            processCommand(sessionId, fileId, request)
        } catch (e: Exception) {
            logger.error("Error handling message", e, "sessionId" to sessionId, "fileId" to fileId)
            sendErrorResponse(sessionId, "", ErrorCode.INTERNAL_ERROR, "Internal server error")
        }
    }

    private suspend fun DefaultWebSocketServerSession.processCommand(
        sessionId: String,
        fileId: String,
        request: MCPRequest
    ) {
        val handler = commandRegistry.get(request.method)

        if (handler == null) {
            sendErrorResponse(
                sessionId,
                request.id,
                ErrorCode.METHOD_NOT_FOUND,
                "Method '${request.method}' not found"
            )
            return
        }

        // Validate parameters if handler provides validation
        val validationError = handler.validate(request.params)
        if (validationError != null) {
            sendErrorResponse(sessionId, request.id, ErrorCode.INVALID_PARAMS, validationError)
            return
        }

        // Create command context
        val context = CommandContext(
            requestId = request.id,
            clientId = sessionId,
            timestamp = System.currentTimeMillis(),
            params = request.params
        )

        try {
            // Execute the command
            val result = handler.execute(context)

            if (result.success) {
                val response = MCPResponse(
                    id = request.id,
                    result = result.data
                )
                send(Frame.Text(json.encodeToString(response)))
            } else {
                val response = MCPResponse(
                    id = request.id,
                    error = MCPError(
                        code = result.error?.code ?: ErrorCode.SERVER_ERROR,
                        message = result.error?.message ?: "Command execution failed",
                        data = result.error?.details
                    )
                )
                send(Frame.Text(json.encodeToString(response)))
            }
        } catch (e: Exception) {
            logger.error("Error executing command", e, "method" to request.method, "sessionId" to sessionId)
            sendErrorResponse(
                sessionId,
                request.id,
                ErrorCode.INTERNAL_ERROR,
                "Command execution failed: ${e.message}"
            )
        }
    }

    private suspend fun DefaultWebSocketServerSession.sendErrorResponse(
        sessionId: String,
        requestId: String,
        code: Int,
        message: String
    ) {
        val response = MCPResponse(
            id = requestId,
            error = MCPError(code = code, message = message)
        )
        send(Frame.Text(json.encodeToString(response)))
        logger.debug("Sent error response", "sessionId" to sessionId, "code" to code)
    }
}
