package com.figma.mcp.tools.impl.styles

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.CallToolResult
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.protocol.ToolContent
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Get Style By ID Tool
 *
 * ## Purpose
 * Retrieves a style by its unique ID from the current Figma document.
 * Supports all style types: Paint (color/fill), Text, Effect, and Grid styles.
 *
 * ## Parameters
 * - styleId: string (required) - The unique ID of the style to retrieve
 *
 * ## Returns
 * Style information including:
 * - id: Style ID
 * - name: Style name
 * - type: Style type (PAINT, TEXT, EFFECT, GRID)
 * - description: Style description (if any)
 * - Additional properties based on style type
 */
class GetStyleByIdTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_get_style_by_id") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Retrieves a style by its unique ID from the current Figma document. " +
                    "Returns the style's properties including name, type, and styling information. " +
                    "Supports all style types: Paint (colors/fills), Text, Effect (shadows/blurs), and Grid styles.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    "styleId" to mapOf(
                        "type" to "string",
                        "description" to "The unique ID of the style to retrieve (required)"
                    )
                ),
                required = listOf("styleId")
            )
        )
    }

    override fun getPluginMethod(): String {
        return "getStyleById"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val styleId = params.getRequiredString("styleId")

        return buildJsonObject {
            put("styleId", styleId)
        }
    }

    override fun formatSuccessResponse(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): CallToolResult {
        // Return the style data as JSON
        val styleData = pluginResponse?.jsonObject ?: buildJsonObject {}

        val styleName = styleData["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val styleType = styleData["type"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val jsonString = styleData.toJsonString()

        return CallToolResult(
            content = listOf(
                ToolContent.TextContent(text = "Style '$styleName' ($styleType): $jsonString")
            ),
            isError = false
        )
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styleId = params["styleId"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val styleName = pluginResponse?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
        val styleType = pluginResponse?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull

        return if (styleName != null && styleType != null) {
            "Successfully retrieved $styleType style: '$styleName' (ID: $styleId)"
        } else {
            "Style with ID '$styleId' not found"
        }
    }
}
