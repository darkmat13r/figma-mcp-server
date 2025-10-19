package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.CommandError
import com.figma.mcp.core.CommandResult
import com.figma.mcp.core.ErrorCode
import com.figma.mcp.core.ICommandHandler
import com.figma.mcp.core.ILogger
import kotlinx.serialization.json.*

/**
 * Create Node Command Handler
 *
 * Handles Figma node creation requests
 * Demonstrates complex parameter validation and business logic
 */
class CreateNodeCommand(private val logger: ILogger) : ICommandHandler {
    override val commandName = "createNode"

    private val validTypes = listOf("RECTANGLE", "ELLIPSE", "FRAME", "TEXT", "LINE")

    override fun validate(params: JsonElement?): String? {
        if (params == null) {
            return "Missing required parameters"
        }

        val jsonObject = params.jsonObject

        // Validate type
        val type = jsonObject["type"]?.jsonPrimitive?.content
        if (type == null) {
            return "Missing required parameter: type"
        }

        if (type !in validTypes) {
            return "Invalid type. Must be one of: ${validTypes.joinToString(", ")}"
        }

        // Validate text for TEXT nodes
        if (type == "TEXT") {
            val text = jsonObject["text"]?.jsonPrimitive?.content
            if (text.isNullOrEmpty()) {
                return "Parameter 'text' is required for TEXT nodes"
            }
        }

        // Validate fill if provided
        val fill = jsonObject["fill"]?.jsonObject
        if (fill != null) {
            val r = fill["r"]?.jsonPrimitive?.doubleOrNull
            val g = fill["g"]?.jsonPrimitive?.doubleOrNull
            val b = fill["b"]?.jsonPrimitive?.doubleOrNull

            if (r == null || g == null || b == null) {
                return "Fill color must have numeric r, g, b values"
            }

            if (r !in 0.0..1.0 || g !in 0.0..1.0 || b !in 0.0..1.0) {
                return "Fill color values must be between 0 and 1"
            }

            val a = fill["a"]?.jsonPrimitive?.doubleOrNull
            if (a != null && a !in 0.0..1.0) {
                return "Fill alpha must be between 0 and 1"
            }
        }

        return null
    }

    override suspend fun execute(context: CommandContext): CommandResult {
        val params = context.params?.jsonObject ?: return CommandResult(
            success = false,
            error = CommandError(
                code = ErrorCode.INVALID_PARAMS,
                message = "Missing parameters"
            )
        )

        val type = params["type"]?.jsonPrimitive?.content ?: ""
        val name = params["name"]?.jsonPrimitive?.content

        logger.info(
            "Creating Figma node",
            "clientId" to context.clientId,
            "type" to type,
            "name" to name
        )

        return try {
            val nodeId = "node_${System.currentTimeMillis()}_${generateRandomId()}"

            val responseData = buildJsonObject {
                put("nodeId", nodeId)
                put("type", type)
                put("name", name ?: "${type}_$nodeId")

                put("properties", buildJsonObject {
                    put("x", params["x"]?.jsonPrimitive?.intOrNull ?: 0)
                    put("y", params["y"]?.jsonPrimitive?.intOrNull ?: 0)
                    put("width", params["width"]?.jsonPrimitive?.intOrNull ?: 100)
                    put("height", params["height"]?.jsonPrimitive?.intOrNull ?: 100)

                    params["fill"]?.let { put("fill", it) }
                    params["text"]?.let { put("text", it) }
                })

                put("createdAt", java.time.Instant.now().toString())
            }

            CommandResult(success = true, data = responseData)
        } catch (e: Exception) {
            logger.error(
                "Error creating node",
                e,
                "clientId" to context.clientId,
                "type" to type
            )

            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.SERVER_ERROR,
                    message = "Failed to create node",
                    details = buildJsonObject {
                        put("error", e.message ?: "Unknown error")
                    }
                )
            )
        }
    }

    private fun generateRandomId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..9).map { chars.random() }.joinToString("")
    }
}
