package com.figma.mcp.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP Request following JSON-RPC 2.0 style protocol
 */
@Serializable
data class MCPRequest(
    val id: String,
    val method: String,
    val params: JsonElement? = null
)

/**
 * MCP Response
 */
@Serializable
data class MCPResponse(
    val id: String,
    val result: JsonElement? = null,
    val error: MCPError? = null
)

/**
 * MCP Error
 */
@Serializable
data class MCPError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

/**
 * Standard error codes following JSON-RPC 2.0
 */
object ErrorCode {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
    const val SERVER_ERROR = -32000
}

/**
 * Command execution context
 */
data class CommandContext(
    val requestId: String,
    val clientId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val params: JsonElement? = null
)

/**
 * Command execution result
 */
data class CommandResult(
    val success: Boolean,
    val data: JsonElement? = null,
    val error: CommandError? = null
)

/**
 * Command-specific error
 */
data class CommandError(
    val code: Int,
    val message: String,
    val details: JsonElement? = null
)
