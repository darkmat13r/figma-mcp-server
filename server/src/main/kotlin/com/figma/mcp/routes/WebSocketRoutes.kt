package com.figma.mcp.routes

import com.figma.mcp.core.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket Routes Configuration
 *
 * Handles WebSocket connections and message routing
 * Follows Single Responsibility Principle - only handles routing
 */
class WebSocketRoutes(
    private val commandRegistry: CommandRegistry,
    private val logger: ILogger,
    private val json: Json,
    private val figmaConnectionManager: com.figma.mcp.services.FigmaConnectionManager
) {
    // Store active WebSocket sessions
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    fun Route.configureWebSocketRoutes() {
        webSocket("/") {
            val clientId = UUID.randomUUID().toString()
            sessions[clientId] = this

            // Register with FigmaConnectionManager so tools can send commands to this plugin
            figmaConnectionManager.registerConnection(clientId, this)

            logger.info("Client connected", "clientId" to clientId, "totalClients" to sessions.size)

            try {
                // Send welcome message
                val welcomeResponse = MCPResponse(
                    id = UUID.randomUUID().toString(),
                    result = json.parseToJsonElement(
                        """
                        {
                            "status": "connected",
                            "clientId": "$clientId",
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
                            handleMessage(clientId, receivedText)
                        }
                        is Frame.Close -> {
                            logger.info("Client requested close", "clientId" to clientId)
                        }
                        else -> {
                            logger.debug("Received non-text frame", "clientId" to clientId)
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Client connection closed", "clientId" to clientId)
            } catch (e: Exception) {
                logger.error("WebSocket error", e, "clientId" to clientId)
            } finally {
                sessions.remove(clientId)
                // Unregister from FigmaConnectionManager
                figmaConnectionManager.unregisterConnection(clientId)
                logger.info("Client disconnected", "clientId" to clientId, "totalClients" to sessions.size)
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleMessage(clientId: String, message: String) {
        try {
            logger.debug("Received message", "clientId" to clientId, "message" to message)

            // Try to parse as JSON first
            val jsonElement = try {
                json.parseToJsonElement(message)
            } catch (e: Exception) {
                sendErrorResponse(clientId, "", ErrorCode.PARSE_ERROR, "Invalid JSON: ${e.message}")
                return
            }

            // Check if this is a response to a tool command (has 'id' and 'result')
            if (jsonElement is kotlinx.serialization.json.JsonObject) {
                val idElement = jsonElement["id"]
                if (idElement is kotlinx.serialization.json.JsonPrimitive && idElement.isString) {
                    val id = idElement.content
                    if (id.startsWith("req_")) {
                        // This is a response to a tool command from FigmaConnectionManager
                        logger.debug("Received tool response", "requestId" to id)
                        figmaConnectionManager.handleResponse(id, jsonElement["result"])
                        return
                    }
                }
            }

            // Parse as regular MCP request
            val request = try {
                json.decodeFromString<MCPRequest>(message)
            } catch (e: Exception) {
                sendErrorResponse(clientId, "", ErrorCode.PARSE_ERROR, "Invalid JSON: ${e.message}")
                return
            }

            // Validate request structure
            if (request.id.isEmpty() || request.method.isEmpty()) {
                sendErrorResponse(
                    clientId,
                    request.id,
                    ErrorCode.INVALID_REQUEST,
                    "Missing id or method"
                )
                return
            }

            // Process the command
            processCommand(clientId, request)
        } catch (e: Exception) {
            logger.error("Error handling message", e, "clientId" to clientId)
            sendErrorResponse(clientId, "", ErrorCode.INTERNAL_ERROR, "Internal server error")
        }
    }

    private suspend fun DefaultWebSocketServerSession.processCommand(clientId: String, request: MCPRequest) {
        val handler = commandRegistry.get(request.method)

        if (handler == null) {
            sendErrorResponse(
                clientId,
                request.id,
                ErrorCode.METHOD_NOT_FOUND,
                "Method '${request.method}' not found"
            )
            return
        }

        // Validate parameters if handler provides validation
        val validationError = handler.validate(request.params)
        if (validationError != null) {
            sendErrorResponse(clientId, request.id, ErrorCode.INVALID_PARAMS, validationError)
            return
        }

        // Create command context
        val context = CommandContext(
            requestId = request.id,
            clientId = clientId,
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
            logger.error("Error executing command", e, "method" to request.method, "clientId" to clientId)
            sendErrorResponse(
                clientId,
                request.id,
                ErrorCode.INTERNAL_ERROR,
                "Command execution failed: ${e.message}"
            )
        }
    }

    private suspend fun DefaultWebSocketServerSession.sendErrorResponse(
        clientId: String,
        requestId: String,
        code: Int,
        message: String
    ) {
        val response = MCPResponse(
            id = requestId,
            error = MCPError(code = code, message = message)
        )
        send(Frame.Text(json.encodeToString(response)))
        logger.debug("Sent error response", "clientId" to clientId, "code" to code)
    }

    /**
     * Broadcast message to all connected clients
     */
    suspend fun broadcast(message: String) {
        sessions.values.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.error("Error broadcasting to client", e)
            }
        }
    }

    /**
     * Get the number of connected clients
     */
    val clientCount: Int
        get() = sessions.size
}
