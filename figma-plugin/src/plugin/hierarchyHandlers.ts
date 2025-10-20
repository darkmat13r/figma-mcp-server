/**
 * Figma Plugin Hierarchy & Query Handlers
 *
 * ## Purpose
 * Handles all hierarchy and query operations for Category 7: Hierarchy & Query Tools
 *
 * ### Core Hierarchy Tools (8)
 * - getNodeInfo: Get detailed node information
 * - getSelection: Get currently selected nodes
 * - setSelection: Change selected nodes
 * - findNodes: Search for nodes by criteria
 * - getChildren: Get node's children
 * - getParent: Get node's parent
 * - moveNode: Move node to different parent
 * - cloneNode: Duplicate node with optional deep clone
 *
 * ### Additional Page & Search Tools (5)
 * - getCurrentPageNodes: Get all top-level nodes on current page
 * - searchNodes: Advanced search by text, ID, or component instance
 * - getAllPages: Get list of all pages
 * - switchPage: Switch to different page
 * - createPage: Create a new page
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific hierarchy operation
 * - Open-Closed: New hierarchy operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// TYPES
// ============================================================================

interface NodeInfo {
  id: string;
  name: string;
  type: string;
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  rotation?: number;
  visible?: boolean;
  locked?: boolean;
  opacity?: number;
  blendMode?: string;
  layoutMode?: string;
  layoutAlign?: string;
  constraints?: any;
  fills?: any;
  strokes?: any;
  effects?: any;
  cornerRadius?: any;
  characters?: string;
  fontSize?: number;
  fontFamily?: string;
  fontStyle?: string;
  textAlignHorizontal?: string;
  textAlignVertical?: string;
  parentId?: string;
  childrenIds?: string[];
}

interface PageInfo {
  id: string;
  name: string;
  type: string;
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
 * Serialize a node into a comprehensive NodeInfo object
 */
function serializeNode(node: BaseNode): NodeInfo {
  const info: NodeInfo = {
    id: node.id,
    name: node.name,
    type: node.type,
  };

  // Add parent ID
  if (node.parent) {
    info.parentId = node.parent.id;
  }

  // Add geometric properties
  if ('x' in node && typeof node.x === 'number') {
    info.x = node.x;
  }
  if ('y' in node && typeof node.y === 'number') {
    info.y = node.y;
  }
  if ('width' in node && typeof node.width === 'number') {
    info.width = node.width;
  }
  if ('height' in node && typeof node.height === 'number') {
    info.height = node.height;
  }
  if ('rotation' in node && typeof node.rotation === 'number') {
    info.rotation = node.rotation;
  }

  // Add visibility and lock properties
  if ('visible' in node) {
    info.visible = node.visible;
  }
  if ('locked' in node) {
    info.locked = node.locked;
  }

  // Add opacity and blend mode
  if ('opacity' in node && typeof node.opacity === 'number') {
    info.opacity = node.opacity;
  }
  if ('blendMode' in node) {
    info.blendMode = (node as any).blendMode;
  }

  // Add layout properties (for frames/components)
  if ('layoutMode' in node) {
    info.layoutMode = (node as any).layoutMode;
  }
  if ('layoutAlign' in node) {
    info.layoutAlign = (node as any).layoutAlign;
  }
  if ('constraints' in node) {
    info.constraints = (node as any).constraints;
  }

  // Add style properties
  if ('fills' in node) {
    info.fills = (node as any).fills;
  }
  if ('strokes' in node) {
    info.strokes = (node as any).strokes;
  }
  if ('effects' in node) {
    info.effects = (node as any).effects;
  }
  if ('cornerRadius' in node) {
    info.cornerRadius = (node as any).cornerRadius;
  }

  // Add text properties (for text nodes)
  if (node.type === 'TEXT') {
    const textNode = node as TextNode;
    info.characters = textNode.characters;

    const fontName = textNode.fontName;
    if (typeof fontName !== 'symbol') {
      info.fontSize = textNode.fontSize as number;
      info.fontFamily = fontName.family;
      info.fontStyle = fontName.style;
    }

    info.textAlignHorizontal = textNode.textAlignHorizontal;
    info.textAlignVertical = textNode.textAlignVertical;
  }

  // Add children IDs (avoid deep serialization)
  if ('children' in node) {
    info.childrenIds = (node as any).children.map((child: BaseNode) => child.id);
  }

  return info;
}

/**
 * Check if node matches the given criteria
 */
function matchesNodeCriteria(
  node: BaseNode,
  nameRegex?: string,
  types?: string[],
): boolean {
  // Check type filter
  if (types && types.length > 0 && !types.includes(node.type)) {
    return false;
  }

  // Check name regex
  if (nameRegex) {
    try {
      const regex = new RegExp(nameRegex, 'i');
      if (!regex.test(node.name)) {
        return false;
      }
    } catch (error) {
      // Invalid regex - do exact match instead
      if (!node.name.toLowerCase().includes(nameRegex.toLowerCase())) {
        return false;
      }
    }
  }

  return true;
}

