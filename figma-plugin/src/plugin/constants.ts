/**
 * Figma Plugin Constants
 *
 * ## Purpose
 * Centralized constants to eliminate magic strings and numbers.
 * Mirrors the server-side FigmaConstants.kt for consistency.
 *
 * ## SOLID Principles
 * - Single Responsibility: Only contains constant definitions
 * - Open-Closed: New constants can be added without modifying existing code
 */

// ============================================================================
// FIGMA NODE TYPES
// ============================================================================

export const NodeTypes = {
  FRAME: 'FRAME',
  COMPONENT: 'COMPONENT',
  INSTANCE: 'INSTANCE',
  RECTANGLE: 'RECTANGLE',
  ELLIPSE: 'ELLIPSE',
  TEXT: 'TEXT',
  POLYGON: 'POLYGON',
  STAR: 'STAR',
  LINE: 'LINE',
  GROUP: 'GROUP',
  SECTION: 'SECTION',
  BOOLEAN_OPERATION: 'BOOLEAN_OPERATION',
} as const;

// ============================================================================
// PLUGIN METHODS (WebSocket Commands)
// ============================================================================

export const PluginMethods = {
  CREATE_NODE: 'createNode',
  GET_INFO: 'getInfo',
  SET_PROPERTIES: 'setProperties',
  GET_SELECTION: 'getSelection',
  GROUP_NODES: 'groupNodes',
  CREATE_BOOLEAN_OP: 'createBooleanOperation',
  SET_STYLE: 'setStyle',
  APPLY_STYLE: 'applyStyle',
  UTILITY_OPERATION: 'utilityOperation',
  // Typography methods
  SET_TEXT_CONTENT: 'setTextContent',
  SET_TEXT_STYLE: 'setTextStyle',
  SET_TEXT_ALIGNMENT: 'setTextAlignment',
  SET_TEXT_AUTO_RESIZE: 'setTextAutoResize',
  SET_TEXT_TRUNCATION: 'setTextTruncation',
  LOAD_FONT: 'loadFont',
  // Hierarchy & Query methods
  GET_NODE_INFO: 'getNodeInfo',
  SET_SELECTION: 'setSelection',
  FIND_NODES: 'findNodes',
  GET_CHILDREN: 'getChildren',
  GET_PARENT: 'getParent',
  MOVE_NODE: 'moveNode',
  CLONE_NODE: 'cloneNode',
  GET_CURRENT_PAGE_NODES: 'getCurrentPageNodes',
  SEARCH_NODES: 'searchNodes',
  GET_ALL_PAGES: 'getAllPages',
  SWITCH_PAGE: 'switchPage',
  CREATE_PAGE: 'createPage',
  // Component & Variant methods
  CREATE_COMPONENT_FROM_NODE: 'createComponentFromNode',
  SET_COMPONENT_PROPERTIES: 'setComponentProperties',
  SET_INSTANCE_PROPERTIES: 'setInstanceProperties',
  DETACH_INSTANCE: 'detachInstance',
  SWAP_INSTANCE: 'swapInstance',
  CREATE_COMPONENT_SET: 'createComponentSet',
  ADD_VARIANT: 'addVariant',
  // Variable & Token methods
  CREATE_VARIABLE_COLLECTION: 'createVariableCollection',
  CREATE_VARIABLE: 'createVariable',
  BIND_VARIABLE: 'bindVariable',
  GET_VARIABLES: 'getVariables',
  SET_VARIABLE_VALUE: 'setVariableValue',
  UNBIND_VARIABLE: 'unbindVariable',
  // Image & Media methods
  CREATE_IMAGE: 'createImage',
  SET_IMAGE_FILL: 'setImageFill',
  EXPORT_NODE: 'exportNode',
  GET_IMAGE_FILLS: 'getImageFills',
  // User & File Info methods
  GET_USER_INFO: 'getUserInfo',
  // Style Management methods (Category 11)
  GET_STYLE_BY_ID: 'getStyleById',
  GET_LOCAL_PAINT_STYLES: 'getLocalPaintStyles',
  GET_LOCAL_TEXT_STYLES: 'getLocalTextStyles',
  GET_LOCAL_EFFECT_STYLES: 'getLocalEffectStyles',
  GET_LOCAL_GRID_STYLES: 'getLocalGridStyles',
  CREATE_PAINT_STYLE: 'createPaintStyle',
  CREATE_TEXT_STYLE: 'createTextStyle',
  CREATE_EFFECT_STYLE: 'createEffectStyle',
  CREATE_GRID_STYLE: 'createGridStyle',
  SET_FILL_STYLE_ID: 'setFillStyleId',
} as const;

