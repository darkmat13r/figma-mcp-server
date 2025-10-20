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

  // Text
  TEXT: 'text',
  FONT_FAMILY: 'fontFamily',
  FONT_STYLE: 'fontStyle',
  FONT_SIZE: 'fontSize',
  TEXT_ALIGN_HORIZONTAL: 'textAlignHorizontal',

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

  // Groups
  NODE_IDS: 'nodeIds',

  // Boolean Operations
  OPERATION: 'operation',

  // Node Info
  NODE_ID: 'nodeId',
  PROPERTIES: 'properties',
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
