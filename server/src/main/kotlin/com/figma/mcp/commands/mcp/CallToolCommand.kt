package com.figma.mcp.commands.mcp

import com.figma.mcp.core.*
import com.figma.mcp.protocol.*
import com.figma.mcp.tools.FigmaToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * MCP Call Tool Command Handler - REFACTORED
 *
 * ## Purpose
 * Handles the `tools/call` method which executes a specific tool.
 * This is called when Claude Code wants to use one of our Figma tools.
 *
 * ## SOLID Principles Applied (REFACTORED)
 *
 * ### Open-Closed Principle (OCP) - FIXED ✅
 * **BEFORE**: Giant when/switch statement that required modification for every new tool
 * **AFTER**: Uses FigmaToolRegistry - new tools are added by registration, not code modification
 *
 * ### Dependency Inversion Principle (DIP) - FIXED ✅
 * **BEFORE**: Depended on concrete FigmaToolExecutor
 * **AFTER**: Depends on FigmaToolRegistry abstraction
 *
 * ### Single Responsibility Principle (SRP) - IMPROVED ✅
 * **BEFORE**: Knew about every tool and how to route to them
 * **AFTER**: Only responsible for MCP protocol handling, delegates tool execution to registry
 *
 * ## Flow
 * 1. Claude sends tools/call with tool name and arguments
 * 2. We validate the request structure
 * 3. We delegate to FigmaToolRegistry which handles tool lookup and execution
 * 4. We return the result to Claude
 *
 * ## Adding New Tools
 * No code changes needed here! Just register the tool in DI configuration.
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/server/tools/
 */
class CallToolCommand(
    private val logger: ILogger,
    private val json: Json,
    private val toolRegistry: FigmaToolRegistry  // ✅ Depends on abstraction, not concrete class
) : ICommandHandler {

    override val commandName: String = "tools/call"

    /**
     * Execute a Figma tool via the registry
     *
     * This method is now MUCH simpler - just parse params and delegate to registry.
     * No switch statements, no modification needed when adding new tools.
     */
    override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            // Parse the call tool parameters
            val params = context.params?.let {
                json.decodeFromJsonElement(CallToolParams.serializer(), it)
            } ?: return CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INVALID_PARAMS,
                    message = "Missing tool call parameters"
                )
            )

            logger.info(
                "Executing tool",
                "tool" to params.name,
                "clientId" to context.clientId,
                "hasArguments" to (params.arguments != null)
            )

            // ✅ REFACTORED: Single line instead of giant when statement
            // The registry handles tool lookup, validation, and execution
            val result = toolRegistry.executeTool(
                toolName = params.name,
                arguments = params.arguments ?: JsonObject(emptyMap()),
                validateArgs = true
            )

            logger.info(
                "Tool execution completed",
                "tool" to params.name,
                "isError" to result.isError
            )

            CommandResult(
                success = true,
                data = json.encodeToJsonElement(CallToolResult.serializer(), result)
            )
        } catch (e: Exception) {
            logger.error("Tool execution failed", e)
            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Tool execution failed: ${e.message}"
                )
            )
        }
    }

    override fun validate(params: kotlinx.serialization.json.JsonElement?): String? {
        if (params == null) {
            return "Missing parameters"
        }

        try {
            val callParams = json.decodeFromJsonElement(CallToolParams.serializer(), params)
            if (callParams.name.isBlank()) {
                return "Tool name is required"
            }
        } catch (e: Exception) {
            return "Invalid parameters: ${e.message}"
        }

        return null
    }
}