/**
 * Recursively find nodes matching criteria
 */
function findNodesRecursive(
  parent: BaseNode & ChildrenMixin,
  nameRegex?: string,
  types?: string[],
  results: BaseNode[] = [],
): BaseNode[] {
  if ('children' in parent) {
    for (const child of parent.children) {
      if (matchesNodeCriteria(child, nameRegex, types)) {
        results.push(child);
      }

      if ('children' in child) {
        findNodesRecursive(child, nameRegex, types, results);
      }
    }
  }

  return results;
}

/**
 * Search nodes by text content, ID, or component instance
 */
function searchNodesRecursive(
  parent: BaseNode & ChildrenMixin,
  query: string,
  searchInText: boolean,
  searchInNames: boolean,
  results: BaseNode[] = [],
): BaseNode[] {
  const queryLower = query.toLowerCase();

  if ('children' in parent) {
    for (const child of parent.children) {
      let matches = false;

      // Search by ID
      if (child.id.toLowerCase().includes(queryLower)) {
        matches = true;
      }

      // Search by name
      if (searchInNames && child.name.toLowerCase().includes(queryLower)) {
        matches = true;
      }

      // Search in text content
      if (searchInText && child.type === 'TEXT') {
        const textNode = child as TextNode;
        if (textNode.characters.toLowerCase().includes(queryLower)) {
          matches = true;
        }
      }

      // Search for component instances
      if (child.type === 'INSTANCE') {
        const instance = child as InstanceNode;
        if (instance.mainComponent && instance.mainComponent.name.toLowerCase().includes(queryLower)) {
          matches = true;
        }
      }

      if (matches) {
        results.push(child);
      }

      if ('children' in child) {
        searchNodesRecursive(child, query, searchInText, searchInNames, results);
      }
    }
  }

  return results;
}

// ============================================================================
// HIERARCHY HANDLERS - CORE TOOLS (8)
// ============================================================================

/**
 * Handle getNodeInfo command (7.1)
 * Get detailed node information including all properties
 */
export async function handleGetNodeInfo(params: Record<string, any>): Promise<NodeInfo> {
  const nodeId = params[ParamNames.NODE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);
  return serializeNode(node);
}

/**
 * Handle getSelection command (7.2)
 * Get currently selected nodes
 */
export async function handleGetSelection(_params: Record<string, any>): Promise<NodeInfo[]> {
  const selection = figma.currentPage.selection;
  return selection.map(serializeNode);
}

/**
 * Handle setSelection command (7.3)
 * Change selected nodes
 */
export async function handleSetSelection(params: Record<string, any>): Promise<void> {
  const nodeIds = params[ParamNames.NODE_IDS];

  if (!nodeIds || !Array.isArray(nodeIds)) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_IDS));
  }

  const nodes: SceneNode[] = [];
  for (const nodeId of nodeIds) {
    const node = await getNode(nodeId);
    if ('x' in node) {
      // Only SceneNodes can be selected
      nodes.push(node as SceneNode);
    }
  }

  figma.currentPage.selection = nodes;
  console.log('[HierarchyHandlers] Selection set to', nodes.length, 'nodes');
}

/**
 * Handle findNodes command (7.4)
 * Search for nodes by criteria (name regex, type, parentId, recursive)
 */
export async function handleFindNodes(params: Record<string, any>): Promise<NodeInfo[]> {
  const nameRegex = params.nameRegex;
  const type = params.type;
  const types = params.types;
  const parentId = params.parentId;
  const recursive = params.recursive !== false; // Default to true

  // Normalize types to array
  let typeFilter: string[] | undefined;
  if (type) {
    typeFilter = [type];
  } else if (types && Array.isArray(types)) {
    typeFilter = types;
  }

  // Get parent node
  let parent: BaseNode & ChildrenMixin;
  if (parentId) {
    const node = await getNode(parentId);
    if (!('children' in node)) {
      throw new Error('Parent node does not have children');
    }
    parent = node as BaseNode & ChildrenMixin;
  } else {
    parent = figma.currentPage;
  }

  // Find nodes
  const results: BaseNode[] = [];

  if (recursive) {
    findNodesRecursive(parent, nameRegex, typeFilter, results);
  } else {
    // Non-recursive - only direct children
    if ('children' in parent) {
      for (const child of parent.children) {
        if (matchesNodeCriteria(child, nameRegex, typeFilter)) {
          results.push(child);
        }
      }
    }
  }

  return results.map(serializeNode);
}

/**
 * Handle getChildren command (7.5)
 * Get node's children (with optional recursive)
 */
export async function handleGetChildren(params: Record<string, any>): Promise<NodeInfo[]> {
  const nodeId = params[ParamNames.NODE_ID];
  const recursive = params.recursive === true; // Default to false

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  if (!('children' in node)) {
    throw new Error('Node does not have children');
  }

  const results: BaseNode[] = [];

  if (recursive) {
    // Get all descendants
    findNodesRecursive(node as BaseNode & ChildrenMixin, undefined, undefined, results);
  } else {
    // Get only direct children
    results.push(...(node as any).children);
  }

  return results.map(serializeNode);
}