// ============================================================================
// LAYOUT MODES
// ============================================================================

export const LayoutModes = {
  NONE: 'NONE',
  HORIZONTAL: 'HORIZONTAL',
  VERTICAL: 'VERTICAL',
} as const;

// ============================================================================
// BOOLEAN OPERATIONS
// ============================================================================

export const BooleanOperations = {
  UNION: 'UNION',
  SUBTRACT: 'SUBTRACT',
  INTERSECT: 'INTERSECT',
  EXCLUDE: 'EXCLUDE',
} as const;

// ============================================================================
// DEFAULT VALUES
// ============================================================================

export const Defaults = {
  // Dimensions
  DEFAULT_WIDTH: 100,
  DEFAULT_HEIGHT: 100,
  DEFAULT_POSITION_X: 0,
  DEFAULT_POSITION_Y: 0,

  // Text
  DEFAULT_FONT_SIZE: 16,
  DEFAULT_FONT_FAMILY: 'Inter',
  DEFAULT_FONT_STYLE: 'Regular',

  // Shapes
  MIN_POLYGON_SIDES: 3,
  MAX_POLYGON_SIDES: 100,
  MIN_STAR_POINTS: 3,
  MAX_STAR_POINTS: 100,

  // Constraints
  MIN_BOOLEAN_OP_NODES: 2,
} as const;

// ============================================================================
// CONNECTION CONFIGURATION
// ============================================================================

export const ConnectionConfig = {
  // WebSocket endpoints
  WS_BASE_URL: 'ws://localhost:8081',
  WS_FIGMA_PATH: '/figma',

  // SSE endpoints (for Claude Code)
  SSE_BASE_URL: 'http://localhost:1234',
  SSE_PATH: '/sse',

  // Build WebSocket URL with fileId
  buildWebSocketUrl: (fileId: string) => {
    return `${ConnectionConfig.WS_BASE_URL}${ConnectionConfig.WS_FIGMA_PATH}?fileId=${encodeURIComponent(fileId)}`;
  },

  // Build SSE URL with fileId (for Claude Code configuration)
  buildSseUrl: (fileId: string) => {
    return `${ConnectionConfig.SSE_BASE_URL}${ConnectionConfig.SSE_PATH}?fileId=${encodeURIComponent(fileId)}`;
  },
} as const;

// ============================================================================
// PARAMETER NAMES
// ============================================================================

