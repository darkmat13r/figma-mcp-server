/**
 * Figma Plugin Variable & Token Handlers
 *
 * ## Purpose
 * Handles all variable and design token operations for Category 6: Variable & Token Tools
 *
 * ### Variable Tools (6)
 * - createVariableCollection: Create variable collection with modes
 * - createVariable: Create variable in collection
 * - bindVariable: Bind variable to node property
 * - getVariables: List variables with filters
 * - setVariableValue: Update variable value for mode
 * - unbindVariable: Remove variable binding
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function handles one specific variable operation
 * - Open-Closed: New variable operations can be added without modifying existing ones
 */

import { ParamNames, ErrorMessages } from './constants';

// ============================================================================
// TYPES
// ============================================================================

interface VariableCollectionInfo {
  collectionId: string;
  name: string;
  modes: Array<{ modeId: string; name: string }>;
}

interface VariableInfo {
  variableId: string;
  name: string;
  type: string;
  collectionId: string;
  valuesByMode: Record<string, any>;
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
 * Get variable collection by ID
 */
async function getVariableCollection(collectionId: string): Promise<VariableCollection> {
  const collection = await figma.variables.getVariableCollectionByIdAsync(collectionId);
  if (!collection) {
    throw new Error(`Variable collection not found: ${collectionId}`);
  }
  return collection;
}

/**
 * Get variable by ID
 */
async function getVariable(variableId: string): Promise<Variable> {
  const variable = await figma.variables.getVariableByIdAsync(variableId);
  if (!variable) {
    throw new Error(`Variable not found: ${variableId}`);
  }
  return variable;
}

/**
 * Serialize variable info
 */
function serializeVariable(variable: Variable): VariableInfo {
  return {
    variableId: variable.id,
    name: variable.name,
    type: variable.resolvedType,
    collectionId: variable.variableCollectionId,
    valuesByMode: variable.valuesByMode,
  };
}

/**
 * Serialize variable collection info
 */
function serializeVariableCollection(collection: VariableCollection): VariableCollectionInfo {
  return {
    collectionId: collection.id,
    name: collection.name,
    modes: collection.modes.map((mode) => ({
      modeId: mode.modeId,
      name: mode.name,
    })),
  };
}

/**
 * Convert color value to RGBA format if needed
 */
function normalizeColorValue(value: any): RGB | RGBA {
  if (typeof value === 'object' && value !== null) {
    // Check if it's already in RGB/RGBA format
    if ('r' in value && 'g' in value && 'b' in value) {
      return value;
    }
  }
  throw new Error('Invalid color value format. Expected {r, g, b, a?}');
}

// ============================================================================
// VARIABLE HANDLERS
// ============================================================================

/**
 * Handle createVariableCollection command (6.1)
 * Create a variable collection with optional modes
 */
export async function handleCreateVariableCollection(
  params: Record<string, any>
): Promise<VariableCollectionInfo> {
  const name = params[ParamNames.NAME];
  const modes = params[ParamNames.MODES];

  if (!name) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NAME));
  }

  // Create the variable collection
  const collection = figma.variables.createVariableCollection(name);

  // Add additional modes if specified
  const addedModes: string[] = [];
  const skippedModes: string[] = [];

  if (modes && Array.isArray(modes) && modes.length > 0) {
    // First, rename the default mode to the first mode name
    const firstMode = collection.modes[0];
    if (firstMode && modes[0] && typeof modes[0] === 'string') {
      collection.renameMode(firstMode.modeId, modes[0]);
      addedModes.push(modes[0]);
    }

    // Then add the remaining modes (starting from index 1)
    for (let i = 1; i < modes.length; i++) {
      const modeName = modes[i];
      if (typeof modeName === 'string' && modeName.trim().length > 0) {
        try {
          collection.addMode(modeName);
          addedModes.push(modeName);
        } catch (error) {
          const errorMsg = error instanceof Error ? error.message : String(error);
          // If it's a pricing tier limitation, log warning and continue
          if (errorMsg.includes('Limited to')) {
            console.warn(`[VariableHandlers] Skipped mode "${modeName}": ${errorMsg}`);
            skippedModes.push(modeName);
            // Stop trying to add more modes since we hit the limit
            break;
          }
          // For other errors, throw
          throw error;
        }
      }
    }
  }

  console.log('[VariableHandlers] Created variable collection:', collection.id);
  if (skippedModes.length > 0) {
    console.warn(
      `[VariableHandlers] Could not add ${skippedModes.length} mode(s) due to pricing tier limits: ${skippedModes.join(', ')}`
    );
  }

  return serializeVariableCollection(collection);
}

/**
 * Handle createVariable command (6.2)
 * Create a variable in a collection
 */
