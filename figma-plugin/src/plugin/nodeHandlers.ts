/**
 * Figma Node Creation Handlers
 *
 * ## Purpose
 * Handler functions for creating various Figma node types.
 * Each handler is responsible for ONE type of node creation.
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function creates one type of node
 * - Open-Closed: New node types can be added without modifying existing handlers
 * - Dependency Inversion: Functions depend on Figma API abstractions
 */

import { NodeTypes, Defaults, ParamNames, ErrorMessages, BooleanOperations } from './constants';
import { validateAndSanitizeFills } from './styleUtils';

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Convert hex color to RGB
 */
function hexToRgb(hex: string): RGB {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) {
    throw new Error(`Invalid hex color: ${hex}`);
  }
  return {
    r: parseInt(result[1], 16) / 255,
    g: parseInt(result[2], 16) / 255,
    b: parseInt(result[3], 16) / 255,
  };
}

/**
 * Apply common properties to a node
 */
function applyCommonProperties(node: SceneNode, params: Record<string, any>): void {
  // Position
  if (params[ParamNames.X] !== undefined && 'x' in node) {
    (node as LayoutMixin).x = params[ParamNames.X];
  }
  if (params[ParamNames.Y] !== undefined && 'y' in node) {
    (node as LayoutMixin).y = params[ParamNames.Y];
  }

  // Dimensions
  if (params[ParamNames.WIDTH] !== undefined && 'resize' in node) {
    (node as SceneNode & LayoutMixin).resize(params[ParamNames.WIDTH], node.height);
  }
  if (params[ParamNames.HEIGHT] !== undefined && 'resize' in node) {
    (node as SceneNode & LayoutMixin).resize(node.width, params[ParamNames.HEIGHT]);
  }

  // Name
  if (params[ParamNames.NAME]) {
    node.name = params[ParamNames.NAME];
  }

  // Fills (with fillColor shorthand support)
  if (params[ParamNames.FILL_COLOR] && 'fills' in node) {
    const rgb = hexToRgb(params[ParamNames.FILL_COLOR]);
    (node as GeometryMixin).fills = [{ type: 'SOLID', color: rgb }];
  } else if (params[ParamNames.FILLS] && 'fills' in node) {
    // Validate and sanitize fills before applying
    const sanitizedFills = validateAndSanitizeFills(params[ParamNames.FILLS]);
    (node as GeometryMixin).fills = sanitizedFills;
  }

  // Strokes
  if (params[ParamNames.STROKES] && 'strokes' in node) {
    (node as GeometryMixin).strokes = params[ParamNames.STROKES];
  }
  if (params[ParamNames.STROKE_WEIGHT] !== undefined && 'strokeWeight' in node) {
    (node as GeometryMixin).strokeWeight = params[ParamNames.STROKE_WEIGHT];
  }

  // Corner radius (for rectangles/frames)
  if (params[ParamNames.CORNER_RADIUS] !== undefined && 'cornerRadius' in node) {
    (node as RectangleNode | FrameNode).cornerRadius = params[ParamNames.CORNER_RADIUS];
  }
}

/**
 * Add node to page and select it
 */
function addToPage(node: SceneNode): void {
  figma.currentPage.appendChild(node);
  figma.currentPage.selection = [node];
  figma.viewport.scrollAndZoomIntoView([node]);
}

// ============================================================================
// NODE CREATION HANDLERS
// ============================================================================

/**
 * Create Frame node
 */
export async function createFrame(params: Record<string, any>): Promise<FrameNode> {
  const frame = figma.createFrame();

  // Set dimensions with defaults
  frame.resize(
    params[ParamNames.WIDTH] !== undefined ? params[ParamNames.WIDTH] : Defaults.DEFAULT_WIDTH,
    params[ParamNames.HEIGHT] !== undefined ? params[ParamNames.HEIGHT] : Defaults.DEFAULT_HEIGHT
  );

  // Apply common properties
  applyCommonProperties(frame, params);

  // Layout mode
  if (params[ParamNames.LAYOUT_MODE]) {
    frame.layoutMode = params[ParamNames.LAYOUT_MODE];
  }

  addToPage(frame);
  return frame;
}

/**
 * Create Component node
 */
