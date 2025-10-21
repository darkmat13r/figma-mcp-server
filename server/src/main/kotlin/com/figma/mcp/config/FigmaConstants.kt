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

        // Category 3: Styling Tools
        const val SET_FILLS = "figma_set_fills"
        const val SET_STROKES = "figma_set_strokes"
        const val SET_EFFECTS = "figma_set_effects"
        const val SET_OPACITY = "figma_set_opacity"
        const val SET_CORNER_RADIUS = "figma_set_corner_radius"
        const val APPLY_PAINT_STYLE = "figma_apply_paint_style"
        const val APPLY_EFFECT_STYLE = "figma_apply_effect_style"
        const val APPLY_TEXT_STYLE = "figma_apply_text_style"

        // Category 4: Typography Tools
        const val SET_TEXT_CONTENT = "figma_set_text_content"
        const val SET_TEXT_STYLE = "figma_set_text_style"
        const val SET_TEXT_ALIGNMENT = "figma_set_text_alignment"
        const val SET_TEXT_AUTO_RESIZE = "figma_set_text_auto_resize"
        const val SET_TEXT_TRUNCATION = "figma_set_text_truncation"
        const val LOAD_FONT = "figma_load_font"

        // Category 5: Component & Variant Tools
        const val CREATE_COMPONENT_FROM_NODE = "figma_create_component_from_node"
        const val SET_COMPONENT_PROPERTIES = "figma_set_component_properties"
        const val SET_INSTANCE_PROPERTIES = "figma_set_instance_properties"
        const val DETACH_INSTANCE = "figma_detach_instance"
        const val SWAP_INSTANCE = "figma_swap_instance"
        const val CREATE_COMPONENT_SET = "figma_create_component_set"
        const val ADD_VARIANT = "figma_add_variant"

        // Category 6: Variable & Token Tools
        const val CREATE_VARIABLE_COLLECTION = "figma_create_variable_collection"
        const val CREATE_VARIABLE = "figma_create_variable"
        const val BIND_VARIABLE = "figma_bind_variable"
        const val GET_VARIABLES = "figma_get_variables"
        const val SET_VARIABLE_VALUE = "figma_set_variable_value"
        const val UNBIND_VARIABLE = "figma_unbind_variable"

        // Category 7: Hierarchy & Query Tools
        const val GET_NODE_INFO = "figma_get_node_info"
        const val GET_SELECTION = "figma_get_selection"
        const val SET_SELECTION = "figma_set_selection"
        const val FIND_NODES = "figma_find_nodes"
        const val GET_CHILDREN = "figma_get_children"
        const val GET_PARENT = "figma_get_parent"
        const val MOVE_NODE = "figma_move_node"
        const val CLONE_NODE = "figma_clone_node"
        const val GET_CURRENT_PAGE_NODES = "figma_get_current_page_nodes"
        const val SEARCH_NODES = "figma_search_nodes"
        const val GET_ALL_PAGES = "figma_get_all_pages"
        const val SWITCH_PAGE = "figma_switch_page"
        const val CREATE_PAGE = "figma_create_page"

        // Category 9: Image & Media Tools
        const val CREATE_IMAGE = "figma_create_image"
        const val SET_IMAGE_FILL = "figma_set_image_fill"
        const val EXPORT_NODE = "figma_export_node"
        const val GET_IMAGE_FILLS = "figma_get_image_fills"

        // Category 10: Utility Tools
        const val DELETE_NODE = "figma_delete_node"
        const val SHOW_NODE = "figma_show_node"
        const val RENAME_NODE = "figma_rename_node"
        const val SET_VISIBLE = "figma_set_visible"
        const val SET_LOCKED = "figma_set_locked"
        const val NOTIFY = "figma_notify"
        const val GET_USER_INFO = "figma_get_user_info"

        // Legacy/deprecated tools
        const val SET_PROPERTIES = "figma_set_properties"
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
        const val SET_STYLE = "setStyle"
        const val APPLY_STYLE = "applyStyle"
        const val UTILITY_OPERATION = "utilityOperation"

        // Typography methods
        const val SET_TEXT_CONTENT = "setTextContent"
        const val SET_TEXT_STYLE = "setTextStyle"
        const val SET_TEXT_ALIGNMENT = "setTextAlignment"
        const val SET_TEXT_AUTO_RESIZE = "setTextAutoResize"
        const val SET_TEXT_TRUNCATION = "setTextTruncation"
        const val LOAD_FONT = "loadFont"

        // Component & Variant methods
        const val CREATE_COMPONENT_FROM_NODE = "createComponentFromNode"
        const val SET_COMPONENT_PROPERTIES = "setComponentProperties"
        const val SET_INSTANCE_PROPERTIES = "setInstanceProperties"
        const val DETACH_INSTANCE = "detachInstance"
        const val SWAP_INSTANCE = "swapInstance"
        const val CREATE_COMPONENT_SET = "createComponentSet"
        const val ADD_VARIANT = "addVariant"

        // Variable & Token methods
        const val CREATE_VARIABLE_COLLECTION = "createVariableCollection"
        const val CREATE_VARIABLE = "createVariable"
        const val BIND_VARIABLE = "bindVariable"
        const val GET_VARIABLES = "getVariables"
        const val SET_VARIABLE_VALUE = "setVariableValue"
        const val UNBIND_VARIABLE = "unbindVariable"

        // Hierarchy & Query methods
        const val GET_NODE_INFO = "getNodeInfo"
        const val SET_SELECTION = "setSelection"
        const val FIND_NODES = "findNodes"
        const val GET_CHILDREN = "getChildren"
        const val GET_PARENT = "getParent"
        const val MOVE_NODE = "moveNode"
        const val CLONE_NODE = "cloneNode"
        const val GET_CURRENT_PAGE_NODES = "getCurrentPageNodes"
        const val SEARCH_NODES = "searchNodes"
        const val GET_ALL_PAGES = "getAllPages"
        const val SWITCH_PAGE = "switchPage"
        const val CREATE_PAGE = "createPage"

        // Image & Media methods
        const val CREATE_IMAGE = "createImage"
        const val SET_IMAGE_FILL = "setImageFill"
        const val EXPORT_NODE = "exportNode"
        const val GET_IMAGE_FILLS = "getImageFills"

        // User & File Info methods
        const val GET_USER_INFO = "getUserInfo"
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
        const val WS_TIMEOUT_MS = 30000L  // 30 seconds - increased from 5s for complex operations
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
        const val STROKE_ALIGN = "strokeAlign"
        const val STROKE_CAP = "strokeCap"
        const val STROKE_JOIN = "strokeJoin"
        const val CORNER_RADIUS = "cornerRadius"
        const val TOP_LEFT_RADIUS = "topLeftRadius"
        const val TOP_RIGHT_RADIUS = "topRightRadius"
        const val BOTTOM_LEFT_RADIUS = "bottomLeftRadius"
        const val BOTTOM_RIGHT_RADIUS = "bottomRightRadius"
        const val CORNER_SMOOTHING = "cornerSmoothing"
        const val FILL_COLOR = "fillColor"
        const val EFFECTS = "effects"
        const val OPACITY = "opacity"
        const val BLEND_MODE = "blendMode"
        const val STYLE_TYPE = "styleType"
        const val STYLE_ID = "styleId"

        // Layout
        const val LAYOUT_MODE = "layoutMode"

        // Text
        const val TEXT = "text"
        const val FONT_FAMILY = "fontFamily"
        const val FONT_STYLE = "fontStyle"
        const val FONT_SIZE = "fontSize"
        const val TEXT_ALIGN_HORIZONTAL = "textAlignHorizontal"

        // Typography (Category 4)
        const val CHARACTERS = "characters"
        const val FONT_WEIGHT = "fontWeight"
        const val LINE_HEIGHT = "lineHeight"
        const val LETTER_SPACING = "letterSpacing"
        const val TEXT_CASE = "textCase"
        const val TEXT_DECORATION = "textDecoration"
        const val TEXT_ALIGN_VERTICAL = "textAlignVertical"
        const val TEXT_AUTO_RESIZE = "textAutoResize"
        const val TEXT_TRUNCATION = "textTruncation"
        const val MAX_LINES = "maxLines"

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
        const val INSTANCE_ID = "instanceId"
        const val NEW_COMPONENT_ID = "newComponentId"
        const val COMPONENT_IDS = "componentIds"
        const val COMPONENT_SET_ID = "componentSetId"

        // Variables & Tokens
        const val COLLECTION_ID = "collectionId"
        const val MODES = "modes"
        const val VARIABLE_ID = "variableId"
        const val FIELD = "field"
        const val VALUE = "value"
        const val MODE_ID = "modeId"
        const val VALUES = "values"

        // Component Properties
        const val DEFAULT_VALUE = "defaultValue"
        const val VARIANT_OPTIONS = "variantOptions"

        // Groups
        const val NODE_IDS = "nodeIds"

        // Boolean Operations
        const val OPERATION = "operation"

        // Node Info
        const val NODE_ID = "nodeId"
        const val PROPERTIES = "properties"

        // Utility Operations
        const val VISIBLE = "visible"
        const val LOCKED = "locked"
        const val ZOOM = "zoom"
        const val MESSAGE = "message"
        const val TIMEOUT = "timeout"
        const val ERROR = "error"

        // Hierarchy & Query Parameters
        const val TYPE = "type"
        const val PARENT_ID = "parentId"
        const val NEW_PARENT_ID = "newParentId"
        const val INDEX = "index"
        const val DEEP = "deep"
        const val RECURSIVE = "recursive"
        const val SEARCH_TEXT = "searchText"
        const val SEARCH_IN_CURRENT_PAGE_ONLY = "searchInCurrentPageOnly"
        const val PAGE_ID = "pageId"
        const val PAGE_NAME = "pageName"
        const val SWITCH_TO_PAGE = "switchToPage"

        // Image & Media Parameters
        const val IMAGE_DATA = "imageData"
        const val SCALE_MODE = "scaleMode"
        const val ROTATION = "rotation"
        const val FORMAT = "format"
        const val SCALE = "scale"
        const val CONSTRAINT = "constraint"
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