export const ParamNames = {
  // Common
  NAME: 'name',
  WIDTH: 'width',
  HEIGHT: 'height',
  X: 'x',
  Y: 'y',
  TYPE: 'type',

  // Styling
  FILLS: 'fills',
  STROKES: 'strokes',
  STROKE_WEIGHT: 'strokeWeight',
  STROKE_ALIGN: 'strokeAlign',
  STROKE_CAP: 'strokeCap',
  STROKE_JOIN: 'strokeJoin',
  CORNER_RADIUS: 'cornerRadius',
  TOP_LEFT_RADIUS: 'topLeftRadius',
  TOP_RIGHT_RADIUS: 'topRightRadius',
  BOTTOM_LEFT_RADIUS: 'bottomLeftRadius',
  BOTTOM_RIGHT_RADIUS: 'bottomRightRadius',
  CORNER_SMOOTHING: 'cornerSmoothing',
  FILL_COLOR: 'fillColor',
  EFFECTS: 'effects',
  OPACITY: 'opacity',
  BLEND_MODE: 'blendMode',
  STYLE_TYPE: 'styleType',
  STYLE_ID: 'styleId',

  // Layout
  LAYOUT_MODE: 'layoutMode',
  PADDING_LEFT: 'paddingLeft',
  PADDING_RIGHT: 'paddingRight',
  PADDING_TOP: 'paddingTop',
  PADDING_BOTTOM: 'paddingBottom',
  ITEM_SPACING: 'itemSpacing',
  PRIMARY_AXIS_ALIGN_ITEMS: 'primaryAxisAlignItems',
  COUNTER_AXIS_ALIGN_ITEMS: 'counterAxisAlignItems',
  PRIMARY_AXIS_SIZING_MODE: 'primaryAxisSizingMode',
  COUNTER_AXIS_SIZING_MODE: 'counterAxisSizingMode',

  // Text
  TEXT: 'text',
  CHARACTERS: 'characters',
  FONT_FAMILY: 'fontFamily',
  FONT_STYLE: 'fontStyle',
  FONT_SIZE: 'fontSize',
  FONT_WEIGHT: 'fontWeight',
  LINE_HEIGHT: 'lineHeight',
  LETTER_SPACING: 'letterSpacing',
  TEXT_CASE: 'textCase',
  TEXT_DECORATION: 'textDecoration',
  TEXT_ALIGN_HORIZONTAL: 'textAlignHorizontal',
  TEXT_ALIGN_VERTICAL: 'textAlignVertical',
  TEXT_AUTO_RESIZE: 'textAutoResize',
  TEXT_TRUNCATION: 'textTruncation',
  MAX_LINES: 'maxLines',

  // Shapes
  SIDES: 'sides',
  RADIUS: 'radius',
  POINTS: 'points',
  INNER_RADIUS: 'innerRadius',

  // Lines
  X1: 'x1',
  Y1: 'y1',
  X2: 'x2',
  Y2: 'y2',

  // Components
  COMPONENT_ID: 'componentId',
  DESCRIPTION: 'description',
  INSTANCE_ID: 'instanceId',
  NEW_COMPONENT_ID: 'newComponentId',
  COMPONENT_IDS: 'componentIds',
  COMPONENT_SET_ID: 'componentSetId',

  // Groups
  NODE_IDS: 'nodeIds',

  // Boolean Operations
  OPERATION: 'operation',

  // Node Info
  NODE_ID: 'nodeId',
  PROPERTIES: 'properties',

  // Utility Operations
  VISIBLE: 'visible',
  LOCKED: 'locked',
  ZOOM: 'zoom',
  MESSAGE: 'message',
  TIMEOUT: 'timeout',
  ERROR: 'error',

  // Hierarchy & Query Operations
  NAME_REGEX: 'nameRegex',
  TYPES: 'types',
  PARENT_ID: 'parentId',
  NEW_PARENT_ID: 'newParentId',
  RECURSIVE: 'recursive',
  INDEX: 'index',
  DEEP: 'deep',
  QUERY: 'query',
  SEARCH_IN_TEXT: 'searchInText',
  SEARCH_IN_NAMES: 'searchInNames',
  CURRENT_PAGE_ONLY: 'currentPageOnly',
  PAGE_ID: 'pageId',
  SWITCH_TO: 'switchTo',

  // Variables & Tokens
  COLLECTION_ID: 'collectionId',
  MODES: 'modes',
  VARIABLE_ID: 'variableId',
  FIELD: 'field',
  VALUE: 'value',
  MODE_ID: 'modeId',
  VALUES: 'values',
  DEFAULT_VALUE: 'defaultValue',
  VARIANT_OPTIONS: 'variantOptions',

  // Image & Media
  IMAGE_DATA: 'imageData',
  SCALE_MODE: 'scaleMode',
  FORMAT: 'format',
  SCALE: 'scale',
  CONSTRAINT: 'constraint',
  ROTATION: 'rotation',
} as const;

// ============================================================================
// ERROR MESSAGES
// ============================================================================

export const ErrorMessages = {
  MISSING_REQUIRED_PARAM: 'Missing required parameter',
  INVALID_PARAM_VALUE: 'Invalid parameter value',
  NODE_NOT_FOUND: 'Node not found',
  INSUFFICIENT_NODES: 'Insufficient nodes for operation',
  INVALID_BOOLEAN_OP: 'Invalid boolean operation type',
  FONT_LOAD_FAILED: 'Failed to load font',

  missingParam: (paramName: string) =>
    `${ErrorMessages.MISSING_REQUIRED_PARAM}: ${paramName}`,

  invalidRange: (paramName: string, min: number, max: number) =>
    `Parameter '${paramName}' must be between ${min} and ${max}`,

  minNodesRequired: (min: number) =>
    `At least ${min} nodes required for this operation`,
} as const;

// ============================================================================
// SUCCESS MESSAGES
// ============================================================================

export const SuccessMessages = {
  nodeCreated: (nodeType: string) => `Successfully created ${nodeType} node`,

  nodeCreatedWithId: (nodeType: string, nodeId: string) =>
    `Successfully created ${nodeType} node with ID: ${nodeId}`,

  nodesGrouped: (count: number) => `Successfully grouped ${count} nodes`,

  booleanOpCreated: (operation: string, count: number) =>
    `Successfully created ${operation} boolean operation with ${count} nodes`,
} as const;