export async function createComponent(params: Record<string, any>): Promise<ComponentNode> {
  const component = figma.createComponent();

  // Set dimensions with defaults
  component.resize(
    params[ParamNames.WIDTH] !== undefined ? params[ParamNames.WIDTH] : Defaults.DEFAULT_WIDTH,
    params[ParamNames.HEIGHT] !== undefined ? params[ParamNames.HEIGHT] : Defaults.DEFAULT_HEIGHT
  );

  // Name is required for components
  if (params[ParamNames.NAME]) {
    component.name = params[ParamNames.NAME];
  }

  // Description
  if (params[ParamNames.DESCRIPTION]) {
    component.description = params[ParamNames.DESCRIPTION];
  }

  applyCommonProperties(component, params);
  addToPage(component);
  return component;
}

/**
 * Create Instance node
 */
export async function createInstance(params: Record<string, any>): Promise<InstanceNode> {
  const componentId = params[ParamNames.COMPONENT_ID];
  if (!componentId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.COMPONENT_ID));
  }

  const component = figma.getNodeById(componentId) as ComponentNode;
  if (!component || component.type !== 'COMPONENT') {
    throw new Error(`Component not found: ${componentId}`);
  }

  const instance = component.createInstance();
  applyCommonProperties(instance, params);
  addToPage(instance);
  return instance;
}

/**
 * Create Rectangle node
 */
export async function createRectangle(params: Record<string, any>): Promise<RectangleNode> {
  const rectangle = figma.createRectangle();

  rectangle.resize(
    params[ParamNames.WIDTH] !== undefined ? params[ParamNames.WIDTH] : Defaults.DEFAULT_WIDTH,
    params[ParamNames.HEIGHT] !== undefined ? params[ParamNames.HEIGHT] : Defaults.DEFAULT_HEIGHT
  );

  applyCommonProperties(rectangle, params);
  addToPage(rectangle);
  return rectangle;
}

/**
 * Create Ellipse node
 */
export async function createEllipse(params: Record<string, any>): Promise<EllipseNode> {
  const ellipse = figma.createEllipse();

  ellipse.resize(
    params[ParamNames.WIDTH] !== undefined ? params[ParamNames.WIDTH] : Defaults.DEFAULT_WIDTH,
    params[ParamNames.HEIGHT] !== undefined ? params[ParamNames.HEIGHT] : Defaults.DEFAULT_HEIGHT
  );

  applyCommonProperties(ellipse, params);
  addToPage(ellipse);
  return ellipse;
}

/**
 * Create Text node
 */
export async function createText(params: Record<string, any>): Promise<TextNode> {
  const textNode = figma.createText();

  // Load font
  const fontFamily = params[ParamNames.FONT_FAMILY] !== undefined ? params[ParamNames.FONT_FAMILY] : Defaults.DEFAULT_FONT_FAMILY;
  const fontStyle = params[ParamNames.FONT_STYLE] !== undefined ? params[ParamNames.FONT_STYLE] : Defaults.DEFAULT_FONT_STYLE;

  try {
    await figma.loadFontAsync({ family: fontFamily, style: fontStyle });
    textNode.fontName = { family: fontFamily, style: fontStyle };
  } catch (error) {
    console.warn(`Failed to load font ${fontFamily} ${fontStyle}, using default`);
    await figma.loadFontAsync({ family: Defaults.DEFAULT_FONT_FAMILY, style: Defaults.DEFAULT_FONT_STYLE });
    textNode.fontName = { family: Defaults.DEFAULT_FONT_FAMILY, style: Defaults.DEFAULT_FONT_STYLE };
  }

  // Set text content
  if (params[ParamNames.TEXT]) {
    textNode.characters = params[ParamNames.TEXT];
  }

  // Font size
  if (params[ParamNames.FONT_SIZE]) {
    textNode.fontSize = params[ParamNames.FONT_SIZE];
  }

  // Text alignment
  if (params[ParamNames.TEXT_ALIGN_HORIZONTAL]) {
    textNode.textAlignHorizontal = params[ParamNames.TEXT_ALIGN_HORIZONTAL];
  }

  applyCommonProperties(textNode, params);
  addToPage(textNode);
  return textNode;
}

/**
 * Create Polygon node
 */
export async function createPolygon(params: Record<string, any>): Promise<PolygonNode> {
  const sides = params[ParamNames.SIDES];
  if (!sides) {
    throw new Error(ErrorMessages.missingParam(ParamNames.SIDES));
  }

  const polygon = figma.createPolygon();
  polygon.pointCount = sides;

  const radius = params[ParamNames.RADIUS] !== undefined ? params[ParamNames.RADIUS] : Defaults.DEFAULT_WIDTH / 2;
  polygon.resize(radius * 2, radius * 2);

  applyCommonProperties(polygon, params);
  addToPage(polygon);
  return polygon;
}

