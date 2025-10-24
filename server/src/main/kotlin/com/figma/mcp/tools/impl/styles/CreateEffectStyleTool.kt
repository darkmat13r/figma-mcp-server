package com.figma.mcp.tools.impl.styles

import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Create Effect Style Tool
 *
 * ## Purpose
 * Creates a new local effect style in the current Figma document.
 * Effect styles can include drop shadows, inner shadows, layer blur, and background blur.
 *
 * ## Parameters
 * - name: string (optional) - The name for the new style (default: "Effect Style")
 * - description: string (optional) - Description for the style
 * - effects: Effect[] (optional) - Initial effect values for the style
 *
 * ## Returns
 * - styleId: The ID of the newly created style
 * - name: The name of the style
 */
class CreateEffectStyleTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, "figma_create_effect_style") {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a new local effect style in the current Figma document. " +
                    "Effect styles can include drop shadows, inner shadows, layer blur, and background blur. " +
                    "The newly created style is local to the current document. " +
                    "Useful for creating reusable shadow and blur effects for design systems.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "Name for the new effect style (optional, default: 'Effect Style')"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "Description for the style (optional)"
                    ),
                    "effects" to mapOf(
                        "type" to "array",
                        "description" to "Array of Effect objects to set as initial value (optional)"
                    )
                ),
                required = listOf()
            )
        )
    }

    override fun getPluginMethod(): String {
        return "createEffectStyle"
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            params.getStringOrNull("name")?.let { put("name", it) }
            params.getStringOrNull("description")?.let { put("description", it) }
            params.getArrayOrNull("effects")?.let { put("effects", it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val styleId = pluginResponse?.jsonObject?.get("styleId")?.jsonPrimitive?.contentOrNull
        val styleName = pluginResponse?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: params["name"]?.jsonPrimitive?.contentOrNull
            ?: "Effect Style"

        return if (styleId != null) {
            "Successfully created effect style '$styleName' (ID: $styleId)"
        } else {
            "Successfully created effect style '$styleName'"
        }
    }
}
