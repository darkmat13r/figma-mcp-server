package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.CommandResult
import com.figma.mcp.core.ICommandHandler
import com.figma.mcp.core.ILogger
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Echo Command Handler
 *
 * Echoes back the provided message
 * Demonstrates parameter validation
 */
class EchoCommand(private val logger: ILogger) : ICommandHandler {
    override val commandName = "echo"

    override fun validate(params: JsonElement?): String? {
        if (params == null) {
            return "Missing required parameter: message"
        }

        val jsonObject = params.jsonObject
        val message = jsonObject["message"]?.jsonPrimitive?.content

        if (message == null) {
            return "Missing required parameter: message"
        }

        if (message.isEmpty()) {
            return "Parameter 'message' cannot be empty"
        }

        return null
    }

    override suspend fun execute(context: CommandContext): CommandResult {
        val message = context.params?.jsonObject?.get("message")?.jsonPrimitive?.content

        logger.debug(
            "Executing echo command",
            "clientId" to context.clientId,
            "message" to message
        )

        val responseData = buildJsonObject {
            put("echo", message)
            put("receivedAt", java.time.Instant.now().toString())
            put("clientId", context.clientId)
        }

        return CommandResult(
            success = true,
            data = responseData
        )
    }
}
