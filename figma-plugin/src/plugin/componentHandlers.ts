/**
 * Figma Plugin Component & Variant Handlers
 *
 * ## Purpose
 * Handles all component and variant operations for Category 5: Component & Variant Tools
 *
 * ### Component Tools (7)
 * - createComponentFromNode: Convert node to component
 * - setComponentProperties: Define component properties
 * - setInstanceProperties: Override instance properties
 * - detachInstance: Break instance link to component
 * - swapInstance: Swap to different component
 * - createComponentSet: Create variant set
 * - addVariant: Add variant to component set
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific component operation
 * - Open-Closed: New component operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// TYPES
// ============================================================================

interface ComponentInfo {
  componentId: string;
  name: string;
  type: string;
}

interface VariantInfo {
  variantId: string;
  name: string;
  properties: Record<string, string>;
}

interface ComponentSetInfo {
  componentSetId: string;
  name: string;
  variantCount: number;
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
 * Check if node can be converted to a component
 * Only certain node types support createComponent()
 */
function canBeComponent(node: SceneNode): node is FrameNode | GroupNode | ComponentNode {
  return (
    node.type === 'FRAME' ||
    node.type === 'GROUP' ||
    node.type === 'COMPONENT' ||
    node.type === 'INSTANCE' ||
    node.type === 'RECTANGLE' ||
    node.type === 'ELLIPSE' ||
    node.type === 'POLYGON' ||
    node.type === 'STAR' ||
    node.type === 'VECTOR' ||
    node.type === 'TEXT' ||
    node.type === 'LINE'
  );
}

/**
 * Ensure node is a component
 */
function ensureComponent(node: SceneNode): ComponentNode {
  if (node.type !== 'COMPONENT') {
    throw new Error('Node must be a COMPONENT node');
  }
  return node as ComponentNode;
}

/**
 * Ensure node is an instance
 */
function ensureInstance(node: SceneNode): InstanceNode {
  if (node.type !== 'INSTANCE') {
    throw new Error('Node must be an INSTANCE node');
  }
  return node as InstanceNode;
}

/**
 * Ensure node is a component set
 */
function ensureComponentSet(node: SceneNode): ComponentSetNode {
  if (node.type !== 'COMPONENT_SET') {
    throw new Error('Node must be a COMPONENT_SET node');
  }
  return node as ComponentSetNode;
}

// ============================================================================
// COMPONENT HANDLERS
// ============================================================================

/**
 * Handle createComponentFromNode command (5.1)
 * Convert a node to a component
 */
export async function handleCreateComponentFromNode(
  params: Record<string, any>
): Promise<ComponentInfo> {
  const nodeId = params[ParamNames.NODE_ID];
  const name = params[ParamNames.NAME];
  const description = params[ParamNames.DESCRIPTION];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }

  const node = await getNode(nodeId);

  // Check if node can be converted to component
  if (!canBeComponent(node)) {
    throw new Error(`Node type ${node.type} cannot be converted to a component`);
  }

  // Create component from node
  let component: ComponentNode;

  if (node.type === 'COMPONENT') {
    // Already a component
    component = node as ComponentNode;
  } else {
    // For Frame nodes, use figma.createComponentFromNode
    // For other node types, wrap in a frame first then convert
    if (node.type === 'FRAME' || node.type === 'GROUP') {
      // Frames and groups can be directly converted
      component = figma.createComponentFromNode(node as FrameNode | GroupNode);
    } else {
      // For other node types, wrap in a frame first
      const frame = figma.createFrame();

      // Type-safe property access
      const nodeWithProps = node as any;
      frame.name = nodeWithProps.name || 'Component';
      frame.x = 'x' in node ? nodeWithProps.x : 0;
      frame.y = 'y' in node ? nodeWithProps.y : 0;
      frame.resize(
        'width' in node ? nodeWithProps.width : 100,
        'height' in node ? nodeWithProps.height : 100
      );

      // Move node into frame
      const parent = nodeWithProps.parent;
      frame.appendChild(node);
      if (parent && 'appendChild' in parent) {
        (parent as any).appendChild(frame);
      }

      // Create component from frame
      component = figma.createComponentFromNode(frame);
    }
  }

  // Set name if provided
  if (name) {
    component.name = name;
  }

  // Set description if provided
  if (description) {
    component.description = description;
  }

  console.log('[ComponentHandlers] Created component:', component.id);

  return {
    componentId: component.id,
    name: component.name,
    type: component.type,
  };
}

/**
 * Handle setComponentProperties command (5.2)
 * Define component properties for variants and instance swaps
 */
export async function handleSetComponentProperties(params: Record<string, any>): Promise<void> {
  const componentId = params[ParamNames.COMPONENT_ID];
  const properties = params[ParamNames.PROPERTIES];

  if (!componentId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.COMPONENT_ID));
  }
  if (!properties || typeof properties !== 'object') {
    throw new Error(ErrorMessages.missingParam(ParamNames.PROPERTIES));
  }

  const node = await getNode(componentId);
  const component = ensureComponent(node);

  // Add each property to the component
  for (const [propName, propConfig] of Object.entries(properties)) {
    const config = propConfig as any;
    const propType = config.type;
    const defaultValue = config.defaultValue;
    const variantOptions = config.variantOptions;

    if (!propType) {
      throw new Error(`Property '${propName}' must have a type`);
    }

    // Component property types: BOOLEAN, TEXT, INSTANCE_SWAP, VARIANT
    switch (propType) {
      case 'BOOLEAN':
        component.addComponentProperty(propName, 'BOOLEAN', defaultValue ?? false);
        break;

      case 'TEXT':
        component.addComponentProperty(propName, 'TEXT', defaultValue ?? '');
        break;

      case 'INSTANCE_SWAP':
        // For INSTANCE_SWAP, defaultValue should be a component ID or key
        component.addComponentProperty(propName, 'INSTANCE_SWAP', defaultValue ?? '');
        break;

      case 'VARIANT':
        // For VARIANT, we need variant options
        if (!variantOptions || !Array.isArray(variantOptions)) {
          throw new Error(`Property '${propName}' of type VARIANT requires variantOptions array`);
        }
        // VARIANT properties don't use the options parameter in the current API
        component.addComponentProperty(propName, 'VARIANT', defaultValue ?? variantOptions[0]);
        break;

      default:
        throw new Error(`Unknown component property type: ${propType}`);
    }
  }

  console.log('[ComponentHandlers] Set component properties for:', componentId);
}

