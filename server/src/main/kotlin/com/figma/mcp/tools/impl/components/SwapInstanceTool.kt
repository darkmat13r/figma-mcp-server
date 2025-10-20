package com.figma.mcp.tools.impl.components

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
 * Swap Instance Tool
 *
 * ## Purpose
 * Swaps an instance to point to a different component master.
 *
 * ## Parameters
 * - instanceId (required): ID of the instance to swap
 * - newComponentId (required): ID of the new component to swap to
 *
 * ## Returns
 * Success message
 */
class SwapInstanceTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.SWAP_INSTANCE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Swap an instance to use a different component master. " +
                    "The instance will now be linked to the new component and receive updates from it.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.INSTANCE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the instance to swap (required)"
                    ),
                    ParamNames.NEW_COMPONENT_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the component to swap to (required)"
                    )
                ),
                required = listOf(ParamNames.INSTANCE_ID, ParamNames.NEW_COMPONENT_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.SWAP_INSTANCE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.INSTANCE_ID, params.getRequiredString(ParamNames.INSTANCE_ID))
            put(ParamNames.NEW_COMPONENT_ID, params.getRequiredString(ParamNames.NEW_COMPONENT_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        return "Successfully swapped instance to new component"
    }
}
