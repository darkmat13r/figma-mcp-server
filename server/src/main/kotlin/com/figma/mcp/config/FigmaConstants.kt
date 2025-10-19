package com.figma.mcp.config

/**
 * Figma MCP Constants Configuration
 *
 * ## Purpose
 * Centralized location for all constants, magic values, and configuration.
 * Eliminates hardcoded strings and numbers throughout the codebase.
 *
 * ## SOLID Principles
 * - Single Responsibility: Only contains constant definitions
 * - Open-Closed: New constants can be added without modifying existing code
 * - Dependency Inversion: Other modules depend on this abstraction
 */
object FigmaConstants {

    // ========================================================================
    // MCP PROTOCOL CONSTANTS
    // ========================================================================

    object Protocol {
        const val MCP_VERSION = "2024-11-05"
        const val SERVER_NAME = "figma-mcp-server"
        const val SERVER_VERSION = "1.0.0"
    }

    // ========================================================================
    // TOOL NAMES (MCP Tool Registry)
    // ========================================================================

    object ToolNames {
        // Category 1: Node Creation Tools
        const val CREATE_FRAME = "figma_create_frame"
        const val CREATE_COMPONENT = "figma_create_component"
        const val CREATE_INSTANCE = "figma_create_instance"
        const val CREATE_RECTANGLE = "figma_create_rectangle"
        const val CREATE_ELLIPSE = "figma_create_ellipse"
        const val CREATE_TEXT = "figma_create_text"
        const val CREATE_POLYGON = "figma_create_polygon"
        const val CREATE_STAR = "figma_create_star"
        const val CREATE_LINE = "figma_create_line"
        const val CREATE_GROUP = "figma_create_group"
        const val CREATE_SECTION = "figma_create_section"
        const val CREATE_BOOLEAN_OPERATION = "figma_create_boolean_operation"

        // Existing tools
        const val GET_SELECTION = "figma_get_selection"
        const val SET_PROPERTIES = "figma_set_properties"
        const val GET_NODE_INFO = "figma_get_node_info"
    }

    // ========================================================================
    // FIGMA PLUGIN METHOD NAMES (WebSocket Commands)
    // ========================================================================

    object PluginMethods {
        const val CREATE_NODE = "createNode"
        const val GET_INFO = "getInfo"
        const val SET_PROPERTIES = "setProperties"
        const val GET_SELECTION = "getSelection"
        const val GROUP_NODES = "groupNodes"
        const val CREATE_BOOLEAN_OP = "createBooleanOperation"
    }

    // ========================================================================
    // FIGMA NODE TYPES
    // ========================================================================

    object NodeTypes {
        const val FRAME = "FRAME"
        const val COMPONENT = "COMPONENT"
        const val INSTANCE = "INSTANCE"
        const val RECTANGLE = "RECTANGLE"
        const val ELLIPSE = "ELLIPSE"
        const val TEXT = "TEXT"
        const val POLYGON = "POLYGON"
        const val STAR = "STAR"
        const val LINE = "LINE"
        const val GROUP = "GROUP"
        const val SECTION = "SECTION"
        const val BOOLEAN_OPERATION = "BOOLEAN_OPERATION"
    }

    // ========================================================================
    // FIGMA LAYOUT MODES
    // ========================================================================

    object LayoutModes {
        const val NONE = "NONE"
        const val HORIZONTAL = "HORIZONTAL"
        const val VERTICAL = "VERTICAL"
    }

    // ========================================================================
    // FIGMA BOOLEAN OPERATION TYPES
    // ========================================================================

    object BooleanOperations {
        const val UNION = "UNION"
        const val SUBTRACT = "SUBTRACT"
        const val INTERSECT = "INTERSECT"
        const val EXCLUDE = "EXCLUDE"
    }

    // ========================================================================
    // DEFAULT VALUES
    // ========================================================================

    object Defaults {
        // Dimensions
        const val DEFAULT_WIDTH = 100.0
        const val DEFAULT_HEIGHT = 100.0
        const val DEFAULT_POSITION_X = 0.0
        const val DEFAULT_POSITION_Y = 0.0

        // Text
        const val DEFAULT_FONT_SIZE = 16.0
        const val DEFAULT_FONT_FAMILY = "Inter"
        const val DEFAULT_FONT_STYLE = "Regular"

        // Shapes
        const val MIN_POLYGON_SIDES = 3
        const val MAX_POLYGON_SIDES = 100
        const val MIN_STAR_POINTS = 3
        const val MAX_STAR_POINTS = 100

        // WebSocket
        const val WS_TIMEOUT_MS = 5000L
        const val WS_REQUEST_ID_PREFIX = "req_"

        // Constraints
        const val MIN_BOOLEAN_OP_NODES = 2
    }

    // ========================================================================
    // PARAMETER NAMES (for consistency across tools)
    // ========================================================================

    object ParamNames {
        // Common
        const val NAME = "name"
        const val WIDTH = "width"
        const val HEIGHT = "height"
        const val X = "x"
        const val Y = "y"

        // Styling
        const val FILLS = "fills"
        const val STROKES = "strokes"
        const val STROKE_WEIGHT = "strokeWeight"
        const val CORNER_RADIUS = "cornerRadius"
        const val FILL_COLOR = "fillColor"

        // Layout
        const val LAYOUT_MODE = "layoutMode"

        // Text
        const val TEXT = "text"
        const val FONT_FAMILY = "fontFamily"
        const val FONT_STYLE = "fontStyle"
        const val FONT_SIZE = "fontSize"
        const val TEXT_ALIGN_HORIZONTAL = "textAlignHorizontal"

        // Shapes
        const val SIDES = "sides"
        const val RADIUS = "radius"
        const val POINTS = "points"
        const val INNER_RADIUS = "innerRadius"

        // Lines
        const val X1 = "x1"
        const val Y1 = "y1"
        const val X2 = "x2"
        const val Y2 = "y2"

        // Components
        const val COMPONENT_ID = "componentId"
        const val DESCRIPTION = "description"

        // Groups
        const val NODE_IDS = "nodeIds"

        // Boolean Operations
        const val OPERATION = "operation"

        // Node Info
        const val NODE_ID = "nodeId"
        const val PROPERTIES = "properties"
    }

    // ========================================================================
    // ERROR MESSAGES
    // ========================================================================

    object ErrorMessages {
        const val MISSING_REQUIRED_PARAM = "Missing required parameter"
        const val INVALID_PARAM_VALUE = "Invalid parameter value"
        const val NO_FIGMA_CONNECTION = "No Figma plugin connected"
        const val TOOL_EXECUTION_FAILED = "Tool execution failed"
        const val NODE_NOT_FOUND = "Node not found"
        const val INSUFFICIENT_NODES = "Insufficient nodes for operation"

        fun missingParam(paramName: String) = "$MISSING_REQUIRED_PARAM: $paramName"
        fun invalidRange(paramName: String, min: Int, max: Int) =
            "Parameter '$paramName' must be between $min and $max"
        fun minNodesRequired(min: Int) =
            "At least $min nodes required for this operation"
    }

    // ========================================================================
    // SUCCESS MESSAGES
    // ========================================================================

    object SuccessMessages {
        fun nodeCreated(nodeType: String) = "Successfully created $nodeType node"
        fun nodeCreatedWithId(nodeType: String, nodeId: String) =
            "Successfully created $nodeType node with ID: $nodeId"
        fun nodesGrouped(count: Int) = "Successfully grouped $count nodes"
        fun booleanOpCreated(operation: String, count: Int) =
            "Successfully created $operation boolean operation with $count nodes"
    }
}
