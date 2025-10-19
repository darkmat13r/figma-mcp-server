package com.figma.mcp.transport

import com.figma.mcp.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Stdio Transport for MCP Protocol
 *
 * ## Purpose
 * This transport enables communication with Claude Code via stdin/stdout.
 * Claude Code launches MCP servers as subprocesses and communicates via
 * stdio using JSON-RPC messages.
 *
 * ## How Claude Code MCP Works
 * 1. Claude Code reads MCP server config from ~/.config/claude/mcp_config.json
 * 2. It launches the server as a subprocess (e.g., `java -jar server.jar`)
 * 3. Claude Code sends JSON-RPC messages to the process's stdin
 * 4. The server reads from stdin, processes messages, and writes to stdout
 * 5. Claude Code reads responses from the process's stdout
 *
 * ## Message Format
 * Each message is a JSON-RPC 2.0 message on a single line:
 * ```
 * {"jsonrpc":"2.0","id":"1","method":"initialize","params":{...}}
 * {"jsonrpc":"2.0","id":"1","result":{...}}
 * ```
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/basic/transports/
 */
class StdioTransport(
    private val commandRegistry: CommandRegistry,
    private val logger: ILogger,
    private val json: Json
) {
    private val reader = BufferedReader(InputStreamReader(System.`in`))
    private val writer = PrintWriter(System.out, true)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Start the stdio transport
     *
     * This will:
     * 1. Read lines from stdin
     * 2. Parse JSON-RPC messages
     * 3. Execute commands
     * 4. Write responses to stdout
     */
    fun start() {
        logger.info("Starting MCP stdio transport")

        scope.launch {
            try {
                // Read lines from stdin
                while (true) {
                    val line = withContext(Dispatchers.IO) {
                        reader.readLine()
                    } ?: break // EOF reached

                    if (line.isBlank()) continue

                    // Process the message
                    processMessage(line)
                }
            } catch (e: Exception) {
                logger.error("Stdio transport error", e)
            } finally {
                logger.info("Stdio transport stopped")
            }
        }
    }

    /**
     * Stop the stdio transport
     */
    fun stop() {
        scope.cancel()
    }

    /**
     * Process a single JSON-RPC message
     */
    private suspend fun processMessage(message: String) {
        try {
            logger.debug("Received message", "message" to message)

            // Parse the JSON-RPC request
            val request = try {
                json.decodeFromString<MCPRequest>(message)
            } catch (e: Exception) {
                sendError("", ErrorCode.PARSE_ERROR, "Invalid JSON: ${e.message}")
                return
            }

            // Validate request
            if (request.id.isEmpty() || request.method.isEmpty()) {
                sendError(request.id, ErrorCode.INVALID_REQUEST, "Missing id or method")
                return
            }

            // Get the command handler
            val handler = commandRegistry.get(request.method)
            if (handler == null) {
                sendError(request.id, ErrorCode.METHOD_NOT_FOUND, "Method '${request.method}' not found")
                return
            }

            // Validate parameters
            val validationError = handler.validate(request.params)
            if (validationError != null) {
                sendError(request.id, ErrorCode.INVALID_PARAMS, validationError)
                return
            }

            // Create command context
            val context = CommandContext(
                requestId = request.id,
                clientId = "stdio",
                params = request.params
            )

            // Execute the command
            val result = handler.execute(context)

            // Send response
            if (result.success) {
                sendResponse(request.id, result.data)
            } else {
                sendError(
                    request.id,
                    result.error?.code ?: ErrorCode.INTERNAL_ERROR,
                    result.error?.message ?: "Command execution failed",
                    result.error?.details
                )
            }
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            sendError("", ErrorCode.INTERNAL_ERROR, "Internal error: ${e.message}")
        }
    }

    /**
     * Send a successful response
     */
    private fun sendResponse(id: String, result: kotlinx.serialization.json.JsonElement?) {
        val response = MCPResponse(
            id = id,
            result = result
        )
        writeMessage(json.encodeToString(response))
    }

    /**
     * Send an error response
     */
    private fun sendError(
        id: String,
        code: Int,
        message: String,
        data: kotlinx.serialization.json.JsonElement? = null
    ) {
        val response = MCPResponse(
            id = id,
            error = MCPError(
                code = code,
                message = message,
                data = data
            )
        )
        writeMessage(json.encodeToString(response))
    }

    /**
     * Write a message to stdout
     *
     * Each message MUST be on a single line (newline-delimited JSON)
     */
    private fun writeMessage(message: String) {
        synchronized(writer) {
            writer.println(message)
            writer.flush()
        }
        logger.debug("Sent message", "message" to message)
    }
}