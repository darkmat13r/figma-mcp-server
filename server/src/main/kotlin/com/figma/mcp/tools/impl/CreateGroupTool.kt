package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants
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
 * Create Group Tool
 *
 * ## Purpose
 * Creates a group from selected nodes, combining them into a single parent.
 * Groups are useful for organizing multiple nodes while maintaining their positions.
 *
 * ## Parameters
 * - nodeIds: string[] (required) - Array of node IDs to group together
 * - name: string (optional) - Name for the group
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
 */
class CreateGroupTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_GROUP) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a group from multiple selected nodes in Figma. " +
                    "Groups combine nodes under a single parent while maintaining their relative positions. " +
                    "Useful for organizing layers and moving multiple elements together.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.NODE_IDS to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "Array of node IDs to group together (required, minimum 1 node)"
                    ),
                    ParamNames.NAME to mapOf(
                        "type" to "string",
                        "description" to "Name for the group (optional)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to this node (optional). Format: \"S:\" followed by hex string (e.g., \"S:c5dea36132bf5a4ec0ef125f93e21c0ca0073976,\")"
                    )
                ),
                required = listOf(ParamNames.NODE_IDS)
            )
        )
    }

    override fun validate(arguments: JsonObject): String? {
        // Validate nodeIds is an array with at least 1 element
        val nodeIds = arguments[ParamNames.NODE_IDS]?.jsonArray
            ?: return FigmaConstants.ErrorMessages.missingParam(ParamNames.NODE_IDS)

        if (nodeIds.isEmpty()) {
            return "At least 1 node is required to create a group"
        }

        return null // Valid
    }

    override fun getPluginMethod(): String {
        return PluginMethods.GROUP_NODES
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val nodeIds = params.getRequiredStringArray(ParamNames.NODE_IDS)

        return buildJsonObject {
            put(ParamNames.NODE_IDS, params.getRequiredArray(ParamNames.NODE_IDS))
            params.getStringOrNull(ParamNames.NAME)?.let { put(ParamNames.NAME, it) }
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val nodeIds = params[ParamNames.NODE_IDS]?.jsonArray
        val count = nodeIds?.size ?: 0
        val groupId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        return if (groupId != null) {
            FigmaConstants.SuccessMessages.nodesGrouped(count) + " (ID: $groupId)"
        } else {
            FigmaConstants.SuccessMessages.nodesGrouped(count)
        }
    }
}
