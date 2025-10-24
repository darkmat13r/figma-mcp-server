package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants
import com.figma.mcp.config.FigmaConstants.BooleanOperations
import com.figma.mcp.config.FigmaConstants.Defaults
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
 * Create Boolean Operation Tool
 *
 * ## Purpose
 * Creates boolean operations combining multiple shapes (union, subtract, intersect, exclude).
 * Essential for creating complex shapes and icons.
 *
 * ## Parameters
 * - operation: "UNION" | "SUBTRACT" | "INTERSECT" | "EXCLUDE" (required)
 * - nodeIds: string[] (required, minimum 2) - IDs of nodes to combine
 * - fillStyleId: string (optional) - Fill/paint style ID to apply
 * - strokeStyleId: string (optional) - Stroke style ID to apply
 * - effectStyleId: string (optional) - Effect style ID to apply
 *
 * ## Boolean Operations Explained
 * - UNION: Combines shapes (A + B)
 * - SUBTRACT: Cuts out second shape from first (A - B)
 * - INTERSECT: Only keeps overlapping area (A ∩ B)
 * - EXCLUDE: Keeps non-overlapping areas only (A ⊕ B)
 */
class CreateBooleanOperationTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_BOOLEAN_OPERATION) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a boolean operation combining multiple shapes in Figma. " +
                    "Boolean operations are essential for creating complex shapes, icons, and illustrations. " +
                    "UNION combines shapes, SUBTRACT cuts out, INTERSECT keeps only overlaps, " +
                    "and EXCLUDE keeps only non-overlapping areas.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.OPERATION to mapOf(
                        "type" to "string",
                        "enum" to listOf(
                            BooleanOperations.UNION,
                            BooleanOperations.SUBTRACT,
                            BooleanOperations.INTERSECT,
                            BooleanOperations.EXCLUDE
                        ),
                        "description" to "Boolean operation type (required). " +
                                "UNION: combine shapes (A+B), " +
                                "SUBTRACT: cut out (A-B), " +
                                "INTERSECT: overlap only (A∩B), " +
                                "EXCLUDE: non-overlapping (A⊕B)"
                    ),
                    ParamNames.NODE_IDS to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "Array of node IDs to combine (required, minimum ${Defaults.MIN_BOOLEAN_OP_NODES} nodes)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill/paint style ID to apply to the boolean operation result (optional)"
                    ),
                    ParamNames.STROKE_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Stroke style ID to apply to the boolean operation result (optional)"
                    ),
                    ParamNames.EFFECT_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Effect style ID to apply to the boolean operation result (optional)"
                    )
                ),
                required = listOf(ParamNames.OPERATION, ParamNames.NODE_IDS)
            )
        )
    }

    override fun validate(arguments: JsonObject): String? {
        // Validate operation is valid
        val operation = arguments[ParamNames.OPERATION]?.jsonPrimitive?.contentOrNull
            ?: return FigmaConstants.ErrorMessages.missingParam(ParamNames.OPERATION)

        val validOperations = listOf(
            BooleanOperations.UNION,
            BooleanOperations.SUBTRACT,
            BooleanOperations.INTERSECT,
            BooleanOperations.EXCLUDE
        )

        if (operation !in validOperations) {
            return "Invalid operation. Must be one of: ${validOperations.joinToString(", ")}"
        }

        // Validate nodeIds is an array with at least 2 elements
        val nodeIds = arguments[ParamNames.NODE_IDS]?.jsonArray
            ?: return FigmaConstants.ErrorMessages.missingParam(ParamNames.NODE_IDS)

        if (nodeIds.size < Defaults.MIN_BOOLEAN_OP_NODES) {
            return FigmaConstants.ErrorMessages.minNodesRequired(Defaults.MIN_BOOLEAN_OP_NODES)
        }

        return null // Valid
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_BOOLEAN_OP
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.OPERATION, params.getRequiredString(ParamNames.OPERATION))
            put(ParamNames.NODE_IDS, params.getRequiredArray(ParamNames.NODE_IDS))
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { put(ParamNames.FILL_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.STROKE_STYLE_ID)?.let { put(ParamNames.STROKE_STYLE_ID, it) }
            params.getStringOrNull(ParamNames.EFFECT_STYLE_ID)?.let { put(ParamNames.EFFECT_STYLE_ID, it) }
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        val operation = params[ParamNames.OPERATION]?.jsonPrimitive?.content ?: "UNKNOWN"
        val nodeIds = params[ParamNames.NODE_IDS]?.jsonArray
        val count = nodeIds?.size ?: 0
        val booleanOpId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull

        return if (booleanOpId != null) {
            FigmaConstants.SuccessMessages.booleanOpCreated(operation, count) + " (ID: $booleanOpId)"
        } else {
            FigmaConstants.SuccessMessages.booleanOpCreated(operation, count)
        }
    }
}
