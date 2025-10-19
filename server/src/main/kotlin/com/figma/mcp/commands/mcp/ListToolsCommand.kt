package com.figma.mcp.commands.mcp

import com.figma.mcp.core.*
import com.figma.mcp.protocol.*
import com.figma.mcp.tools.FigmaToolRegistry
import kotlinx.serialization.json.Json

/**
 * MCP List Tools Command Handler - REFACTORED
 *
 * ## Purpose
 * Handles the `tools/list` method which returns all available tools
 * that Claude Code can call.
 *
 * ## SOLID Principles Applied (REFACTORED)
 *
 * ### Open-Closed Principle (OCP) - FIXED ✅
 * **BEFORE**: Hardcoded list of tools - had to modify this class for every new tool
 * **AFTER**: Gets tools from FigmaToolRegistry - new tools are automatically included
 *
 * ### Single Responsibility Principle (SRP) - IMPROVED ✅
 * **BEFORE**: Knew about every tool and how to define them
 * **AFTER**: Only responsible for MCP protocol handling, delegates tool definitions to registry
 *
 * ### Dependency Inversion Principle (DIP) - FIXED ✅
 * **BEFORE**: No dependencies, but hardcoded all tool definitions
 * **AFTER**: Depends on FigmaToolRegistry abstraction
 *
 * ## What are MCP Tools?
 * Tools are functions that the AI assistant (Claude) can invoke.
 * Each tool has:
 * - A unique name
 * - A description (tells Claude what the tool does)
 * - An input schema (JSON Schema defining parameters)
 *
 * ## Adding New Tools
 * No code changes needed here! Just:
 * 1. Create a new tool class implementing IFigmaTool
 * 2. Register it in the DI configuration
 * 3. It automatically appears in the tools/list response
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/server/tools/
 */
class ListToolsCommand(
    private val logger: ILogger,
    private val json: Json,
    private val toolRegistry: FigmaToolRegistry  // ✅ Depends on abstraction
) : ICommandHandler {

    override val commandName: String = "tools/list"

    /**
     * Return the list of available Figma tools from the registry
     *
     * This method is now MUCH simpler - just get tools from registry.
     * No hardcoded tool definitions, no modification needed when adding new tools.
     */
    override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            logger.info("Listing MCP tools", "clientId" to context.clientId)

            // ✅ REFACTORED: Get all tools from registry instead of hardcoded list
            // Each tool provides its own definition via IFigmaTool.getDefinition()
            val tools = toolRegistry.getAllTools()

            val result = ListToolsResult(
                tools = tools,
                nextCursor = null // We return all tools at once (no pagination)
            )

            logger.info(
                "Returned tools from registry",
                "toolCount" to tools.size,
                "toolNames" to toolRegistry.getAllToolNames().joinToString(", ")
            )

            CommandResult(
                success = true,
                data = json.encodeToJsonElement(ListToolsResult.serializer(), result)
            )
        } catch (e: Exception) {
            logger.error("Failed to list tools", e)
            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Failed to list tools: ${e.message}"
                )
            )
        }
    }

    override fun validate(params: kotlinx.serialization.json.JsonElement?): String? {
        // tools/list doesn't require parameters
        return null
    }
}
