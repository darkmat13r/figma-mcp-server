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
import * as StyleHandlers from './styleHandlers';
import * as UtilityHandlers from './utilityHandlers';
import * as TypographyHandlers from './typographyHandlers';
import * as HierarchyHandlers from './hierarchyHandlers';

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
 *
 * All 12 Category 1 tools are supported:
 * - FRAME, COMPONENT, INSTANCE (containers)
 * - RECTANGLE, ELLIPSE, TEXT (basic shapes)
 * - POLYGON, STAR, LINE (geometric shapes)
 * - SECTION (organizational)
 * - GROUP and BOOLEAN_OPERATION handled by separate methods
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
  [NodeTypes.SECTION]: NodeHandlers.createSection,
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
 * Handle setStyle command (Category 3: Styling Tools)
 */
async function handleSetStyle(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await StyleHandlers.handleSetStyle(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully applied style to node: ${params.nodeId}`,
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
 * Handle applyStyle command (Category 3: Styling Tools)
 */
async function handleApplyStyle(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await StyleHandlers.handleApplyStyle(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully applied ${params.styleType} style to node: ${params.nodeId}`,
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
 * Handle utilityOperation command (Category 10: Utility Tools)
 */
async function handleUtilityOperation(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await UtilityHandlers.handleUtilityOperation(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully completed utility operation: ${params.operation}`,
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
 * Handle setTextContent command (Category 4: Typography Tools)
 */
async function handleSetTextContent(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await TypographyHandlers.handleSetTextContent(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set text content for node: ${params.nodeId}`,
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
 * Handle setTextStyle command (Category 4: Typography Tools)
 */
async function handleSetTextStyle(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await TypographyHandlers.handleSetTextStyle(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set text style for node: ${params.nodeId}`,
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
 * Handle setTextAlignment command (Category 4: Typography Tools)
 */
async function handleSetTextAlignment(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await TypographyHandlers.handleSetTextAlignment(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set text alignment for node: ${params.nodeId}`,
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
 * Handle setTextAutoResize command (Category 4: Typography Tools)
 */
async function handleSetTextAutoResize(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await TypographyHandlers.handleSetTextAutoResize(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set text auto-resize for node: ${params.nodeId}`,
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
 * Handle setTextTruncation command (Category 4: Typography Tools)
 */
async function handleSetTextTruncation(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await TypographyHandlers.handleSetTextTruncation(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set text truncation for node: ${params.nodeId}`,
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
 * Handle loadFont command (Category 4: Typography Tools)
 */
async function handleLoadFont(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await TypographyHandlers.handleLoadFont(params);
    sendWSResponse(requestId, {
      success: result.available,
      ...result,
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
 * Handle getNodeInfo command (Category 7: Hierarchy & Query Tools)
 */
async function handleGetNodeInfo(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const nodeInfo = await HierarchyHandlers.handleGetNodeInfo(params);
    sendWSResponse(requestId, {
      success: true,
      ...nodeInfo,
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
 * Handle getSelection command (Category 7: Hierarchy & Query Tools)
 * NOTE: This replaces the existing getInfo with type=selection
 */
async function handleGetSelectionCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const selection = await HierarchyHandlers.handleGetSelection(params);
    sendWSResponse(requestId, {
      success: true,
      selection: selection,
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
 * Handle setSelection command (Category 7: Hierarchy & Query Tools)
 */
async function handleSetSelection(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await HierarchyHandlers.handleSetSelection(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set selection to ${params.nodeIds?.length ?? 0} nodes`,
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
 * Handle findNodes command (Category 7: Hierarchy & Query Tools)
 */
async function handleFindNodes(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const nodes = await HierarchyHandlers.handleFindNodes(params);
    sendWSResponse(requestId, {
      success: true,
      nodes: nodes,
      count: nodes.length,
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
 * Handle getChildren command (Category 7: Hierarchy & Query Tools)
 */
async function handleGetChildren(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const children = await HierarchyHandlers.handleGetChildren(params);
    sendWSResponse(requestId, {
      success: true,
      children: children,
      count: children.length,
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
 * Handle getParent command (Category 7: Hierarchy & Query Tools)
 */
async function handleGetParentCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const parent = await HierarchyHandlers.handleGetParent(params);
    sendWSResponse(requestId, {
      success: true,
      parent: parent,
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
 * Handle moveNode command (Category 7: Hierarchy & Query Tools)
 */
async function handleMoveNodeCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await HierarchyHandlers.handleMoveNode(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully moved node ${params.nodeId} to parent ${params.parentId}`,
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
 * Handle cloneNode command (Category 7: Hierarchy & Query Tools)
 */
async function handleCloneNodeCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const clone = await HierarchyHandlers.handleCloneNode(params);
    sendWSResponse(requestId, {
      success: true,
      nodeId: clone.id,
      message: `Successfully cloned node ${params.nodeId}`,
      ...clone,
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
 * Handle getCurrentPageNodes command (Category 7: Hierarchy & Query Tools)
 */
async function handleGetCurrentPageNodes(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const nodes = await HierarchyHandlers.handleGetCurrentPageNodes(params);
    sendWSResponse(requestId, {
      success: true,
      nodes: nodes,
      count: nodes.length,
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
 * Handle searchNodes command (Category 7: Hierarchy & Query Tools)
 */
async function handleSearchNodes(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const nodes = await HierarchyHandlers.handleSearchNodes(params);
    sendWSResponse(requestId, {
      success: true,
      nodes: nodes,
      count: nodes.length,
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
 * Handle getAllPages command (Category 7: Hierarchy & Query Tools)
 */
async function handleGetAllPages(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const pages = await HierarchyHandlers.handleGetAllPages(params);
    sendWSResponse(requestId, {
      success: true,
      pages: pages,
      count: pages.length,
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
 * Handle switchPage command (Category 7: Hierarchy & Query Tools)
 */
async function handleSwitchPage(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await HierarchyHandlers.handleSwitchPage(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully switched to page ${params.pageId}`,
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
 * Handle createPage command (Category 7: Hierarchy & Query Tools)
 */
async function handleCreatePage(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const page = await HierarchyHandlers.handleCreatePage(params);
    sendWSResponse(requestId, {
      success: true,
      pageId: page.id,
      message: `Successfully created page: ${page.name}`,
      ...page,
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

      case PluginMethods.GET_SELECTION:
        await handleGetSelectionCommand(params, id);
        break;

      case PluginMethods.SET_PROPERTIES:
        await handleSetProperties(params, id);
        break;

      case PluginMethods.SET_STYLE:
        await handleSetStyle(params, id);
        break;

      case PluginMethods.APPLY_STYLE:
        await handleApplyStyle(params, id);
        break;

      case PluginMethods.UTILITY_OPERATION:
        await handleUtilityOperation(params, id);
        break;

      case PluginMethods.SET_TEXT_CONTENT:
        await handleSetTextContent(params, id);
        break;

      case PluginMethods.SET_TEXT_STYLE:
        await handleSetTextStyle(params, id);
        break;

      case PluginMethods.SET_TEXT_ALIGNMENT:
        await handleSetTextAlignment(params, id);
        break;

      case PluginMethods.SET_TEXT_AUTO_RESIZE:
        await handleSetTextAutoResize(params, id);
        break;

      case PluginMethods.SET_TEXT_TRUNCATION:
        await handleSetTextTruncation(params, id);
        break;

      case PluginMethods.LOAD_FONT:
        await handleLoadFont(params, id);
        break;

      // Category 7: Hierarchy & Query Tools
      case PluginMethods.GET_NODE_INFO:
        await handleGetNodeInfo(params, id);
        break;

      case PluginMethods.SET_SELECTION:
        await handleSetSelection(params, id);
        break;

      case PluginMethods.FIND_NODES:
        await handleFindNodes(params, id);
        break;

      case PluginMethods.GET_CHILDREN:
        await handleGetChildren(params, id);
        break;

      case PluginMethods.GET_PARENT:
        await handleGetParentCommand(params, id);
        break;

      case PluginMethods.MOVE_NODE:
        await handleMoveNodeCommand(params, id);
        break;

      case PluginMethods.CLONE_NODE:
        await handleCloneNodeCommand(params, id);
        break;

      case PluginMethods.GET_CURRENT_PAGE_NODES:
        await handleGetCurrentPageNodes(params, id);
        break;

      case PluginMethods.SEARCH_NODES:
        await handleSearchNodes(params, id);
        break;

      case PluginMethods.GET_ALL_PAGES:
        await handleGetAllPages(params, id);
        break;

      case PluginMethods.SWITCH_PAGE:
        await handleSwitchPage(params, id);
        break;

      case PluginMethods.CREATE_PAGE:
        await handleCreatePage(params, id);
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
