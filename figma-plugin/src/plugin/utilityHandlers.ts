/**
 * Figma Plugin Utility Handlers
 *
 * ## Purpose
 * Handles all utility operations for Category 10: Utility Tools
 * - deleteNode: Remove nodes
 * - showNode: Navigate to nodes
 * - renameNode: Change node names
 * - setVisible: Show/hide nodes
 * - setLocked: Lock/unlock nodes
 * - notify: Show notifications
 */

import { ParamNames, ErrorMessages } from './constants';

/**
 * Handle utility operations
 * Routes to the appropriate handler based on operation type
 */
export async function handleUtilityOperation(params: Record<string, any>): Promise<void> {
  const operation = params[ParamNames.OPERATION];

  if (!operation) {
    throw new Error(ErrorMessages.missingParam(ParamNames.OPERATION));
  }

  console.log('[UtilityHandlers] Operation:', operation, 'Params:', JSON.stringify(params));

  switch (operation) {
    case 'deleteNode':
      await handleDeleteNode(params);
      break;

    case 'showNode':
      await handleShowNode(params);
      break;

    case 'renameNode':
      await handleRenameNode(params);
      break;

    case 'setVisible':
      await handleSetVisible(params);
      break;

    case 'setLocked':
      await handleSetLocked(params);
      break;

    case 'notify':
      await handleNotify(params);
      break;

    default:
      throw new Error(`Unknown utility operation: ${operation}`);
  }
}

/**
 * Delete a node from the document
 */
async function handleDeleteNode(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }

  node.remove();
  console.log('[UtilityHandlers] Deleted node:', nodeId);
}

/**
 * Show node in viewport
 */
async function handleShowNode(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }

  // Scroll viewport to show node
  figma.viewport.scrollAndZoomIntoView([node]);

  // Apply zoom if specified
  if (params[ParamNames.ZOOM] !== undefined) {
    const zoom = params[ParamNames.ZOOM];
    figma.viewport.zoom = zoom;
  }

  console.log('[UtilityHandlers] Navigated to node:', nodeId);
}

/**
 * Rename a node
 */
async function handleRenameNode(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const name = params[ParamNames.NAME];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!name) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NAME));
  }

  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }

  node.name = name;
  console.log('[UtilityHandlers] Renamed node:', nodeId, 'to:', name);
}

/**
 * Set node visibility
 */
async function handleSetVisible(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const visible = params[ParamNames.VISIBLE];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (visible === undefined) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VISIBLE));
  }

  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }

  (node as any).visible = visible;
  console.log('[UtilityHandlers] Set node visibility:', nodeId, visible);
}

/**
 * Set node locked state
 */
async function handleSetLocked(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const locked = params[ParamNames.LOCKED];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (locked === undefined) {
    throw new Error(ErrorMessages.missingParam(ParamNames.LOCKED));
  }

  const node = await figma.getNodeByIdAsync(nodeId);
  if (!node) {
    throw new Error(ErrorMessages.NODE_NOT_FOUND);
  }

  (node as any).locked = locked;
  console.log('[UtilityHandlers] Set node locked:', nodeId, locked);
}

/**
 * Show notification to user
 */
async function handleNotify(params: Record<string, any>): Promise<void> {
  const message = params[ParamNames.MESSAGE];

  if (!message) {
    throw new Error(ErrorMessages.missingParam(ParamNames.MESSAGE));
  }

  const timeout = params[ParamNames.TIMEOUT] || 3000;
  const isError = params[ParamNames.ERROR] || false;

  figma.notify(message, {
    timeout: timeout,
    error: isError
  });

  console.log('[UtilityHandlers] Notification shown:', message);
}
