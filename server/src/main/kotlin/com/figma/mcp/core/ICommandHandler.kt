package com.figma.mcp.core

/**
 * Command Handler Interface
 *
 * Follows Interface Segregation Principle - small, focused interface
 * Each command handler implements this interface
 *
 * Benefits:
 * - Single Responsibility: Each handler does one thing
 * - Liskov Substitution: All handlers are interchangeable
 * - Easy to test with mocks
 */
interface ICommandHandler {
    /**
     * The command name this handler processes
     */
    val commandName: String

    /**
     * Execute the command with given context
     * @param context Command execution context
     * @return CommandResult with success/failure and data
     */
    suspend fun execute(context: CommandContext): CommandResult

    /**
     * Validate command parameters (optional)
     * @param params Parameters to validate
     * @return null if valid, error message if invalid
     */
    fun validate(params: kotlinx.serialization.json.JsonElement?): String? = null
}
