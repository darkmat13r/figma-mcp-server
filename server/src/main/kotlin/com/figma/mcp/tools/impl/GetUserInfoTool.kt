package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get User Info Tool
 *
 * ## Purpose
 * Retrieves information about the current user, file, viewport, and capabilities.
 *
 * ## Returns
 * - user: User information (id, name, photoUrl, color, sessionId) or null if no user
 * - file: File information (name, editorType)
 * - viewport: Viewport information (center, zoom)
 * - capabilities: User capabilities (canEditFile)
 */
class GetUserInfoTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_USER_INFO) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Retrieves information about the current user, file, viewport, and capabilities in Figma. " +
                    "Returns user details, file metadata, viewport state, and what the user can do.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_USER_INFO
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        // No parameters needed for getUserInfo
        return buildJsonObject {}
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        if (pluginResponse is JsonObject) {
            val userName = pluginResponse["user"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            val fileName = pluginResponse["file"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull

            return buildString {
                append("User Info Retrieved")
                if (userName != null) {
                    append(" - User: $userName")
                }
                if (fileName != null) {
                    append(", File: $fileName")
                }
            }
        }
        return "Successfully retrieved user info"
    }
}
