/**
 * Figma Plugin Typography Handlers
 *
 * ## Purpose
 * Handles all typography operations for Category 4: Typography Tools
 * - setTextContent: Change text content
 * - setTextStyle: Set text formatting (font, size, weight, line height, etc.)
 * - setTextAlignment: Control text alignment
 * - setTextAutoResize: Control text box auto-sizing
 * - setTextTruncation: Control text overflow
 * - loadFont: Load fonts before use
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific typography operation
 * - Open-Closed: New typography operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Get node by ID with type checking
 */
async function getNode(nodeId: string): Promise<SceneNode> {
  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }
  return node as SceneNode;
}

/**
 * Check if node is a text node
 */
function isTextNode(node: SceneNode): node is TextNode {
  return node.type === 'TEXT';
}

/**
 * Ensure node is a text node
 */
function ensureTextNode(node: SceneNode): TextNode {
  if (!isTextNode(node)) {
    throw new Error('Node must be a TEXT node for typography operations');
  }
  return node;
}

// ============================================================================
// TYPOGRAPHY HANDLERS
// ============================================================================

/**
 * Handle setTextContent command (4.1)
 * Changes the text content of a text node
 */
export async function handleSetTextContent(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const characters = params[ParamNames.CHARACTERS];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (characters === undefined || characters === null) {
    throw new Error(ErrorMessages.missingParam(ParamNames.CHARACTERS));
  }

  const node = await getNode(nodeId);
  const textNode = ensureTextNode(node);

  // Load the current font before modifying text
  const fontName = textNode.fontName;
  if (typeof fontName !== 'symbol') {
    await figma.loadFontAsync(fontName);
  }

  // Set the text content
  textNode.characters = String(characters);
}

/**
 * Handle setTextStyle command (4.2)
 * Sets text formatting properties
 */
export async function handleSetTextStyle(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);
  const textNode = ensureTextNode(node);

  // Load current font first
  const currentFontName = textNode.fontName;
  if (typeof currentFontName !== 'symbol') {
    await figma.loadFontAsync(currentFontName);
  }

  // Apply font family and style
  if (params[ParamNames.FONT_FAMILY] !== undefined || params[ParamNames.FONT_STYLE] !== undefined) {
    const currentFont = typeof currentFontName !== 'symbol' ? currentFontName : { family: 'Inter', style: 'Regular' };
    const fontFamily = params[ParamNames.FONT_FAMILY] || currentFont.family;
    const fontStyle = params[ParamNames.FONT_STYLE] || currentFont.style;

    const newFontName: FontName = {
      family: fontFamily,
      style: fontStyle,
    };

    // Load the new font
    try {
      await figma.loadFontAsync(newFontName);
      textNode.fontName = newFontName;
    } catch (error) {
      throw new Error(`${ErrorMessages.FONT_LOAD_FAILED}: ${fontFamily} ${fontStyle}`);
    }
  }

  // Apply font size
  if (params[ParamNames.FONT_SIZE] !== undefined) {
    textNode.fontSize = params[ParamNames.FONT_SIZE];
  }

  // Apply font weight
  if (params[ParamNames.FONT_WEIGHT] !== undefined) {
    // Font weight is handled through fontStyle (e.g., "Bold", "Medium", etc.)
    // This is a convenience parameter that tries common weight names
    const weight = params[ParamNames.FONT_WEIGHT];
    const weightMap: Record<number, string> = {
      100: 'Thin',
      200: 'ExtraLight',
      300: 'Light',
      400: 'Regular',
      500: 'Medium',
      600: 'SemiBold',
      700: 'Bold',
      800: 'ExtraBold',
      900: 'Black',
    };

    const currentFont = typeof currentFontName !== 'symbol' ? currentFontName : { family: 'Inter', style: 'Regular' };
    const styleName = weightMap[weight] || currentFont.style;
    const newFontName: FontName = {
      family: currentFont.family,
      style: styleName,
    };

    try {
      await figma.loadFontAsync(newFontName);
      textNode.fontName = newFontName;
    } catch (error) {
      // If weight not found, silently ignore and keep current font
      console.warn(`Font weight ${weight} (${styleName}) not available for ${currentFont.family}`);
    }
  }

  // Apply line height
  if (params[ParamNames.LINE_HEIGHT] !== undefined) {
    const lineHeight = params[ParamNames.LINE_HEIGHT];
    if (typeof lineHeight === 'object') {
      if (lineHeight.unit === 'AUTO') {
        textNode.lineHeight = { unit: 'AUTO' };
      } else if (lineHeight.unit === 'PIXELS') {
        textNode.lineHeight = { unit: 'PIXELS', value: lineHeight.value };
      } else if (lineHeight.unit === 'PERCENT') {
        textNode.lineHeight = { unit: 'PERCENT', value: lineHeight.value };
      }
    } else if (typeof lineHeight === 'number') {
      // If just a number is provided, treat as pixels
      textNode.lineHeight = { unit: 'PIXELS', value: lineHeight };
    }
  }

  // Apply letter spacing
  if (params[ParamNames.LETTER_SPACING] !== undefined) {
    const letterSpacing = params[ParamNames.LETTER_SPACING];
    if (typeof letterSpacing === 'object') {
      if (letterSpacing.unit === 'PIXELS') {
        textNode.letterSpacing = { unit: 'PIXELS', value: letterSpacing.value };
      } else if (letterSpacing.unit === 'PERCENT') {
        textNode.letterSpacing = { unit: 'PERCENT', value: letterSpacing.value };
      }
    } else if (typeof letterSpacing === 'number') {
      // If just a number is provided, treat as percent
      textNode.letterSpacing = { unit: 'PERCENT', value: letterSpacing };
    }
  }

  // Apply text case
  if (params[ParamNames.TEXT_CASE] !== undefined) {
    textNode.textCase = params[ParamNames.TEXT_CASE];
  }

  // Apply text decoration
  if (params[ParamNames.TEXT_DECORATION] !== undefined) {
    textNode.textDecoration = params[ParamNames.TEXT_DECORATION];
  }
}