export async function handleCreateVariable(params: Record<string, any>): Promise<VariableInfo> {
  const collectionId = params[ParamNames.COLLECTION_ID];
  const name = params[ParamNames.NAME];
  const type = params[ParamNames.TYPE];
  const values = params[ParamNames.VALUES]; // { modeId: value }
  const defaultValue = params[ParamNames.DEFAULT_VALUE];

  if (!collectionId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.COLLECTION_ID));
  }
  if (!name) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NAME));
  }
  if (!type) {
    throw new Error(ErrorMessages.missingParam(ParamNames.TYPE));
  }

  const collection = await getVariableCollection(collectionId);

  // Variable types: BOOLEAN, FLOAT, STRING, COLOR
  let variableType: VariableResolvedDataType;
  switch (type.toUpperCase()) {
    case 'BOOLEAN':
      variableType = 'BOOLEAN';
      break;
    case 'FLOAT':
      variableType = 'FLOAT';
      break;
    case 'STRING':
      variableType = 'STRING';
      break;
    case 'COLOR':
      variableType = 'COLOR';
      break;
    default:
      throw new Error(`Unknown variable type: ${type}. Must be BOOLEAN, FLOAT, STRING, or COLOR`);
  }

  // Create the variable
  const variable = figma.variables.createVariable(name, collection, variableType);

  // Set values for each mode
  if (values && typeof values === 'object') {
    for (const [modeId, value] of Object.entries(values)) {
      let normalizedValue: VariableValue = value as VariableValue;

      // Normalize color values
      if (variableType === 'COLOR') {
        normalizedValue = normalizeColorValue(value) as VariableValue;
      }

      variable.setValueForMode(modeId, normalizedValue);
    }
  } else if (defaultValue !== undefined) {
    // Set default value for all modes
    const defaultModeId = collection.modes[0].modeId;
    let normalizedValue = defaultValue;

    if (variableType === 'COLOR') {
      normalizedValue = normalizeColorValue(defaultValue);
    }

    variable.setValueForMode(defaultModeId, normalizedValue);
  }

  console.log('[VariableHandlers] Created variable:', variable.id);

  return serializeVariable(variable);
}

/**
 * Handle bindVariable command (6.3)
 * Bind a variable to a node property
 */
export async function handleBindVariable(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const variableId = params[ParamNames.VARIABLE_ID];
  const field = params[ParamNames.FIELD];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!variableId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VARIABLE_ID));
  }
  if (!field) {
    throw new Error(ErrorMessages.missingParam(ParamNames.FIELD));
  }

  const node = await getNode(nodeId);
  const variable = await getVariable(variableId);

  // Bind the variable to the field
  // Common fields: "fills", "strokes", "opacity", "width", "height", "layoutGrids", etc.
  try {
    // Type-cast to work around the strict type checking
    const bindableNode = node as any;

    // Check if node supports setBoundVariable
    if (!bindableNode.setBoundVariable || typeof bindableNode.setBoundVariable !== 'function') {
      throw new Error(`Node type ${node.type} does not support variable binding`);
    }

    bindableNode.setBoundVariable(field, variable);
    console.log('[VariableHandlers] Bound variable:', variableId, 'to field:', field, 'on node:', nodeId);
  } catch (error) {
    throw new Error(
      `Failed to bind variable to field '${field}': ${error instanceof Error ? error.message : String(error)}`
    );
  }
}

/**
 * Handle getVariables command (6.4)
 * List variables with optional filters
 */
export async function handleGetVariables(
  params: Record<string, any>
): Promise<{ variables: VariableInfo[] }> {
  const collectionId = params[ParamNames.COLLECTION_ID];
  const type = params[ParamNames.TYPE];

  // Get all local variables
  const allVariables = await figma.variables.getLocalVariablesAsync();

  // Filter variables
  let filteredVariables = allVariables;

  // Filter by collection
  if (collectionId) {
    filteredVariables = filteredVariables.filter((v) => v.variableCollectionId === collectionId);
  }

  // Filter by type
  if (type) {
    const normalizedType = type.toUpperCase();
    filteredVariables = filteredVariables.filter((v) => v.resolvedType === normalizedType);
  }

  console.log('[VariableHandlers] Retrieved', filteredVariables.length, 'variables');

  return {
    variables: filteredVariables.map(serializeVariable),
  };
}

/**
 * Handle setVariableValue command (6.5)
 * Update a variable's value for a specific mode
 */
export async function handleSetVariableValue(params: Record<string, any>): Promise<void> {
  const variableId = params[ParamNames.VARIABLE_ID];
  const modeId = params[ParamNames.MODE_ID];
  const value = params[ParamNames.VALUE];

  if (!variableId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VARIABLE_ID));
  }
  if (!modeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.MODE_ID));
  }
  if (value === undefined) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VALUE));
  }

  const variable = await getVariable(variableId);

  // Normalize value based on variable type
  let normalizedValue = value;
  if (variable.resolvedType === 'COLOR') {
    normalizedValue = normalizeColorValue(value);
  }

  // Set the value for the mode
  variable.setValueForMode(modeId, normalizedValue);

  console.log('[VariableHandlers] Set variable value for:', variableId, 'mode:', modeId);
}

/**
 * Handle unbindVariable command (6.6)
 * Remove variable binding from node property
 */
export async function handleUnbindVariable(params: Record<string, any>): Promise<void> {
  const nodeId = params[ParamNames.NODE_ID];
  const field = params[ParamNames.FIELD];

  if (!nodeId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.NODE_ID));
  }
  if (!field) {
    throw new Error(ErrorMessages.missingParam(ParamNames.FIELD));
  }

  const node = await getNode(nodeId);

  // Unbind by setting to null
  try {
    // Type-cast to work around the strict type checking
    const bindableNode = node as any;

    // Check if node supports setBoundVariable
    if (!bindableNode.setBoundVariable || typeof bindableNode.setBoundVariable !== 'function') {
      throw new Error(`Node type ${node.type} does not support variable binding`);
    }

    bindableNode.setBoundVariable(field, null);
    console.log('[VariableHandlers] Unbound variable from field:', field, 'on node:', nodeId);
  } catch (error) {
    throw new Error(
      `Failed to unbind variable from field '${field}': ${error instanceof Error ? error.message : String(error)}`
    );
  }
}
