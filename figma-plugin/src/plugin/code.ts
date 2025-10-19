/// <reference types="@figma/plugin-typings" />

/**
 * Figma MCP Plugin - REFACTORED
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle (SRP) - FIXED ✅
 * **BEFORE**: One massive file with all logic mixed together
 * **AFTER**: Separated into modules:
 * - constants.ts: Configuration and constants
 * - nodeHandlers.ts: Node creation logic
 * - code.ts: WebSocket communication and routing
 *
 * ### Open-Closed Principle (OCP) - FIXED ✅
 * **BEFORE**: Giant switch statement for node types
 * **AFTER**: Handler registry pattern - new node types are added by registration
 *
 * ### Dependency Inversion Principle (DIP) - IMPROVED ✅
 * - Depends on Figma API abstractions
 * - Handler functions are modular and injectable
 */

import { PluginMessage, UIMessage } from './types';
import { NodeTypes, PluginMethods, ErrorMessages, SuccessMessages } from './constants';
import * as NodeHandlers from './nodeHandlers';

// ============================================================================
// PLUGIN STATE
// ============================================================================

let isConnected = false;

// ============================================================================
// NODE CREATION HANDLER REGISTRY
// ============================================================================

/**
 * Registry mapping node types to their creation handlers
 * ✅ REFACTORED: Uses Strategy Pattern instead of switch statement
 */
const nodeCreationHandlers: Record<string, (params: Record<string, any>) => Promise<SceneNode>> = {
  [NodeTypes.FRAME]: NodeHandlers.createFrame,
  [NodeTypes.COMPONENT]: NodeHandlers.createComponent,
  [NodeTypes.INSTANCE]: NodeHandlers.createInstance,
  [NodeTypes.RECTANGLE]: NodeHandlers.createRectangle,
  [NodeTypes.ELLIPSE]: NodeHandlers.createEllipse,
  [NodeTypes.TEXT]: NodeHandlers.createText,
  [NodeTypes.POLYGON]: NodeHandlers.createPolygon,
  [NodeTypes.STAR]: NodeHandlers.createStar,
  [NodeTypes.LINE]: NodeHandlers.createLine,
};

// ============================================================================
// WEBSOCKET COMMAND HANDLERS
// ============================================================================

/**
 * Handle createNode command
 * ✅ REFACTORED: Uses handler registry instead of switch statement
 */
