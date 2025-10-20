/**
 * Figma Plugin Style Handlers
 *
 * ## Purpose
 * Handles all styling operations for Category 3: Styling Tools
 * - setStyle: Apply fills, strokes, effects, opacity, corner radius
 * - applyStyle: Apply existing paint/effect/text styles
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific styling operation
 * - Open-Closed: New style operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Get node by ID with type checking
 */
function getNode(nodeId: string): SceneNode {
  const node = figma.getNodeById(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }
  return node as SceneNode;
}

/**
 * Check if node supports fills
 */
function supportsFills(node: SceneNode): boolean {
  return 'fills' in node;
}

/**
 * Check if node supports strokes
 */
function supportsStrokes(node: SceneNode): boolean {
  return 'strokes' in node;
}

/**
 * Check if node supports effects
 */
function supportsEffects(node: SceneNode): boolean {
  return 'effects' in node;
}

/**
 * Check if node supports opacity
 */
function supportsOpacity(node: SceneNode): boolean {
  return 'opacity' in node;
}

/**
 * Check if node supports corner radius
 */
function supportsCornerRadius(node: SceneNode): boolean {
  return 'cornerRadius' in node;
}

// ============================================================================
// STYLE APPLICATION HANDLERS
// ============================================================================

/**
 * Handle setStyle command
 * Applies various styling properties based on the parameters provided
 */
export async function handleSetStyle(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = getNode(nodeId);

  // Apply fills
  if (params[ParamNames.FILLS] !== undefined && supportsFills(node)) {
    (node as any).fills = params[ParamNames.FILLS];
  }

  // Apply strokes
  if (params[ParamNames.STROKES] !== undefined && supportsStrokes(node)) {
    (node as any).strokes = params[ParamNames.STROKES];

    // Optional stroke properties
    if (params[ParamNames.STROKE_WEIGHT] !== undefined) {
      (node as any).strokeWeight = params[ParamNames.STROKE_WEIGHT];
    }
    if (params[ParamNames.STROKE_ALIGN] !== undefined) {
      (node as any).strokeAlign = params[ParamNames.STROKE_ALIGN];
    }
    if (params[ParamNames.STROKE_CAP] !== undefined) {
      (node as any).strokeCap = params[ParamNames.STROKE_CAP];
    }
    if (params[ParamNames.STROKE_JOIN] !== undefined) {
      (node as any).strokeJoin = params[ParamNames.STROKE_JOIN];
    }
  }

  // Apply effects
  if (params[ParamNames.EFFECTS] !== undefined && supportsEffects(node)) {
    (node as any).effects = params[ParamNames.EFFECTS];
  }

  // Apply opacity
  if (params[ParamNames.OPACITY] !== undefined && supportsOpacity(node)) {
    (node as any).opacity = params[ParamNames.OPACITY];
  }

  // Apply blend mode
  if (params[ParamNames.BLEND_MODE] !== undefined && supportsOpacity(node)) {
    (node as any).blendMode = params[ParamNames.BLEND_MODE];
  }

  // Apply corner radius
  if (supportsCornerRadius(node)) {
    // Uniform corner radius
    if (params[ParamNames.CORNER_RADIUS] !== undefined) {
      (node as any).cornerRadius = params[ParamNames.CORNER_RADIUS];
    }

    // Individual corner radii (if node supports it)
    if ('topLeftRadius' in node) {
      if (params[ParamNames.TOP_LEFT_RADIUS] !== undefined) {
        (node as any).topLeftRadius = params[ParamNames.TOP_LEFT_RADIUS];
      }
      if (params[ParamNames.TOP_RIGHT_RADIUS] !== undefined) {
        (node as any).topRightRadius = params[ParamNames.TOP_RIGHT_RADIUS];
      }
      if (params[ParamNames.BOTTOM_LEFT_RADIUS] !== undefined) {
        (node as any).bottomLeftRadius = params[ParamNames.BOTTOM_LEFT_RADIUS];
      }
      if (params[ParamNames.BOTTOM_RIGHT_RADIUS] !== undefined) {
        (node as any).bottomRightRadius = params[ParamNames.BOTTOM_RIGHT_RADIUS];
      }
    }

    // Corner smoothing
    if (params[ParamNames.CORNER_SMOOTHING] !== undefined && 'cornerSmoothing' in node) {
      (node as any).cornerSmoothing = params[ParamNames.CORNER_SMOOTHING];
    }
  }
}

/**
 * Handle applyStyle command
 * Applies an existing style (paint, effect, or text) to a node
 */
export async function handleApplyStyle(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const styleType = params[ParamNames.STYLE_TYPE];
  const styleId = params[ParamNames.STYLE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!styleType) {
    throw new Error(ErrorMessages.missingParam(ParamNames.STYLE_TYPE));
  }
  if (!styleId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.STYLE_ID));
  }

  const node = getNode(nodeId);
  const style = figma.getStyleById(styleId);

  if (!style) {
    throw new Error(`Style not found: ${styleId}`);
  }

  // Apply the appropriate style type
  switch (styleType) {
    case 'FILL':
      if ('fillStyleId' in node) {
        (node as any).fillStyleId = styleId;
      } else {
        throw new Error('Node does not support fill styles');
      }
      break;

    case 'STROKE':
      if ('strokeStyleId' in node) {
        (node as any).strokeStyleId = styleId;
      } else {
        throw new Error('Node does not support stroke styles');
      }
      break;

    case 'EFFECT':
      if ('effectStyleId' in node) {
        (node as any).effectStyleId = styleId;
      } else {
        throw new Error('Node does not support effect styles');
      }
      break;

    case 'TEXT':
      if (node.type === 'TEXT') {
        (node as TextNode).textStyleId = styleId;
      } else {
        throw new Error('Node must be a text node to apply text styles');
      }
      break;

    default:
      throw new Error(`Unknown style type: ${styleType}`);
  }
}
