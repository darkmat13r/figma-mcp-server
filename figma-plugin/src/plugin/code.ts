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
import * as ComponentHandlers from './componentHandlers';
import * as VariableHandlers from './variableHandlers';
import * as ImageHandlers from './imageHandlers';

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
    console.log('[handleCreateNode] Starting with params:', params, 'requestId:', requestId);
    const nodeType = params.type;
    if (!nodeType) {
      throw new Error(ErrorMessages.missingParam('type'));
    }

    // Look up handler in registry
    const handler = nodeCreationHandlers[nodeType];
    if (!handler) {
      throw new Error(`Unknown node type: ${nodeType}`);
    }

    console.log('[handleCreateNode] Found handler for type:', nodeType);
    // Execute handler
    const node = await handler(params);
    console.log('[handleCreateNode] Handler completed, node created:', node.id);

    // Send success response
    if (requestId) {
      console.log('[handleCreateNode] Sending success response for requestId:', requestId);
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
    console.log('[handleCreateNode] Response sent successfully');
  } catch (error) {
    console.error('[handleCreateNode] Error occurred:', error);
    const errorMessage = error instanceof Error ? error.message : String(error);
    if (requestId) {
      console.log('[handleCreateNode] Sending error response for requestId:', requestId);
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
      message: `Successfully moved node ${params.nodeId} to parent ${params.newParentId}`,
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
 * Handle createComponentFromNode command (Category 5: Component Tools)
 */
async function handleCreateComponentFromNode(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ComponentHandlers.handleCreateComponentFromNode(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle setComponentProperties command (Category 5: Component Tools)
 */
async function handleSetComponentProperties(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await ComponentHandlers.handleSetComponentProperties(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set component properties for: ${params.componentId}`,
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
 * Handle setInstanceProperties command (Category 5: Component Tools)
 */
async function handleSetInstanceProperties(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await ComponentHandlers.handleSetInstanceProperties(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set instance properties for: ${params.instanceId}`,
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
 * Handle detachInstance command (Category 5: Component Tools)
 */
async function handleDetachInstanceCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ComponentHandlers.handleDetachInstance(params);
    sendWSResponse(requestId, {
      success: true,
      ...result,
      message: `Successfully detached instance: ${params.instanceId}`,
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
 * Handle swapInstance command (Category 5: Component Tools)
 */
async function handleSwapInstanceCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await ComponentHandlers.handleSwapInstance(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully swapped instance ${params.instanceId} to component ${params.newComponentId}`,
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
 * Handle createComponentSet command (Category 5: Component Tools)
 */
async function handleCreateComponentSet(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ComponentHandlers.handleCreateComponentSet(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle addVariant command (Category 5: Component Tools)
 */
async function handleAddVariant(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ComponentHandlers.handleAddVariant(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createVariableCollection command (Category 6: Variable Tools)
 */
async function handleCreateVariableCollection(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await VariableHandlers.handleCreateVariableCollection(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createVariable command (Category 6: Variable Tools)
 */
async function handleCreateVariableCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await VariableHandlers.handleCreateVariable(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle bindVariable command (Category 6: Variable Tools)
 */
async function handleBindVariableCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await VariableHandlers.handleBindVariable(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully bound variable ${params.variableId} to field ${params.field} on node ${params.nodeId}`,
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
 * Handle getVariables command (Category 6: Variable Tools)
 */
async function handleGetVariablesCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await VariableHandlers.handleGetVariables(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle setVariableValue command (Category 6: Variable Tools)
 */
async function handleSetVariableValue(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await VariableHandlers.handleSetVariableValue(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set variable value for: ${params.variableId}`,
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
 * Handle unbindVariable command (Category 6: Variable Tools)
 */
async function handleUnbindVariableCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await VariableHandlers.handleUnbindVariable(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully unbound variable from field ${params.field} on node ${params.nodeId}`,
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
 * Handle createImage command (Category 9: Image & Media Tools)
 */
async function handleCreateImageCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ImageHandlers.handleCreateImage(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle setImageFill command (Category 9: Image & Media Tools)
 */
async function handleSetImageFillCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await ImageHandlers.handleSetImageFill(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully set image fill for node: ${params.nodeId}`,
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
 * Handle exportNode command (Category 9: Image & Media Tools)
 */
async function handleExportNodeCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ImageHandlers.handleExportNode(params);
    // Convert Uint8Array to regular array for JSON serialization
    sendWSResponse(requestId, {
      success: true,
      imageData: Array.from(result.imageData),
      format: result.format,
      width: result.width,
      height: result.height,
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
 * Handle getImageFills command (Category 9: Image & Media Tools)
 */
async function handleGetImageFillsCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await ImageHandlers.handleGetImageFills(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle getUserInfo command (User & File Information)
 */
async function handleGetUserInfoCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await UtilityHandlers.handleGetUserInfo();
    sendWSResponse(requestId, {
      success: true,
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

// ============================================================================
// STYLE MANAGEMENT HANDLERS (Category 11)
// ============================================================================

/**
 * Handle getStyleById command
 */
async function handleGetStyleByIdCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleGetStyleById(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle getLocalPaintStyles command
 */
async function handleGetLocalPaintStylesCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleGetLocalPaintStyles(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle getLocalTextStyles command
 */
async function handleGetLocalTextStylesCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleGetLocalTextStyles(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle getLocalEffectStyles command
 */
async function handleGetLocalEffectStylesCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleGetLocalEffectStyles(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle getLocalGridStyles command
 */
async function handleGetLocalGridStylesCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleGetLocalGridStyles(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createPaintStyle command
 */
async function handleCreatePaintStyleCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleCreatePaintStyle(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createTextStyle command
 */
async function handleCreateTextStyleCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleCreateTextStyle(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createEffectStyle command
 */
async function handleCreateEffectStyleCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleCreateEffectStyle(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle createGridStyle command
 */
async function handleCreateGridStyleCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    const result = await StyleHandlers.handleCreateGridStyle(params);
    sendWSResponse(requestId, {
      success: true,
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
 * Handle setFillStyleId command
 */
async function handleSetFillStyleIdCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    await StyleHandlers.handleSetFillStyleId(params);
    sendWSResponse(requestId, {
      success: true,
      message: `Successfully applied fill style ${params.styleId} to node ${params.nodeId}`,
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
  let commandId: string | undefined;
  try {
    const { id, method, params } = command;
    commandId = id;
    console.log('[handleWSCommand] Received command:', method, 'id:', id, 'params:', params);

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

      // Category 5: Component & Variant Tools
      case PluginMethods.CREATE_COMPONENT_FROM_NODE:
        await handleCreateComponentFromNode(params, id);
        break;

      case PluginMethods.SET_COMPONENT_PROPERTIES:
        await handleSetComponentProperties(params, id);
        break;

      case PluginMethods.SET_INSTANCE_PROPERTIES:
        await handleSetInstanceProperties(params, id);
        break;

      case PluginMethods.DETACH_INSTANCE:
        await handleDetachInstanceCommand(params, id);
        break;

      case PluginMethods.SWAP_INSTANCE:
        await handleSwapInstanceCommand(params, id);
        break;

      case PluginMethods.CREATE_COMPONENT_SET:
        await handleCreateComponentSet(params, id);
        break;

      case PluginMethods.ADD_VARIANT:
        await handleAddVariant(params, id);
        break;

      // Category 6: Variable & Token Tools
      case PluginMethods.CREATE_VARIABLE_COLLECTION:
        await handleCreateVariableCollection(params, id);
        break;

      case PluginMethods.CREATE_VARIABLE:
        await handleCreateVariableCommand(params, id);
        break;

      case PluginMethods.BIND_VARIABLE:
        await handleBindVariableCommand(params, id);
        break;

      case PluginMethods.GET_VARIABLES:
        await handleGetVariablesCommand(params, id);
        break;

      case PluginMethods.SET_VARIABLE_VALUE:
        await handleSetVariableValue(params, id);
        break;

      case PluginMethods.UNBIND_VARIABLE:
        await handleUnbindVariableCommand(params, id);
        break;

      // Category 9: Image & Media Tools
      case PluginMethods.CREATE_IMAGE:
        await handleCreateImageCommand(params, id);
        break;

      case PluginMethods.SET_IMAGE_FILL:
        await handleSetImageFillCommand(params, id);
        break;

      case PluginMethods.EXPORT_NODE:
        await handleExportNodeCommand(params, id);
        break;

      case PluginMethods.GET_IMAGE_FILLS:
        await handleGetImageFillsCommand(params, id);
        break;

      // User & File Information
      case PluginMethods.GET_USER_INFO:
        await handleGetUserInfoCommand(params, id);
        break;

      // Category 11: Style Management Tools
      case PluginMethods.GET_STYLE_BY_ID:
        await handleGetStyleByIdCommand(params, id);
        break;

      case PluginMethods.GET_LOCAL_PAINT_STYLES:
        await handleGetLocalPaintStylesCommand(params, id);
        break;

      case PluginMethods.GET_LOCAL_TEXT_STYLES:
        await handleGetLocalTextStylesCommand(params, id);
        break;

      case PluginMethods.GET_LOCAL_EFFECT_STYLES:
        await handleGetLocalEffectStylesCommand(params, id);
        break;

      case PluginMethods.GET_LOCAL_GRID_STYLES:
        await handleGetLocalGridStylesCommand(params, id);
        break;

      case PluginMethods.CREATE_PAINT_STYLE:
        await handleCreatePaintStyleCommand(params, id);
        break;

      case PluginMethods.CREATE_TEXT_STYLE:
        await handleCreateTextStyleCommand(params, id);
        break;

      case PluginMethods.CREATE_EFFECT_STYLE:
        await handleCreateEffectStyleCommand(params, id);
        break;

      case PluginMethods.CREATE_GRID_STYLE:
        await handleCreateGridStyleCommand(params, id);
        break;

      case PluginMethods.SET_FILL_STYLE_ID:
        await handleSetFillStyleIdCommand(params, id);
        break;

      default:
        sendWSResponse(id, { error: `Unknown method: ${method}` });
    }
  } catch (error) {
    console.error('Error handling WS command:', error);
    // CRITICAL FIX: Always send error response back to server to prevent hanging
    if (commandId) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      sendWSResponse(commandId, {
        success: false,
        error: errorMessage,
      });
    }
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

    case 'get-file-info':
      // Send file ID to UI for MCP URL generation
      // Note: figma.fileKey is only available for cloud-saved files
      // For local files, we generate a unique ID based on the file name and timestamp
      let fileKey = figma.fileKey;

      if (!fileKey) {
        // For local/unsaved files, create a deterministic ID from file name
        const fileName = figma.root.name;
        const sanitized = fileName.toLowerCase().replace(/[^a-z0-9]/g, '-');
        fileKey = `local-${sanitized}`;
        console.warn('File not saved to Figma cloud. Using generated fileKey:', fileKey);
      }

      sendMessage({
        type: 'file-info',
        fileKey: fileKey,
        fileName: figma.root.name,
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
