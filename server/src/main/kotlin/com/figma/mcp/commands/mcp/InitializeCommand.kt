package com.figma.mcp.commands.mcp

import com.figma.mcp.core.*
import com.figma.mcp.protocol.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MCP Initialize Command Handler
 *
 * ## Purpose
 * Handles the `initialize` method which is the first MCP protocol message.
 * This establishes the connection and negotiates capabilities between the
 * MCP client (Claude Code) and this server.
 *
 * ## MCP Protocol Flow
 * 1. Client sends: initialize request with client info and capabilities
 * 2. Server responds: with server info, protocol version, and capabilities
 * 3. Client sends: initialized notification (acknowledgment)
 * 4. Normal operation: Client can now call tools, read resources, etc.
 *
 * ## Documentation
 * See: https://spec.modelcontextprotocol.io/specification/basic/lifecycle/
 */
class InitializeCommand(
    private val logger: ILogger,
    private val json: Json
) : ICommandHandler {

    override val commandName: String = "initialize"

    /**
     * Execute initialize handshake
     *
     * Returns server capabilities telling Claude Code what this server can do:
     * - tools: We provide Figma manipulation tools
     * - resources: (optional) We can provide Figma design resources
     * - prompts: (optional) Pre-defined prompt templates
     */
    override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            logger.info(
                "MCP Initialize request received",
                "clientId" to context.clientId,
                "requestId" to context.requestId
            )

            // Parse client initialize params (optional - we can initialize without them)
            val initParams = try {
                context.params?.let {
                    json.decodeFromJsonElement(InitializeParams.serializer(), it)
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse initialize params, using defaults", "error" to e.message)
                null
            }

            logger.info(
                "Client info",
                "client" to (initParams?.clientInfo?.name ?: "unknown"),
                "version" to (initParams?.clientInfo?.version ?: "unknown"),
                "protocolVersion" to (initParams?.protocolVersion ?: "unknown")
            )

            // Create initialize result with our server capabilities
            val result = InitializeResult(
                protocolVersion = SUPPORTED_PROTOCOL_VERSION,
                capabilities = ServerCapabilities(
                    tools = ToolsCapability(
                        listChanged = true // We support notifying when tools change
                    ),
                    resources = ResourcesCapability(
                        subscribe = false, // We don't support resource subscriptions yet
                        listChanged = true // We support notifying when resources change
                    ),
                    prompts = PromptsCapability(
                        listChanged = true // We support notifying when prompts change
                    )
                ),
                serverInfo = Implementation(
                    name = SERVER_NAME,
                    version = SERVER_VERSION
                ),
                instructions = """
                    Figma MCP Server

                    This server provides tools to interact with Figma designs:
                    - Create and manipulate nodes
                    - Read design information
                    - Access Figma resources

                    Use the tools/list method to see available tools.
                """.trimIndent()
            )

            logger.info("Initialize successful", "serverName" to SERVER_NAME)

            CommandResult(
                success = true,
                data = json.encodeToJsonElement(InitializeResult.serializer(), result)
            )
        } catch (e: Exception) {
            logger.error("Initialize failed", e)
            CommandResult(
                success = false,
                error = CommandError(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Initialize failed: ${e.message}"
                )
            )
        }
    }

    override fun validate(params: kotlinx.serialization.json.JsonElement?): String? {
        // Initialize can work with or without params
        return null
    }

    companion object {
        // MCP Protocol version we support
        private const val SUPPORTED_PROTOCOL_VERSION = "2024-11-05"

        // Server information
        private const val SERVER_NAME = "figma-mcp-server"
        private const val SERVER_VERSION = "1.0.0"
    }
}