/**
 * Create Star node
 */
export async function createStar(params: Record<string, any>): Promise<StarNode> {
  const points = params[ParamNames.POINTS];
  if (!points) {
    throw new Error(ErrorMessages.missingParam(ParamNames.POINTS));
  }

  const star = figma.createStar();
  star.pointCount = points;

  const radius = params[ParamNames.RADIUS] !== undefined ? params[ParamNames.RADIUS] : Defaults.DEFAULT_WIDTH / 2;
  star.resize(radius * 2, radius * 2);

  // Inner radius (controls star sharpness)
  if (params[ParamNames.INNER_RADIUS] !== undefined) {
    star.innerRadius = params[ParamNames.INNER_RADIUS] / radius;
  }

  applyCommonProperties(star, params);
  addToPage(star);
  return star;
}

/**
 * Create Line node
 */
export async function createLine(params: Record<string, any>): Promise<LineNode> {
  const line = figma.createLine();

  const x1 = params[ParamNames.X1] !== undefined ? params[ParamNames.X1] : 0;
  const y1 = params[ParamNames.Y1] !== undefined ? params[ParamNames.Y1] : 0;
  const x2 = params[ParamNames.X2];
  const y2 = params[ParamNames.Y2];

  if (x2 === undefined || y2 === undefined) {
    throw new Error('Line requires x1, y1, x2, y2 parameters');
  }

  // Position line at start point
  line.x = x1;
  line.y = y1;

  // Resize line to create endpoint
  line.resize(Math.abs(x2 - x1), 0);
  line.rotation = Math.atan2(y2 - y1, x2 - x1) * (180 / Math.PI);

  applyCommonProperties(line, params);
  addToPage(line);
  return line;
}

/**
 * Create Group from nodes
 */
export async function createGroup(params: Record<string, any>): Promise<GroupNode> {
  const nodeIds = params[ParamNames.NODE_IDS];
  if (!nodeIds || nodeIds.length === 0) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_IDS));
  }

  const nodes = nodeIds
    .map((id: string) => figma.getNodeById(id))
    .filter((node: BaseNode | null): node is SceneNode =>
      node !== null && 'parent' in node
    );

  if (nodes.length === 0) {
    throw new Error('No valid nodes found to group');
  }

  const group = figma.group(nodes, figma.currentPage);

  if (params[ParamNames.NAME]) {
    group.name = params[ParamNames.NAME];
  }

  figma.currentPage.selection = [group];
  figma.viewport.scrollAndZoomIntoView([group]);

  return group;
}

/**
 * Create Section node
 */
export async function createSection(params: Record<string, any>): Promise<SectionNode> {
  const section = figma.createSection();

  // Note: SectionNode doesn't have resize method, dimensions are auto-calculated
  // We can only set the name for sections
  if (params[ParamNames.NAME]) {
    section.name = params[ParamNames.NAME];
  }

  addToPage(section);
  return section;
}

/**
 * Create Boolean Operation
 */
export async function createBooleanOperation(params: Record<string, any>): Promise<BooleanOperationNode> {
  const operation = params[ParamNames.OPERATION];
  const nodeIds = params[ParamNames.NODE_IDS];

  if (!operation) {
    throw new Error(ErrorMessages.missingParam(ParamNames.OPERATION));
  }
  if (!nodeIds || nodeIds.length < Defaults.MIN_BOOLEAN_OP_NODES) {
    throw new Error(ErrorMessages.minNodesRequired(Defaults.MIN_BOOLEAN_OP_NODES));
  }

  // Validate operation type
  const validOps = Object.values(BooleanOperations);
  if (!validOps.includes(operation)) {
    throw new Error(ErrorMessages.INVALID_BOOLEAN_OP);
  }

  const nodes = nodeIds
    .map((id: string) => figma.getNodeById(id))
    .filter((node: BaseNode | null): node is SceneNode =>
      node !== null && 'parent' in node
    );

  if (nodes.length < Defaults.MIN_BOOLEAN_OP_NODES) {
    throw new Error(ErrorMessages.minNodesRequired(Defaults.MIN_BOOLEAN_OP_NODES));
  }

  const booleanOp = figma.union(nodes, figma.currentPage);
  booleanOp.booleanOperation = operation;

  figma.currentPage.selection = [booleanOp];
  figma.viewport.scrollAndZoomIntoView([booleanOp]);

  return booleanOp;
}
