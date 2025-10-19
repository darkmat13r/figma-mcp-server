package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.CommandResult
import com.figma.mcp.core.ICommandHandler
import com.figma.mcp.core.ILogger
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Ping Command Handler
 *
 * Simple health check command
 * Demonstrates how to create a basic command handler
 */
class PingCommand(private val logger: ILogger) : ICommandHandler {
    override val commandName = "ping"

    override suspend fun execute(context: CommandContext): CommandResult {
        logger.debug("Executing ping command", "clientId" to context.clientId)

        val responseData = buildJsonObject {
            put("message", "pong")
            put("timestamp", System.currentTimeMillis())
            put("serverTime", java.time.Instant.now().toString())
        }

        return CommandResult(
            success = true,
            data = responseData
        )
    }
}
