package com.figma.mcp.tools

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Figma Tool Registry
 *
 * ## Purpose
 * Central registry for all Figma tools using the Registry Pattern.
 * Manages tool registration, lookup, and execution.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Only responsible for tool registration and routing
 * - Doesn't implement tool logic (delegated to IFigmaTool implementations)
 *
 * ### Open-Closed Principle (OCP)
 * - Adding new tools: Just call register() - no code modification needed
 * - Removing tools: Just remove from registration - no code modification needed
 * - The core registry logic never changes
 *
 * ### Dependency Inversion Principle (DIP)
 * - Depends on IFigmaTool abstraction, not concrete implementations
 * - Tools can be injected via constructor or register() method
 *
 * ## Design Pattern
 * This implements the **Registry Pattern** combined with **Strategy Pattern**:
 * - Registry: Maintains a map of tool name → tool implementation
 * - Strategy: Each tool is an interchangeable strategy
 *
 * ## Thread Safety
 * Uses ConcurrentHashMap for thread-safe registration and lookup
 *
 * ## Usage Example
 * ```kotlin
 * val registry = FigmaToolRegistry(logger)
 *
 * // Register tools
 * registry.register(CreateFrameTool(...))
 * registry.register(CreateRectangleTool(...))
 *
 * // Execute tool
 * val result = registry.executeTool("figma_create_frame", arguments)
 *
 * // Get all tool definitions
 * val tools = registry.getAllTools()
 * ```
 */
class FigmaToolRegistry(
    private val logger: ILogger
) {

    /**
     * Thread-safe map of tool name → tool implementation
     */
    private val tools = ConcurrentHashMap<String, IFigmaTool>()

    /**
     * Register a single Figma tool
     *
     * @param tool The tool implementation to register
     * @throws IllegalArgumentException if tool with same name already registered
     */
    fun register(tool: IFigmaTool) {
        val existingTool = tools.putIfAbsent(tool.toolName, tool)
        if (existingTool != null) {
            throw IllegalArgumentException(
                "Tool '${tool.toolName}' is already registered. " +
                "Each tool name must be unique."
            )
        }
        logger.info("Registered Figma tool", "toolName" to tool.toolName)
    }

    /**
     * Register multiple Figma tools at once
     *
     * @param toolList List of tools to register
     */
    fun registerAll(vararg toolList: IFigmaTool) {
        toolList.forEach { register(it) }
    }

    /**
     * Unregister a tool by name
     *
     * @param toolName The name of the tool to unregister
     * @return true if tool was unregistered, false if not found
     */
    fun unregister(toolName: String): Boolean {
        val removed = tools.remove(toolName)
        if (removed != null) {
            logger.info("Unregistered Figma tool", "toolName" to toolName)
            return true
        }
        return false
    }

    /**
     * Get a tool by name
     *
     * @param toolName The name of the tool
     * @return The tool implementation, or null if not found
     */
    fun getTool(toolName: String): IFigmaTool? {
        return tools[toolName]
    }

    /**
     * Check if a tool is registered
     *
     * @param toolName The name of the tool
     * @return true if registered, false otherwise
     */
    fun hasTool(toolName: String): Boolean {
        return tools.containsKey(toolName)
    }

    /**
     * Get all registered tool definitions for MCP tools/list response
     *
     * @return List of Tool definitions
     */
    fun getAllTools(): List<Tool> {
        return tools.values.map { it.getDefinition() }.sortedBy { it.name }
    }

    /**
     * Get all registered tool names
     *
     * @return List of tool names
     */
    fun getAllToolNames(): List<String> {
        return tools.keys.sorted()
    }

    /**
     * Execute a tool by name
     *
     * This method:
     * 1. Looks up the tool in the registry
     * 2. Validates the tool exists
     * 3. Optionally validates arguments
     * 4. Executes the tool
     * 5. Returns the result
     *
     * @param toolName The name of the tool to execute
     * @param arguments The arguments to pass to the tool
     * @param validateArgs Whether to run validation before execution (default: true)
     * @return CallToolResult with execution result or error
     */
    suspend fun executeTool(
        toolName: String,
        arguments: JsonObject,
        validateArgs: Boolean = true
    ): CallToolResult {
        logger.info("→ FigmaToolRegistry.executeTool() STARTED", "toolName" to toolName)

        // Look up the tool
        val tool = tools[toolName]
            ?: return errorResult("Unknown tool: $toolName")

        // Optionally validate arguments
        if (validateArgs) {
            val validationError = tool.validate(arguments)
            if (validationError != null) {
                return errorResult("Validation failed: $validationError")
            }
        }

        // Execute the tool
        return try {
            logger.info(
                "  Calling tool.execute()...",
                "toolName" to toolName,
                "hasArguments" to arguments.isNotEmpty()
            )

            val result = tool.execute(arguments)

            logger.info(
                "  ✓ tool.execute() returned",
                "toolName" to toolName,
                "isError" to result.isError,
                "contentSize" to result.content.size
            )

            logger.info("← FigmaToolRegistry.executeTool() RETURNING", "toolName" to toolName)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error("✗ Tool execution failed", e, "toolName" to toolName)
            errorResult("Tool execution failed: ${e.message}")
        }
    }

    /**
     * Get the count of registered tools
     *
     * @return Number of registered tools
     */
    fun getToolCount(): Int {
        return tools.size
    }

    /**
     * Clear all registered tools
     * Useful for testing or reinitialization
     */
    fun clear() {
        val count = tools.size
        tools.clear()
        logger.info("Cleared all Figma tools", "count" to count)
    }

    /**
     * Helper function to create an error result
     */
    private fun errorResult(message: String): CallToolResult {
        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = true
        )
    }
}
