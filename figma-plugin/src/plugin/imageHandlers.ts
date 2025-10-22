/**
 * Figma Plugin Image & Media Handlers
 *
 * ## Purpose
 * Handles all image and media operations for Category 9: Image & Media Tools
 *
 * ### Image & Media Tools (4)
 * - handleCreateImage: Create image node from URL or base64
 * - handleSetImageFill: Apply image fill to node
 * - handleExportNode: Export node as image
 * - handleGetImageFills: Get image fills from node
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific image operation
 * - Open-Closed: New image operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// TYPES
// ============================================================================

interface ImageNodeResult {
  imageNodeId: string;
  width: number;
  height: number;
}

interface ExportResult {
  imageData: Uint8Array;
  format: string;
  width: number;
  height: number;
}

interface ImagePaintInfo {
  type: string;
  scaleMode: string;
  imageHash: string | null;
  rotation: number;
  opacity: number;
  visible: boolean;
}

interface ImageFillsResult {
  images: ImagePaintInfo[];
  count: number;
}

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
 * Fetch image data from URL
 */
async function fetchImageFromUrl(url: string): Promise<Uint8Array> {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch image: ${response.statusText}`);
    }
    const arrayBuffer = await response.arrayBuffer();
    return new Uint8Array(arrayBuffer);
  } catch (error) {
    throw new Error(`Failed to load image from URL: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Decode base64 string to Uint8Array
 */
function decodeBase64(base64: string): Uint8Array {
  try {
    // Remove data URL prefix if present (e.g., "data:image/png;base64,")
    const base64Data = base64.includes(',') ? base64.split(',')[1] : base64;

    // Decode base64 to binary string
    const binaryString = atob(base64Data);

    // Convert binary string to Uint8Array
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }

    return bytes;
  } catch (error) {
    throw new Error(`Failed to decode base64 image data: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Load image data from URL or base64
 */
async function loadImageData(imageData: string): Promise<Uint8Array> {
  // Check if it's a URL or base64
  if (imageData.startsWith('http://') || imageData.startsWith('https://')) {
    return await fetchImageFromUrl(imageData);
  } else {
    return decodeBase64(imageData);
  }
}

/**
 * Ensure node supports fills
 */
function ensureFillable(node: SceneNode): node is GeometryMixin & SceneNode {
  if (!('fills' in node)) {
    throw new Error(`Node type ${node.type} does not support fills`);
  }
  return true;
}

/**
 * Parse scale mode string to Figma ScaleMode
 */
function parseScaleMode(scaleMode?: string): 'FILL' | 'FIT' | 'CROP' | 'TILE' {
  const mode = (scaleMode || 'FILL').toUpperCase();
  if (mode === 'FILL' || mode === 'FIT' || mode === 'CROP' || mode === 'TILE') {
    return mode;
  }
  throw new Error(`Invalid scale mode: ${scaleMode}. Must be FILL, FIT, CROP, or TILE`);
}

/**
 * Parse export format string
 */
function parseExportFormat(format?: string): 'PNG' | 'JPG' | 'SVG' | 'PDF' {
  const fmt = (format || 'PNG').toUpperCase();
  if (fmt === 'PNG' || fmt === 'JPG' || fmt === 'SVG' || fmt === 'PDF') {
    return fmt;
  }
  throw new Error(`Invalid export format: ${format}. Must be PNG, JPG, SVG, or PDF`);
}

// ============================================================================
// IMAGE HANDLERS
// ============================================================================

/**
 * Handle handleCreateImage command (9.1)
 * Create image node from URL or base64
 */
export async function handleCreateImage(params: Record<string, any>): Promise<ImageNodeResult> {
  const imageData = params[ParamNames.IMAGE_DATA];
  const width = params[ParamNames.WIDTH];
  const height = params[ParamNames.HEIGHT];
  const x = params[ParamNames.X] ?? 0;
  const y = params[ParamNames.Y] ?? 0;

  if (!imageData) {
    throw new Error(ErrorMessages.missingParam(ParamNames.IMAGE_DATA));
  }

  // Load image data
  const bytes = await loadImageData(imageData);

  // Create image in Figma
  const image = figma.createImage(bytes);
  const imageHash = image.hash;

  // Get image size if not provided
  let imageWidth = width;
  let imageHeight = height;

  if (!imageWidth || !imageHeight) {
    // Use default size if dimensions not provided
    imageWidth = imageWidth || 200;
    imageHeight = imageHeight || 200;
  }

  // Create rectangle node with image fill
  const rect = figma.createRectangle();
  rect.x = x;
  rect.y = y;
  rect.resize(imageWidth, imageHeight);

  // Apply image fill
  const imagePaint: ImagePaint = {
    type: 'IMAGE',
    scaleMode: 'FILL',
    imageHash: imageHash,
  };

  rect.fills = [imagePaint];
  rect.name = 'Image';

  // Add to current page
  figma.currentPage.appendChild(rect);

  console.log('[ImageHandlers] Created image node:', rect.id);

  return {
    imageNodeId: rect.id,
    width: imageWidth,
    height: imageHeight,
  };
}

/**
 * Handle handleSetImageFill command (9.2)
 * Apply image fill to node
 */
export async function handleSetImageFill(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const imageData = params[ParamNames.IMAGE_DATA];
  const scaleMode = params[ParamNames.SCALE_MODE];
  const rotation = params[ParamNames.ROTATION] ?? 0;
  const opacity = params[ParamNames.OPACITY] ?? 1;

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!imageData) {
    throw new Error(ErrorMessages.missingParam(ParamNames.IMAGE_DATA));
  }

  const node = await getNode(nodeId);
  ensureFillable(node);

  // Load image data
  const bytes = await loadImageData(imageData);

  // Create image in Figma
  const image = figma.createImage(bytes);
  const imageHash = image.hash;

  // Parse scale mode
  const parsedScaleMode = parseScaleMode(scaleMode);

  // Create image paint
  const imagePaint: ImagePaint = {
    type: 'IMAGE',
    scaleMode: parsedScaleMode,
    imageHash: imageHash,
    rotation: rotation,
    opacity: opacity,
    visible: true,
  };

  // Apply to node fills
  const fillableNode = node as GeometryMixin & SceneNode;
  fillableNode.fills = [imagePaint];

  console.log('[ImageHandlers] Set image fill for node:', nodeId);
}

/**
 * Handle handleExportNode command (9.3)
 * Export node as image
 */
export async function handleExportNode(params: Record<string, any>): Promise<ExportResult> {
  const nodeId = params[ParamNames.NODE_ID];
  const format = params[ParamNames.FORMAT];
  const scale = params[ParamNames.SCALE] ?? 1;
  const constraint = params[ParamNames.CONSTRAINT];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  // Parse export format
  const exportFormat = parseExportFormat(format);

  // Build export settings based on format
  let exportSettings: ExportSettings;

  if (exportFormat === 'SVG') {
    exportSettings = {
      format: 'SVG',
    };
  } else if (exportFormat === 'PDF') {
    exportSettings = {
      format: 'PDF',
    };
  } else {
    // PNG or JPG - supports constraint
    const constraintValue = constraint || { type: 'SCALE' as const, value: scale };
    exportSettings = {
      format: exportFormat,
      constraint: constraintValue,
    };
  }

  // Export node
  const bytes = await node.exportAsync(exportSettings);

  // Get node dimensions
  const width = 'width' in node ? (node as any).width : 0;
  const height = 'height' in node ? (node as any).height : 0;

  console.log('[ImageHandlers] Exported node:', nodeId, 'as', exportFormat);

  return {
    imageData: bytes,
    format: exportFormat,
    width: Math.round(width * scale),
    height: Math.round(height * scale),
  };
}

/**
 * Handle handleGetImageFills command (9.4)
 * Get image fills from node
 */
export async function handleGetImageFills(params: Record<string, any>): Promise<ImageFillsResult> {
  const nodeId = params[ParamNames.NODE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  if (!('fills' in node)) {
    return {
      images: [],
      count: 0,
    };
  }

  const fillableNode = node as GeometryMixin & SceneNode;
  const fills = fillableNode.fills;

  // Extract image fills
  const imageFills: ImagePaintInfo[] = [];

  if (Array.isArray(fills)) {
    for (const fill of fills) {
      if (fill.type === 'IMAGE') {
        const imagePaint = fill as ImagePaint;
        imageFills.push({
          type: 'IMAGE',
          scaleMode: imagePaint.scaleMode,
          imageHash: imagePaint.imageHash,
          rotation: imagePaint.rotation || 0,
          opacity: imagePaint.opacity || 1,
          visible: imagePaint.visible !== false,
        });
      }
    }
  }

  // Also check strokes for image paints
  if ('strokes' in node) {
    const strokes = (node as any).strokes;
    if (Array.isArray(strokes)) {
      for (const stroke of strokes) {
        if (stroke.type === 'IMAGE') {
          const imagePaint = stroke as ImagePaint;
          imageFills.push({
            type: 'IMAGE',
            scaleMode: imagePaint.scaleMode,
            imageHash: imagePaint.imageHash,
            rotation: imagePaint.rotation || 0,
            opacity: imagePaint.opacity || 1,
            visible: imagePaint.visible !== false,
          });
        }
      }
    }
  }

  console.log('[ImageHandlers] Found', imageFills.length, 'image fills for node:', nodeId);

  return {
    images: imageFills,
    count: imageFills.length,
  };
}