/**
 * Handle setInstanceProperties command (5.3)
 * Override instance properties
 */
export async function handleSetInstanceProperties(params: Record<string, any>): Promise<void> {
  const instanceId = params[ParamNames.INSTANCE_ID];
  const properties = params[ParamNames.PROPERTIES];

  if (!instanceId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.INSTANCE_ID));
  }
  if (!properties || typeof properties !== 'object') {
    throw new Error(ErrorMessages.missingParam(ParamNames.PROPERTIES));
  }

  const node = await getNode(instanceId);
  const instance = ensureInstance(node);

  // Set properties on instance
  instance.setProperties(properties);

  console.log('[ComponentHandlers] Set instance properties for:', instanceId);
}

/**
 * Handle detachInstance command (5.4)
 * Break instance link to component
 */
export async function handleDetachInstance(params: Record<string, any>): Promise<{ nodeId: string }> {
  const instanceId = params[ParamNames.INSTANCE_ID];

  if (!instanceId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.INSTANCE_ID));
  }

  const node = await getNode(instanceId);
  const instance = ensureInstance(node);

  // Detach instance
  const detached = instance.detachInstance();

  console.log('[ComponentHandlers] Detached instance:', instanceId, '-> Frame:', detached.id);

  return {
    nodeId: detached.id,
  };
}

/**
 * Handle swapInstance command (5.5)
 * Swap instance to different component
 */
export async function handleSwapInstance(params: Record<string, any>): Promise<void> {
  const instanceId = params[ParamNames.INSTANCE_ID];
  const newComponentId = params[ParamNames.NEW_COMPONENT_ID];

  if (!instanceId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.INSTANCE_ID));
  }
  if (!newComponentId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NEW_COMPONENT_ID));
  }

  const instance = await getNode(instanceId);
  const instanceNode = ensureInstance(instance);

  const newComponent = await getNode(newComponentId);
  const newComponentNode = ensureComponent(newComponent);

  // Swap to new component
  instanceNode.swapComponent(newComponentNode);

  console.log('[ComponentHandlers] Swapped instance:', instanceId, 'to component:', newComponentId);
}

/**
 * Handle createComponentSet command (5.6)
 * Create a variant set from multiple components
 */
export async function handleCreateComponentSet(
  params: Record<string, any>
): Promise<ComponentSetInfo> {
  const componentIds = params[ParamNames.COMPONENT_IDS];
  const name = params[ParamNames.NAME];

  if (!componentIds || !Array.isArray(componentIds) || componentIds.length === 0) {
    throw new Error(ErrorMessages.missingParam(ParamNames.COMPONENT_IDS));
  }

  // Get all components
  const components: ComponentNode[] = [];
  for (const componentId of componentIds) {
    const node = await getNode(componentId);
    const component = ensureComponent(node);
    components.push(component);
  }

  // Combine as variants
  const componentSet = figma.combineAsVariants(components, figma.currentPage);

  // Set name if provided
  if (name) {
    componentSet.name = name;
  }

  console.log('[ComponentHandlers] Created component set:', componentSet.id);

  return {
    componentSetId: componentSet.id,
    name: componentSet.name,
    variantCount: components.length,
  };
}

/**
 * Handle addVariant command (5.7)
 * Add a new variant to a component set
 */
export async function handleAddVariant(params: Record<string, any>): Promise<VariantInfo> {
  const componentSetId = params[ParamNames.COMPONENT_SET_ID];
  const properties = params[ParamNames.PROPERTIES];
  const name = params[ParamNames.NAME];

  if (!componentSetId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.COMPONENT_SET_ID));
  }

  const node = await getNode(componentSetId);
  const componentSet = ensureComponentSet(node);

  // Get default variant to clone
  if (componentSet.children.length === 0) {
    throw new Error('Component set has no variants to clone from');
  }

  const defaultVariant = componentSet.defaultVariant;
  if (!defaultVariant) {
    throw new Error('Component set has no default variant');
  }

  // Clone the default variant
  const newVariant = defaultVariant.clone();

  // Add to component set
  componentSet.appendChild(newVariant);

  // Set variant properties if provided
  if (properties && typeof properties === 'object') {
    // Variant properties are key-value pairs like { "Size": "Large", "Type": "Primary" }
    for (const [key, value] of Object.entries(properties)) {
      // Set variant property through the component set
      // Note: This sets the variant's name in the format "Property1=Value1, Property2=Value2"
      const variantProps = { ...defaultVariant.variantProperties, [key]: String(value) };
      newVariant.name = Object.entries(variantProps)
        .map(([k, v]) => `${k}=${v}`)
        .join(', ');
    }
  }

  // Set custom name if provided (will override variant property name)
  if (name) {
    newVariant.name = name;
  }

  console.log('[ComponentHandlers] Added variant:', newVariant.id, 'to component set:', componentSetId);

  return {
    variantId: newVariant.id,
    name: newVariant.name,
    properties: newVariant.variantProperties || {},
  };
}
