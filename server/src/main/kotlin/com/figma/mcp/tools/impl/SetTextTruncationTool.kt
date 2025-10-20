package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Set Text Truncation Tool
 *
 * ## Purpose
 * Controls text overflow behavior (truncation with ellipsis).
 *
 * ## Parameters
 * - nodeId: string (required) - Target text node ID
 * - textTruncation: enum (required) - Truncation mode (DISABLED or ENDING)
 * - maxLines: number (optional) - Maximum number of lines before truncation
 */
class SetTextTruncationTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SET_TEXT_TRUNCATION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Controls text overflow behavior in Figma. " +
                    "DISABLED: No truncation, all text shown. " +
                    "ENDING: Truncate with ellipsis (...) after maxLines. " +
                    "Use maxLines parameter to control when truncation occurs.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the text node to configure (required)"
                    ),
                    ParamNames.TEXT_TRUNCATION to mapOf(
                        "type" to "string",
                        "description" to "Truncation mode (required)",
                        "enum" to listOf("DISABLED", "ENDING")
                    ),
                    ParamNames.MAX_LINES to mapOf(
                        "type" to "number",
                        "description" to "Maximum number of lines before truncation (optional, only applies when truncation is ENDING)"
                    )
                ),
                required = listOf(ParamNames.NODE_ID, ParamNames.TEXT_TRUNCATION)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SET_TEXT_TRUNCATION
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.NODE_ID, params.getRequiredString(ParamNames.NODE_ID))
            put(ParamNames.TEXT_TRUNCATION, params.getRequiredString(ParamNames.TEXT_TRUNCATION))

            // Optional max lines
            params[ParamNames.MAX_LINES]?.jsonPrimitive?.intOrNull?.let {
                put(ParamNames.MAX_LINES, it)
            }
        }
    }

    override fun buildSuccessMessage(pluginResponse: kotlinx.serialization.json.JsonElement?, params: JsonObject): String {
        val nodeId = params[ParamNames.NODE_ID]?.jsonPrimitive?.contentOrNull
        return "Successfully set text truncation for node: $nodeId"
    }
}
