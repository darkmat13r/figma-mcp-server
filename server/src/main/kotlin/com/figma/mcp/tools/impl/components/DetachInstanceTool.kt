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
 * Detach Instance Tool
 *
 * ## Purpose
 * Breaks the link between an instance and its master component.
 *
 * ## Parameters
 * - instanceId (required): ID of the instance to detach
 *
 * ## Returns
 * Success message
 */
class DetachInstanceTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.DETACH_INSTANCE) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Break the link between an instance and its master component. " +
                    "After detaching, the instance becomes a regular frame and no longer receives updates from the master.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.INSTANCE_ID to mapOf(
                        "type" to "string",
                        "description" to "ID of the instance to detach (required)"
                    )
                ),
                required = listOf(ParamNames.INSTANCE_ID)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.DETACH_INSTANCE
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put(ParamNames.INSTANCE_ID, params.getRequiredString(ParamNames.INSTANCE_ID))
        }
    }

    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        return "Successfully detached instance from component"
    }
}
