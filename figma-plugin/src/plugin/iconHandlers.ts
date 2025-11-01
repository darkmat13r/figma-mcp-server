/**
 * Figma Plugin Icon Handlers
 *
 * ## Purpose
 * Handles all icon operations, specifically Lucide icon creation from SVG content
 *
 * ### Icon Tools
 * - handleCreateLucideIcon: Create vector icon from SVG content (Lucide icons)
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific icon operation
 * - Open-Closed: New icon operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages, SuccessMessages, Defaults } from './constants';

// ============================================================================
// TYPES
// ============================================================================

interface LucideIconResult {
  iconNodeId: string;
  iconName: string;
  width: number;
  height: number;
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Parse hex color to RGB object
 * Supports #RGB, #RRGGBB formats
 */
function parseHexColor(hex: string): RGB {
  // Remove # if present
  let cleanHex = hex.replace('#', '');

  // Expand 3-digit hex to 6-digit
  if (cleanHex.length === 3) {
    cleanHex = cleanHex
      .split('')
      .map((char) => char + char)
      .join('');
  }

  if (cleanHex.length !== 6) {
    throw new Error(`Invalid hex color: ${hex}`);
  }

  const r = parseInt(cleanHex.substring(0, 2), 16) / 255;
  const g = parseInt(cleanHex.substring(2, 4), 16) / 255;
  const b = parseInt(cleanHex.substring(4, 6), 16) / 255;

  return { r, g, b };
}

/**
 * Apply color to all children of a node recursively
 * Used to recolor SVG icons created with createNodeFromSvg
 */
function applyColorToNode(node: SceneNode, color: RGB): void {
  // Apply to current node if it supports fills
  if ('fills' in node) {
    const fills = node.fills;
    if (Array.isArray(fills) && fills.length > 0) {
      node.fills = fills.map((fill) => {
        if (fill.type === 'SOLID') {
          return {
            ...fill,
            color: color,
          } as SolidPaint;
        }
        return fill;
      });
    }
  }

  // Apply to strokes if present
  if ('strokes' in node) {
    const strokes = node.strokes;
    if (Array.isArray(strokes) && strokes.length > 0) {
      node.strokes = strokes.map((stroke) => {
        if (stroke.type === 'SOLID') {
          return {
            ...stroke,
            color: color,
          } as SolidPaint;
        }
        return stroke;
      });
    }
  }

  // Recursively apply to children
  if ('children' in node) {
    for (const child of node.children) {
      applyColorToNode(child, color);
    }
  }
}

/**
 * Create Figma node from SVG using native figma.createNodeFromSvg
 * This is the preferred method as it handles complex SVG parsing
 */
async function createNodeFromSVG(svgContent: string): Promise<FrameNode> {
  try {
    const node = figma.createNodeFromSvg(svgContent);

    // figma.createNodeFromSvg returns a FrameNode containing the SVG
    if (!node || node.type !== 'FRAME') {
      throw new Error('Failed to create node from SVG');
    }

    return node;
  } catch (error) {
    throw new Error(`${ErrorMessages.ICON_CREATION_FAILED}: ${error instanceof Error ? error.message : String(error)}`);
  }
}

// ============================================================================
// ICON HANDLERS
// ============================================================================

/**
 * Handle handleCreateLucideIcon command
 * Create Lucide icon from SVG content
 *
 * @param params.iconName - Name of the icon (for labeling)
 * @param params.svgContent - SVG markup string
 * @param params.size - Optional size in pixels (default: 24)
 * @param params.color - Optional hex color (e.g., "#000000")
 * @param params.x - Optional x position (default: 0)
 * @param params.y - Optional y position (default: 0)
 */
export async function handleCreateLucideIcon(params: Record<string, any>): Promise<LucideIconResult> {
  const iconName = params[ParamNames.ICON_NAME];
  const svgContent = params[ParamNames.SVG_CONTENT] || params[ParamNames.SVG_DATA];
  const size = params[ParamNames.SIZE] || 24;
  const color = params[ParamNames.COLOR];
  const x = params[ParamNames.X] ?? 0;
  const y = params[ParamNames.Y] ?? 0;

  // Validate required parameters
  if (!iconName) {
    throw new Error(ErrorMessages.missingParam(ParamNames.ICON_NAME));
  }
  if (!svgContent) {
    throw new Error(ErrorMessages.missingParam(ParamNames.SVG_CONTENT));
  }

  console.log('[IconHandlers] Creating Lucide icon:', iconName);

  try {
    // Use Figma's native SVG parser (recommended approach)
    // This handles all SVG complexity including transformations, groups, etc.
    const iconNode = await createNodeFromSVG(svgContent);
    console.log('[IconHandlers] Created icon using native SVG parser');

    // Set position
    iconNode.x = x;
    iconNode.y = y;

    // Set name
    iconNode.name = iconName;

    // Apply custom color if provided
    if (color) {
      try {
        const rgbColor = parseHexColor(color);
        applyColorToNode(iconNode, rgbColor);
        console.log('[IconHandlers] Applied custom color:', color);
      } catch (colorError) {
        console.warn('[IconHandlers] Failed to apply color, using SVG default:', colorError);
      }
    }

    // Resize to target size while maintaining aspect ratio
    const currentWidth = iconNode.width;
    const currentHeight = iconNode.height;

    if (currentWidth > 0 && currentHeight > 0) {
      const scale = size / Math.max(currentWidth, currentHeight);
      iconNode.resize(currentWidth * scale, currentHeight * scale);
      console.log(`[IconHandlers] Resized icon from ${currentWidth}x${currentHeight} to ${iconNode.width}x${iconNode.height}`);
    }

    // Add to current page
    figma.currentPage.appendChild(iconNode);

    console.log('[IconHandlers] Successfully created Lucide icon:', iconNode.id);

    return {
      iconNodeId: iconNode.id,
      iconName: iconName,
      width: iconNode.width,
      height: iconNode.height,
    };
  } catch (error) {
    console.error('[IconHandlers] Error creating icon:', error);
    const errorMessage = error instanceof Error ? error.message : String(error);
    throw new Error(`${ErrorMessages.ICON_CREATION_FAILED}: ${errorMessage}`);
  }
}
