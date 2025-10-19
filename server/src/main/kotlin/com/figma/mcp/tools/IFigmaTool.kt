package com.figma.mcp.tools

import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.Tool
import kotlinx.serialization.json.JsonObject

/**
 * Figma Tool Interface
 *
 * ## Purpose
 * Abstraction for all Figma tools following the Strategy Pattern.
 * Each tool is a self-contained strategy that can be executed independently.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Each tool implementation has ONE job: execute a specific Figma operation
 * - Tool definition, validation, and execution are separate concerns
 *
 * ### Open-Closed Principle (OCP)
 * - System is OPEN for extension: add new tools by implementing this interface
 * - System is CLOSED for modification: existing code doesn't change when adding tools
 *
 * ### Liskov Substitution Principle (LSP)
 * - All implementations are interchangeable through this interface
 * - Caller doesn't need to know which concrete tool it's using
 *
 * ### Interface Segregation Principle (ISP)
 * - Small, focused interface with only essential methods
 * - No client is forced to depend on methods it doesn't use
 *
 * ### Dependency Inversion Principle (DIP)
 * - High-level modules depend on this abstraction, not concrete tools
 * - FigmaToolRegistry depends on IFigmaTool, not CreateRectangleTool, etc.
 *
 * ## Usage Example
 * ```kotlin
 * class CreateFrameTool : IFigmaTool {
 *     override val toolName = FigmaConstants.ToolNames.CREATE_FRAME
 *
 *     override fun getDefinition(): Tool = Tool(...)
 *
 *     override suspend fun execute(arguments: JsonObject): CallToolResult {
 *         // Implementation
 *     }
 * }
 * ```
 */
interface IFigmaTool {

    /**
     * Unique tool name identifier
     * Must be one of FigmaConstants.ToolNames constants
     */
    val toolName: String

    /**
     * Get MCP tool definition for registration
     *
     * This definition is returned to Claude Code in the tools/list response.
     * It includes:
     * - Tool name
     * - Description (how Claude should use it)
     * - JSON Schema for input validation
     *
     * @return Tool definition for MCP protocol
     */
    fun getDefinition(): Tool

    /**
     * Execute the tool with provided arguments
     *
     * This method:
     * 1. Validates arguments
     * 2. Sends command to Figma plugin via WebSocket
     * 3. Waits for response
     * 4. Formats and returns result
     *
     * @param arguments JSON object containing tool parameters
     * @return CallToolResult with success/failure and content
     */
    suspend fun execute(arguments: JsonObject): CallToolResult

    /**
     * Validate tool arguments (optional, default implementation)
     *
     * Override this method to add custom validation logic before execution.
     * Return null if valid, error message if invalid.
     *
     * @param arguments JSON object to validate
     * @return null if valid, error message string if invalid
     */
    fun validate(arguments: JsonObject): String? = null
}