/**
 * Handle getParent command (7.6)
 * Get node's parent
 */
export async function handleGetParent(params: Record<string, any>): Promise<NodeInfo | null> {
  const nodeId = params[ParamNames.NODE_ID];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  if (!node.parent) {
    return null;
  }

  return serializeNode(node.parent);
}

/**
 * Handle moveNode command (7.7)
 * Move node to different parent
 */
export async function handleMoveNode(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const newParentId = params[ParamNames.NEW_PARENT_ID];
  const index = params[ParamNames.INDEX];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!newParentId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NEW_PARENT_ID));
  }

  const node = await getNode(nodeId);
  const newParent = await getNode(newParentId);

  if (!('children' in newParent)) {
    throw new Error('Target parent node does not support children');
  }

  // Move node to new parent at specified index or end
  if (index !== undefined && typeof index === 'number') {
    // Use insertChild to place node at specific index
    (newParent as any).insertChild(index, node);
  } else {
    // Use appendChild to place node at end
    (newParent as any).appendChild(node);
  }

  console.log('[HierarchyHandlers] Moved node', nodeId, 'to parent', newParentId, index !== undefined ? `at index ${index}` : 'at end');
}

/**
 * Handle cloneNode command (7.8)
 * Duplicate node with optional deep clone
 */
export async function handleCloneNode(params: Record<string, any>): Promise<NodeInfo> {
  const nodeId = params[ParamNames.NODE_ID];
  const deep = params.deep !== false; // Default to true (deep clone)

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  // Clone the node
  const clone = node.clone();

  // If not deep and node has children, remove them
  if (!deep && 'children' in clone) {
    const children = [...(clone as any).children];
    for (const child of children) {
      child.remove();
    }
  }

  console.log('[HierarchyHandlers] Cloned node', nodeId, 'deep:', deep);

  return serializeNode(clone);
}

// ============================================================================
// HIERARCHY HANDLERS - PAGE & SEARCH TOOLS (5)
// ============================================================================

/**
 * Handle getCurrentPageNodes command (7.9)
 * Get all top-level nodes on current page
 */
export async function handleGetCurrentPageNodes(_params: Record<string, any>): Promise<NodeInfo[]> {
  const topLevelNodes = figma.currentPage.children;
  return topLevelNodes.map(serializeNode);
}

/**
 * Handle searchNodes command (7.10)
 * Advanced search by text, ID, or component instance
 */
export async function handleSearchNodes(params: Record<string, any>): Promise<NodeInfo[]> {
  const query = params.query;
  const searchInText = params.searchInText !== false; // Default to true
  const searchInNames = params.searchInNames !== false; // Default to true
  const currentPageOnly = params.currentPageOnly !== false; // Default to true

  if (!query || typeof query !== 'string') {
    throw new Error(ErrorMessages.missingParam('query'));
  }

  const results: BaseNode[] = [];

  if (currentPageOnly) {
    // Search only current page
    searchNodesRecursive(figma.currentPage, query, searchInText, searchInNames, results);
  } else {
    // Search all pages
    for (const page of figma.root.children) {
      searchNodesRecursive(page, query, searchInText, searchInNames, results);
    }
  }

  return results.map(serializeNode);
}

/**
 * Handle getAllPages command (7.11)
 * Get list of all pages
 */
export async function handleGetAllPages(_params: Record<string, any>): Promise<PageInfo[]> {
  const pages = figma.root.children;
  return pages.map((page) => ({
    id: page.id,
    name: page.name,
    type: page.type,
  }));
}

/**
 * Handle switchPage command (7.12)
 * Switch to different page
 */
export async function handleSwitchPage(params: Record<string, any>): Promise<void> {
  const pageId = params.pageId;

  if (!pageId) {
    throw new Error(ErrorMessages.missingParam('pageId'));
  }

  const page = await figma.getNodeByIdAsync(pageId);
  if (!page) {
    throw new Error('Page not found');
  }

  if (page.type !== 'PAGE') {
    throw new Error('Node is not a page');
  }

  figma.currentPage = page as PageNode;
  console.log('[HierarchyHandlers] Switched to page:', page.name);
}

/**
 * Handle createPage command (7.13)
 * Create a new page
 */
export async function handleCreatePage(params: Record<string, any>): Promise<PageInfo> {
  const name = params[ParamNames.NAME];
  const switchTo = params.switchTo !== false; // Default to true

  const page = figma.createPage();

  if (name) {
    page.name = name;
  }

  if (switchTo) {
    figma.currentPage = page;
  }

  console.log('[HierarchyHandlers] Created page:', page.name);

  return {
    id: page.id,
    name: page.name,
    type: page.type,
  };
}
