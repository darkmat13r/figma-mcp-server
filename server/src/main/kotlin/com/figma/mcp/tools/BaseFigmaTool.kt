package com.figma.mcp.tools

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import kotlinx.serialization.json.*

/**
 * Base Figma Tool Abstract Class
 *
 * ## Purpose
 * Provides common functionality for all Figma tools to avoid code duplication (DRY principle).
 * Implements Template Method Pattern for tool execution flow.
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP)
 * - Handles only common tool execution logic
 * - Delegates specific tool behavior to subclasses
 *
 * ### Open-Closed Principle (OCP)
 * - Open for extension: Subclasses can override methods for custom behavior
 * - Closed for modification: Core template methods don't change
 *
 * ### Liskov Substitution Principle (LSP)
 * - All subclasses can be used wherever BaseFigmaTool is expected
 * - Subclasses extend behavior without changing base contracts
 *
 * ### Template Method Pattern
 * The execute() method defines the algorithm skeleton:
 * 1. Extract and validate parameters → extractParameters() (overrideable)
 * 2. Build command payload → buildCommandParams() (abstract - must implement)
 * 3. Send to Figma plugin → sendCommand() (common implementation)
 * 4. Format response → formatSuccessResponse() (overrideable)
 *
 * ## Usage Example
 * ```kotlin
 * class CreateFrameTool(
 *     logger: ILogger,
 *     connectionManager: FigmaConnectionManager
 * ) : BaseFigmaTool(logger, connectionManager, FigmaConstants.ToolNames.CREATE_FRAME) {
 *
 *     override fun buildCommandParams(arguments: JsonObject): JsonObject {
 *         return buildJsonObject {
 *             put("type", FigmaConstants.NodeTypes.FRAME)
 *             put("width", arguments.getDoubleOrDefault("width", 100.0))
 *             // ...
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseFigmaTool(
    protected val logger: ILogger,
    protected val connectionManager: FigmaConnectionManager,
    final override val toolName: String
) : IFigmaTool {

    /**
     * Template method: Defines the tool execution flow
     *
     * This method orchestrates the tool execution process:
     * 1. Extract parameters from arguments
     * 2. Validate parameters (if validation is enabled)
     * 3. Build command payload for Figma plugin
     * 4. Send command via WebSocket
     * 5. Handle response and format result
     *
     * Subclasses should NOT override this method.
     * Override the specific template methods instead.
     */
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            logger.debug(
                "Tool execution started",
                "toolName" to toolName,
                "arguments" to arguments.toString()
            )

            // Step 1: Extract parameters (overrideable)
            val extractedParams = extractParameters(arguments)

            // Step 2: Build command for Figma plugin (abstract - must implement)
            val commandParams = buildCommandParams(extractedParams)

            // Step 3: Determine which plugin method to call (default: createNode)
            val pluginMethod = getPluginMethod()

            // Step 4: Send command to Figma plugin
            val pluginResponse = sendCommand(pluginMethod, commandParams)

            // Step 5: Format and return success response
            formatSuccessResponse(pluginResponse, extractedParams)

        } catch (e: ParameterValidationException) {
            logger.warn("Parameter validation failed", "toolName" to toolName, "error" to e.message)
            errorResult(e.message ?: FigmaConstants.ErrorMessages.INVALID_PARAM_VALUE)

        } catch (e: FigmaConnectionException) {
            logger.error("Figma connection failed", e, "toolName" to toolName)
            errorResult(FigmaConstants.ErrorMessages.NO_FIGMA_CONNECTION)

        } catch (e: Exception) {
            logger.error("Tool execution failed", e, "toolName" to toolName)
            errorResult("${FigmaConstants.ErrorMessages.TOOL_EXECUTION_FAILED}: ${e.message}")
        }
    }

    /**
     * Extract and optionally transform parameters from arguments
     *
     * Override this method if you need custom parameter extraction logic.
     * Default implementation returns arguments as-is.
     *
     * @param arguments Raw arguments from MCP
     * @return Extracted/transformed parameters
     * @throws ParameterValidationException if parameters are invalid
     */
    protected open fun extractParameters(arguments: JsonObject): JsonObject {
        return arguments
    }

    /**
     * Build command parameters to send to Figma plugin
     *
     * This is the main method subclasses must implement.
     * Transform MCP arguments into Figma plugin command format.
     *
     * @param params Extracted parameters
     * @return JsonObject to send to Figma plugin
     */
    protected abstract fun buildCommandParams(params: JsonObject): JsonObject

    /**
     * Get the Figma plugin method name for this tool
     *
     * Override this method if your tool uses a different plugin method.
     * Default: FigmaConstants.PluginMethods.CREATE_NODE
     *
     * @return Plugin method name
     */
    protected open fun getPluginMethod(): String {
        return FigmaConstants.PluginMethods.CREATE_NODE
    }

    /**
     * Send command to Figma plugin via WebSocket
     *
     * Common implementation that all tools use.
     * Handles connection errors and timeouts.
     *
     * @param method Plugin method name
     * @param params Command parameters
     * @return Response from Figma plugin
     * @throws FigmaConnectionException if no plugin is connected
     */
    protected suspend fun sendCommand(method: String, params: JsonObject): JsonElement? {
        if (!connectionManager.hasConnections()) {
            throw FigmaConnectionException(FigmaConstants.ErrorMessages.NO_FIGMA_CONNECTION)
        }

        return connectionManager.sendCommand(
            method = method,
            params = params,
            timeout = FigmaConstants.Defaults.WS_TIMEOUT_MS
        )
    }

    /**
     * Format successful response for MCP
     *
     * Override this method for custom success message formatting.
     * Default implementation returns a generic success message.
     *
     * @param pluginResponse Response from Figma plugin
     * @param params Original parameters
     * @return CallToolResult with success content
     */
    protected open fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        val message = buildSuccessMessage(pluginResponse, params)

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = false
        )
    }

    /**
     * Build success message text
     *
     * Override this method to customize success messages.
     * Default implementation uses node type from response or parameters.
     *
     * @param pluginResponse Response from Figma plugin
     * @param params Original parameters
     * @return Success message string
     */
    protected open fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        // Try to extract nodeId from response
        val nodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        // Try to extract node type from parameters
        val nodeType = params["type"]?.jsonPrimitive?.contentOrNull
            ?: toolName.removePrefix("figma_create_").uppercase()

        return if (nodeId != null) {
            FigmaConstants.SuccessMessages.nodeCreatedWithId(nodeType, nodeId)
        } else {
            FigmaConstants.SuccessMessages.nodeCreated(nodeType)
        }
    }

    // ========================================================================
    // HELPER METHODS FOR PARAMETER EXTRACTION
    // ========================================================================

    /**
     * Get required string parameter
     * @throws ParameterValidationException if missing or not a string
     */
    protected fun JsonObject.getRequiredString(key: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull
            ?: throw ParameterValidationException(FigmaConstants.ErrorMessages.missingParam(key))
    }

    /**
     * Get optional string parameter with default value
     */
    protected fun JsonObject.getStringOrDefault(key: String, default: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull ?: default
    }

    /**
     * Get optional string parameter
     */
    protected fun JsonObject.getStringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    /**
     * Get required number parameter
     * @throws ParameterValidationException if missing or not a number
     */
    protected fun JsonObject.getRequiredDouble(key: String): Double {
        return this[key]?.jsonPrimitive?.doubleOrNull
            ?: throw ParameterValidationException(FigmaConstants.ErrorMessages.missingParam(key))
    }

    /**
     * Get optional number parameter with default value
     */
    protected fun JsonObject.getDoubleOrDefault(key: String, default: Double): Double {
        return this[key]?.jsonPrimitive?.doubleOrNull ?: default
    }

    /**
     * Get optional number parameter
     */
    protected fun JsonObject.getDoubleOrNull(key: String): Double? {
        return this[key]?.jsonPrimitive?.doubleOrNull
    }

    /**
     * Get required integer parameter
     * @throws ParameterValidationException if missing or not an integer
     */
    protected fun JsonObject.getRequiredInt(key: String): Int {
        return this[key]?.jsonPrimitive?.intOrNull
            ?: throw ParameterValidationException(FigmaConstants.ErrorMessages.missingParam(key))
    }

    /**
     * Get optional integer parameter with default value
     */
    protected fun JsonObject.getIntOrDefault(key: String, default: Int): Int {
        return this[key]?.jsonPrimitive?.intOrNull ?: default
    }

    /**
     * Get required string array parameter
     * @throws ParameterValidationException if missing or not an array
     */
    protected fun JsonObject.getRequiredStringArray(key: String): List<String> {
        val array = this[key]?.jsonArray
            ?: throw ParameterValidationException(FigmaConstants.ErrorMessages.missingParam(key))

        return array.mapNotNull { it.jsonPrimitive.contentOrNull }
    }

    /**
     * Get optional JSON object parameter
     */
    protected fun JsonObject.getObjectOrNull(key: String): JsonObject? {
        return this[key]?.jsonObject
    }

    /**
     * Get required JSON array parameter
     * @throws ParameterValidationException if missing
     */
    protected fun JsonObject.getRequiredArray(key: String): JsonArray {
        return this[key]?.jsonArray
            ?: throw ParameterValidationException(FigmaConstants.ErrorMessages.missingParam(key))
    }

    /**
     * Validate number is within range
     * @throws ParameterValidationException if out of range
     */
    protected fun validateRange(value: Int, min: Int, max: Int, paramName: String) {
        if (value < min || value > max) {
            throw ParameterValidationException(
                FigmaConstants.ErrorMessages.invalidRange(paramName, min, max)
            )
        }
    }

    /**
     * Helper function to create an error result
     */
    protected fun errorResult(message: String): CallToolResult {
        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = message)
            ),
            isError = true
        )
    }
}

/**
 * Custom exception for parameter validation failures
 */
class ParameterValidationException(message: String) : Exception(message)

/**
 * Custom exception for Figma connection failures
 */
class FigmaConnectionException(message: String) : Exception(message)