async function handleCreateNode(params: Record<string, any>, requestId?: string): Promise<void> {
  try {
    const nodeType = params.type;
    if (!nodeType) {
      throw new Error(ErrorMessages.missingParam('type'));
    }

    // Look up handler in registry
    const handler = nodeCreationHandlers[nodeType];
    if (!handler) {
      throw new Error(`Unknown node type: ${nodeType}`);
    }

    // Execute handler
    const node = await handler(params);

    // Send success response
    if (requestId) {
      sendWSResponse(requestId, {
        success: true,
        nodeId: node.id,
        message: SuccessMessages.nodeCreatedWithId(nodeType, node.id),
      });
    } else {
      sendMessage({
        type: 'operation-complete',
        message: SuccessMessages.nodeCreated(nodeType),
      });
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    if (requestId) {
      sendWSResponse(requestId, {
        success: false,
        error: errorMessage,
      });
    } else {
      sendMessage({
        type: 'operation-error',
        error: errorMessage,
      });
    }
  }
}

/**
 * Handle groupNodes command
 */
async function handleGroupNodes(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const group = await NodeHandlers.createGroup(params);

    sendWSResponse(requestId, {
      success: true,
      nodeId: group.id,
      message: SuccessMessages.nodesGrouped(params.nodeIds?.length ?? 0),
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    sendWSResponse(requestId, {
      success: false,
      error: errorMessage,
    });
  }
}

/**
 * Handle createBooleanOperation command
 */
async function handleCreateBooleanOperation(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const booleanOp = await NodeHandlers.createBooleanOperation(params);

    sendWSResponse(requestId, {
      success: true,
      nodeId: booleanOp.id,
      message: SuccessMessages.booleanOpCreated(
        params.operation,
        params.nodeIds?.length ?? 0
      ),
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    sendWSResponse(requestId, {
      success: false,
      error: errorMessage,
    });
  }
}

/**
 * Handle getInfo command
 */
async function handleGetInfo(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    if (params.type === 'selection') {
      const selection = figma.currentPage.selection;
      const selectionInfo = selection.map((node) => ({
        id: node.id,
        name: node.name,
        type: node.type,
        x: 'x' in node ? node.x : 0,
        y: 'y' in node ? node.y : 0,
        width: 'width' in node ? node.width : 0,
        height: 'height' in node ? node.height : 0,
      }));
      sendWSResponse(requestId, selectionInfo);
    } else if (params.nodeId) {
      const node = figma.getNodeById(params.nodeId);
      if (node) {
        const nodeInfo = {
          id: node.id,
          name: node.name,
          type: node.type,
          x: 'x' in node ? node.x : 0,
          y: 'y' in node ? node.y : 0,
          width: 'width' in node ? node.width : 0,
          height: 'height' in node ? node.height : 0,
        };
        sendWSResponse(requestId, nodeInfo);
      } else {
        sendWSResponse(requestId, { error: ErrorMessages.NODE_NOT_FOUND });
      }
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    sendWSResponse(requestId, { error: errorMessage });
  }
}

/**
 * Handle setProperties command
 */
async function handleSetProperties(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const node = figma.getNodeById(params.nodeId);
    if (node) {
      Object.entries(params.properties).forEach(([key, value]) => {
        if (key in node) {
          (node as any)[key] = value;
        }
      });
      sendWSResponse(requestId, { success: true });
    } else {
      sendWSResponse(requestId, { error: ErrorMessages.NODE_NOT_FOUND });
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    sendWSResponse(requestId, { error: errorMessage });
  }
}

/**
 * Main WebSocket command handler
 * ✅ REFACTORED: Cleaner routing to specific handlers
 */
async function handleWSCommand(command: any): Promise<void> {
  try {
    const { id, method, params } = command;

    switch (method) {
      case PluginMethods.CREATE_NODE:
        await handleCreateNode(params, id);
        break;

      case PluginMethods.GROUP_NODES:
        await handleGroupNodes(params, id);
        break;

      case PluginMethods.CREATE_BOOLEAN_OP:
        await handleCreateBooleanOperation(params, id);
        break;

      case PluginMethods.GET_INFO:
        await handleGetInfo(params, id);
        break;

      case PluginMethods.SET_PROPERTIES:
        await handleSetProperties(params, id);
        break;

      default:
        sendWSResponse(id, { error: `Unknown method: ${method}` });
    }
  } catch (error) {
    console.error('Error handling WS command:', error);
  }
}

// ============================================================================
// MESSAGE SENDING FUNCTIONS
// ============================================================================

/**
 * Send message to UI
 */
function sendMessage(message: PluginMessage): void {
  figma.ui.postMessage(message);
}

/**
 * Send WebSocket response via UI
 */
function sendWSResponse(requestId: string, result: any): void {
  sendMessage({
    type: 'ws-response',
    requestId: requestId,
    result: result,
  });
}

// ============================================================================
// INITIALIZATION
// ============================================================================

// Show plugin UI
figma.showUI(__html__, {
  width: 480,
  height: 640,
  themeColors: true,
});

// Handle messages from UI
figma.ui.onmessage = async (msg: UIMessage) => {
  switch (msg.type) {
    case 'connect-ws':
      isConnected = true;
      break;

    case 'disconnect-ws':
      isConnected = false;
      break;

    case 'ws-command':
      await handleWSCommand(msg.command);
      break;

    case 'create-node':
      await handleCreateNode(msg.properties);
      break;

    case 'get-selection':
      const selection = figma.currentPage.selection;
      const selectionInfo = selection.map((node) => ({
        id: node.id,
        name: node.name,
        type: node.type,
        x: 'x' in node ? node.x : 0,
        y: 'y' in node ? node.y : 0,
        width: 'width' in node ? node.width : 0,
        height: 'height' in node ? node.height : 0,
      }));
      sendMessage({
        type: 'selection-changed',
        selection: selectionInfo,
      });
      break;

    default:
      console.warn('Unknown message type:', (msg as any).type);
  }
};

// Listen for selection changes
figma.on('selectionchange', () => {
  const selection = figma.currentPage.selection;
  const selectionInfo = selection.map((node) => ({
    id: node.id,
    name: node.name,
    type: node.type,
    x: 'x' in node ? node.x : 0,
    y: 'y' in node ? node.y : 0,
    width: 'width' in node ? node.width : 0,
    height: 'height' in node ? node.height : 0,
  }));

  sendMessage({
    type: 'selection-changed',
    selection: selectionInfo,
  });
});

// Send initial selection on startup
(() => {
  const selection = figma.currentPage.selection;
  const selectionInfo = selection.map((node) => ({
    id: node.id,
    name: node.name,
    type: node.type,
    x: 'x' in node ? node.x : 0,
    y: 'y' in node ? node.y : 0,
    width: 'width' in node ? node.width : 0,
    height: 'height' in node ? node.height : 0,
  }));

  sendMessage({
    type: 'selection-changed',
    selection: selectionInfo,
  });
})();