/**
 * Handle setTextAlignment command (4.3)
 * Controls text alignment
 */
export async function handleSetTextAlignment(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);
  const textNode = ensureTextNode(node);

  // Apply horizontal alignment
  if (params[ParamNames.TEXT_ALIGN_HORIZONTAL] !== undefined) {
    textNode.textAlignHorizontal = params[ParamNames.TEXT_ALIGN_HORIZONTAL];
  }

  // Apply vertical alignment
  if (params[ParamNames.TEXT_ALIGN_VERTICAL] !== undefined) {
    textNode.textAlignVertical = params[ParamNames.TEXT_ALIGN_VERTICAL];
  }
}

/**
 * Handle setTextAutoResize command (4.4)
 * Controls text box auto-sizing
 */
export async function handleSetTextAutoResize(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const textAutoResize = params[ParamNames.TEXT_AUTO_RESIZE];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!textAutoResize) {
    throw new Error(ErrorMessages.missingParam(ParamNames.TEXT_AUTO_RESIZE));
  }

  const node = await getNode(nodeId);
  const textNode = ensureTextNode(node);

  textNode.textAutoResize = textAutoResize;

  // Apply max lines if provided
  if (params[ParamNames.MAX_LINES] !== undefined) {
    // maxLines is only available when textAutoResize is HEIGHT or WIDTH_AND_HEIGHT
    if (textAutoResize === 'HEIGHT' || textAutoResize === 'WIDTH_AND_HEIGHT') {
      // Note: maxLines is not directly available in Figma Plugin API
      // This would need to be handled through textTruncation
      console.warn('maxLines parameter requires textTruncation to be enabled');
    }
  }
}

/**
 * Handle setTextTruncation command (4.5)
 * Controls text overflow behavior
 */
export async function handleSetTextTruncation(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const textTruncation = params[ParamNames.TEXT_TRUNCATION];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!textTruncation) {
    throw new Error(ErrorMessages.missingParam(ParamNames.TEXT_TRUNCATION));
  }

  const node = await getNode(nodeId);
  const textNode = ensureTextNode(node);

  textNode.textTruncation = textTruncation;

  // Apply max lines if provided
  if (params[ParamNames.MAX_LINES] !== undefined && textTruncation === 'ENDING') {
    textNode.maxLines = params[ParamNames.MAX_LINES];
  }
}

/**
 * Handle loadFont command (4.6)
 * Loads a font before use
 */
export async function handleLoadFont(params: Record<string, any>): Promise<{ available: boolean; message?: string }> {
  const fontFamily = params[ParamNames.FONT_FAMILY];
  const fontStyle = params[ParamNames.FONT_STYLE];

  if (!fontFamily) {
    throw new Error(ErrorMessages.missingParam(ParamNames.FONT_FAMILY));
  }
  if (!fontStyle) {
    throw new Error(ErrorMessages.missingParam(ParamNames.FONT_STYLE));
  }

  const fontName: FontName = {
    family: fontFamily,
    style: fontStyle,
  };

  try {
    await figma.loadFontAsync(fontName);
    return {
      available: true,
      message: `Successfully loaded font: ${fontFamily} ${fontStyle}`,
    };
  } catch (error) {
    return {
      available: false,
      message: `Failed to load font: ${fontFamily} ${fontStyle}. ${error instanceof Error ? error.message : String(error)}`,
    };
  }
}