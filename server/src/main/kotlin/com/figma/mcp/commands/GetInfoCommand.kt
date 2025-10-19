package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.CommandRegistry
import com.figma.mcp.core.CommandResult
import com.figma.mcp.core.ICommandHandler
import com.figma.mcp.core.ILogger
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.lang.management.ManagementFactory

/**
 * Get Info Command Handler
 *
 * Returns server information and capabilities
 * Demonstrates dependency on CommandRegistry
 */
class GetInfoCommand(
    private val logger: ILogger,
    private val commandRegistry: CommandRegistry
) : ICommandHandler {
    override val commandName = "getInfo"

    override suspend fun execute(context: CommandContext): CommandResult {
        logger.debug("Executing getInfo command", "clientId" to context.clientId)

        val uptime = ManagementFactory.getRuntimeMXBean().uptime / 1000

        val responseData = buildJsonObject {
            put("serverName", "Figma MCP Server (Ktor)")
            put("version", "1.0.0")
            put("protocol", "MCP")
            put("language", "Kotlin")
            put("framework", "Ktor")

            put("capabilities", buildJsonObject {
                put("commands", buildJsonArray {
                    commandRegistry.getCommandNames().forEach { add(it) }
                })
                put("websocket", true)
                put("broadcasting", true)
            })

            put("uptime", uptime)
            put("timestamp", System.currentTimeMillis())
        }

        return CommandResult(
            success = true,
            data = responseData
        )
    }
}
