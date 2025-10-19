package com.figma.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * MCP Protocol Types
 *
 * This file defines types for the Model Context Protocol (MCP) that are used
 * to connect with Claude Code and other MCP clients.
 *
 * MCP Specification: https://spec.modelcontextprotocol.io/
 *
 * ## Protocol Overview
 * MCP uses JSON-RPC 2.0 for message exchange. The protocol defines:
 * - Initialize: Handshake and capability negotiation
 * - Tools: Functions that can be called by the AI
 * - Resources: Data that can be accessed by the AI
 * - Prompts: Templates for common interactions
 */

// ============================================================================
// STEP 1: Initialize Protocol
// ============================================================================

/**
 * Initialize Request Parameters
 *
 * Sent by the client (Claude Code) to start the MCP session.
 * This is always the first message in an MCP session.
 */
@Serializable
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: ClientCapabilities,
    val clientInfo: Implementation
)

@Serializable
data class ClientCapabilities(
    val roots: RootsCapability? = null,
    val sampling: JsonObject? = null,
    val experimental: JsonObject? = null
)

@Serializable
data class RootsCapability(
    val listChanged: Boolean = false
)

@Serializable
data class Implementation(
    val name: String,
    val version: String
)

/**
 * Initialize Response Result
 *
 * Server responds with its capabilities and information
 */
@Serializable
data class InitializeResult(
    val protocolVersion: String,
    val capabilities: ServerCapabilities,
    val serverInfo: Implementation,
    val instructions: String? = null
)

@Serializable
data class ServerCapabilities(
    val tools: ToolsCapability? = null,
    val resources: ResourcesCapability? = null,
    val prompts: PromptsCapability? = null,
    val logging: JsonObject? = null,
    val experimental: JsonObject? = null
)

@Serializable
data class ToolsCapability(
    val listChanged: Boolean = true
)

@Serializable
data class ResourcesCapability(
    val subscribe: Boolean = false,
    val listChanged: Boolean = true
)

@Serializable
data class PromptsCapability(
    val listChanged: Boolean = true
)

// ============================================================================
// STEP 2: Tools Protocol (Primary MCP Feature)
// ============================================================================

/**
 * Tool Definition
 *
 * A tool is a function that Claude Code can call.
 * Each tool has:
 * - name: Unique identifier
 * - description: What the tool does (shown to Claude)
 * - inputSchema: JSON Schema defining the tool's parameters
 */
@Serializable
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

/**
 * List Tools Request (no params needed)
 */
@Serializable
data class ListToolsParams(
    val cursor: String? = null
)

/**
 * List Tools Response
 */
@Serializable
data class ListToolsResult(
    val tools: List<Tool>,
    val nextCursor: String? = null
)

/**
 * Call Tool Request Parameters
 *
 * When Claude wants to use a tool, it sends this request
 */
@Serializable
data class CallToolParams(
    val name: String,
    val arguments: JsonObject? = null
)

/**
 * Call Tool Response Result
 *
 * The result of tool execution, returned to Claude
 */
@Serializable
data class CallToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

/**
 * Tool Content Types
 *
 * Tools can return text, images, or resources
 */
@Serializable
sealed class ToolContent {
    @Serializable
    data class TextContent(
        val type: String = "text",
        val text: String
    ) : ToolContent()

    @Serializable
    data class ImageContent(
        val type: String = "image",
        val data: String,
        val mimeType: String
    ) : ToolContent()

    @Serializable
    data class EmbeddedResource(
        val type: String = "resource",
        val resource: ResourceContents
    ) : ToolContent()
}

// ============================================================================
// STEP 3: Resources Protocol
// ============================================================================

/**
 * Resource Definition
 *
 * Resources are data sources that Claude can read from
 */
@Serializable
data class Resource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

/**
 * Resource Contents
 */
@Serializable
data class ResourceContents(
    val uri: String,
    val mimeType: String? = null,
    val text: String? = null,
    val blob: String? = null
)

@Serializable
data class ListResourcesParams(
    val cursor: String? = null
)

@Serializable
data class ListResourcesResult(
    val resources: List<Resource>,
    val nextCursor: String? = null
)

@Serializable
data class ReadResourceParams(
    val uri: String
)

@Serializable
data class ReadResourceResult(
    val contents: List<ResourceContents>
)

// ============================================================================
// STEP 4: Prompts Protocol (Optional)
// ============================================================================

/**
 * Prompt Definition
 *
 * Prompts are templates for common interactions
 */
@Serializable
data class Prompt(
    val name: String,
    val description: String? = null,
    val arguments: List<PromptArgument>? = null
)

@Serializable
data class PromptArgument(
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

@Serializable
data class ListPromptsParams(
    val cursor: String? = null
)

@Serializable
data class ListPromptsResult(
    val prompts: List<Prompt>,
    val nextCursor: String? = null
)

@Serializable
data class GetPromptParams(
    val name: String,
    val arguments: JsonObject? = null
)

@Serializable
data class GetPromptResult(
    val description: String? = null,
    val messages: List<PromptMessage>
)

@Serializable
data class PromptMessage(
    val role: String, // "user" or "assistant"
    val content: ToolContent.TextContent
)

// ============================================================================
// STEP 5: Logging Protocol (Optional)
// ============================================================================

/**
 * Log Levels for MCP logging
 */
@Serializable
enum class LogLevel {
    debug,
    info,
    notice,
    warning,
    error,
    critical,
    alert,
    emergency
}

@Serializable
data class SetLevelParams(
    val level: LogLevel
)

// ============================================================================
// Utility Types
// ============================================================================

/**
 * JSON Schema helper for tool input schemas
 */
object JSONSchema {
    /**
     * Create a simple object schema for tool parameters
     *
     * Example:
     * ```
     * JSONSchema.createObjectSchema(
     *     properties = mapOf(
     *         "nodeId" to mapOf("type" to "string", "description" to "The node ID"),
     *         "color" to mapOf("type" to "string", "description" to "Hex color code")
     *     ),
     *     required = listOf("nodeId")
     * )
     * ```
     */
    fun createObjectSchema(
        properties: Map<String, Map<String, Any>>,
        required: List<String> = emptyList()
    ): JsonObject {
        return kotlinx.serialization.json.buildJsonObject {
            put("type", kotlinx.serialization.json.JsonPrimitive("object"))
            put("properties", kotlinx.serialization.json.buildJsonObject {
                properties.forEach { (key, value) ->
                    put(key, kotlinx.serialization.json.buildJsonObject {
                        value.forEach { (k, v) ->
                            put(k, when (v) {
                                is String -> kotlinx.serialization.json.JsonPrimitive(v)
                                is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                                is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                                else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                            })
                        }
                    })
                }
            })
            if (required.isNotEmpty()) {
                put("required", kotlinx.serialization.json.buildJsonArray {
                    required.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                })
            }
        }
    }
}
