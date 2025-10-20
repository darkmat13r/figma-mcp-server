package com.figma.mcp.tools.impl.variables

import com.figma.mcp.config.FigmaConstants.ParamNames
import com.figma.mcp.config.FigmaConstants.PluginMethods
import com.figma.mcp.config.FigmaConstants.ToolNames
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get Variables Tool
 *
 * ## Purpose
 * Lists available variables, optionally filtered by collection or type.
 *
 * ## Parameters
 * - collectionId (optional): Filter by collection ID
 * - type (optional): Filter by variable type (BOOLEAN | FLOAT | STRING | COLOR)
 *
 * ## Returns
 * {
 *   variables: Variable[] - Array of variable information
 * }
 */
class GetVariablesTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.GET_VARIABLES) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "List available variables in the document. " +
                    "Can be filtered by collection ID or variable type. " +
                    "Returns detailed information about each variable including name, type, and values.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.COLLECTION_ID to mapOf(
                        "type" to "string",
                        "description" to "Filter by collection ID (optional)"
                    ),
                    ParamNames.TYPE to mapOf(
                        "type" to "string",
                        "description" to "Filter by variable type (optional): BOOLEAN, FLOAT, STRING, or COLOR",
                        "enum" to listOf("BOOLEAN", "FLOAT", "STRING", "COLOR")
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GET_VARIABLES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull(ParamNames.COLLECTION_ID)?.let { put(ParamNames.COLLECTION_ID, it) }
            params.getStringOrNull(ParamNames.TYPE)?.let { put(ParamNames.TYPE, it) }
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the variables array data as JSON
        val variablesData = pluginResponse?.jsonObject ?: buildJsonObject {
            put("variables", buildJsonArray {})
        }

        val variableCount = variablesData["variables"]?.jsonArray?.size ?: 0

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Variables (${variableCount}): ${variablesData.toString()}")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val variableCount = pluginResponse?.jsonObject?.get("variables")?.jsonArray?.size ?: 0
        return "Successfully retrieved $variableCount variable(s)"
    }
}
