package com.figma.mcp.core

/**
 * Command Registry
 *
 * Open/Closed Principle - Open for extension (add new commands),
 * closed for modification (no need to change existing code)
 *
 * Single Responsibility - Only manages command registration and lookup
 */
class CommandRegistry(private val logger: ILogger) {
    private val handlers = mutableMapOf<String, ICommandHandler>()

    /**
     * Register a command handler
     * @param handler Command handler to register
     */
    fun register(handler: ICommandHandler) {
        if (handlers.containsKey(handler.commandName)) {
            logger.warn("Command handler for '${handler.commandName}' is being replaced")
        }

        handlers[handler.commandName] = handler
        logger.info("Registered command handler: ${handler.commandName}")
    }

    /**
     * Register multiple command handlers at once
     * @param handlers List of command handlers to register
     */
    fun registerAll(vararg handlers: ICommandHandler) {
        handlers.forEach { register(it) }
    }

    /**
     * Get a command handler by name
     * @param commandName Name of the command
     * @return Command handler if found, null otherwise
     */
    fun get(commandName: String): ICommandHandler? = handlers[commandName]

    /**
     * Check if a command handler exists
     * @param commandName Name of the command
     * @return true if handler exists, false otherwise
     */
    fun has(commandName: String): Boolean = handlers.containsKey(commandName)

    /**
     * Get all registered command names
     * @return List of command names
     */
    fun getCommandNames(): List<String> = handlers.keys.toList()

    /**
     * Unregister a command handler
     * @param commandName Name of the command to unregister
     * @return true if handler was removed, false if not found
     */
    fun unregister(commandName: String): Boolean {
        val removed = handlers.remove(commandName) != null
        if (removed) {
            logger.info("Unregistered command handler: $commandName")
        }
        return removed
    }

    /**
     * Clear all registered handlers
     */
    fun clear() {
        handlers.clear()
        logger.info("Cleared all command handlers")
    }

    /**
     * Get the count of registered handlers
     */
    val count: Int
        get() = handlers.size
}
