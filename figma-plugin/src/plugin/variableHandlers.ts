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
 * Supports multiple color formats:
 * - Object: {r: 0-1, g: 0-1, b: 0-1, a?: 0-1}
 * - Hex string: "#RRGGBB" or "#RGB"
 * - RGB array: [r, g, b] where values are 0-255 or 0-1
 */
function normalizeColorValue(value: any): RGB | RGBA {
  // Handle object format {r, g, b, a?}
  if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
    // Check if it's already in RGB/RGBA format
    if ('r' in value && 'g' in value && 'b' in value) {
      const r = value.r;
      const g = value.g;
      const b = value.b;

      // Check if values are in 0-1 range (normalized) or 0-255 range
      const isNormalized = r <= 1 && g <= 1 && b <= 1;

      if (isNormalized) {
        // Already normalized
        return value;
      } else {
        // Convert from 0-255 to 0-1
        if ('a' in value) {
          const normalized: RGBA = {
            r: r / 255,
            g: g / 255,
            b: b / 255,
            a: value.a > 1 ? value.a / 255 : value.a,
          };
          return normalized;
        } else {
          const normalized: RGB = {
            r: r / 255,
            g: g / 255,
            b: b / 255,
          };
          return normalized;
        }
      }
    }
  }

  // Handle hex string format "#RRGGBB" or "#RGB"
  if (typeof value === 'string' && value.startsWith('#')) {
    let hex = value.substring(1);

    // Expand shorthand form (e.g., "03F") to full form (e.g., "0033FF")
    if (hex.length === 3) {
      hex = hex.split('').map(char => char + char).join('');
    }

    if (hex.length === 6) {
      const r = parseInt(hex.substring(0, 2), 16) / 255;
      const g = parseInt(hex.substring(2, 4), 16) / 255;
      const b = parseInt(hex.substring(4, 6), 16) / 255;
      return { r, g, b };
    }
  }

  // Handle array format [r, g, b] or [r, g, b, a]
  if (Array.isArray(value) && (value.length === 3 || value.length === 4)) {
    const [r, g, b, a] = value;

    // Check if values are in 0-1 range (normalized) or 0-255 range
    const isNormalized = r <= 1 && g <= 1 && b <= 1;

    if (isNormalized) {
      if (a !== undefined) {
        return { r, g, b, a } as RGBA;
      } else {
        return { r, g, b } as RGB;
      }
    } else {
      if (a !== undefined) {
        return {
          r: r / 255,
          g: g / 255,
          b: b / 255,
          a: a > 1 ? a / 255 : a,
        } as RGBA;
      } else {
        return {
          r: r / 255,
          g: g / 255,
          b: b / 255,
        } as RGB;
      }
    }
  }

  throw new Error(
    'Invalid color value format. Expected one of:\n' +
    '  - Object: {r: 0-1, g: 0-1, b: 0-1, a?: 0-1}\n' +
    '  - Hex string: "#RRGGBB" or "#RGB"\n' +
    '  - Array: [r, g, b] or [r, g, b, a] where values are 0-255 or 0-1'
  );
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
export async function handleCreateVariable(params: Record<string, any>): Promise<VariableInfo & { valuesSet?: Record<string, boolean>; errors?: string[] }> {
  console.log('[VariableHandlers] handleCreateVariable called with params:', JSON.stringify(params));

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
  console.log('[VariableHandlers] Collection retrieved:', collection.id, 'with modes:', collection.modes.map(m => ({ id: m.modeId, name: m.name })));

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

  console.log('[VariableHandlers] Creating variable with type:', variableType);

  // Create the variable
  const variable = figma.variables.createVariable(name, collection, variableType);
  console.log('[VariableHandlers] Variable created:', variable.id);

  // Track which values were set and any errors
  const valuesSet: Record<string, boolean> = {};
  const errors: string[] = [];

  // Validate mode IDs before setting values
  const validModeIds = new Set(collection.modes.map(m => m.modeId));

  // Set values for each mode
  if (values && typeof values === 'object') {
    console.log('[VariableHandlers] Setting values for modes:', Object.keys(values));

    for (const [modeId, value] of Object.entries(values)) {
      try {
        console.log(`[VariableHandlers] Processing mode ${modeId} with value:`, value);

        // Validate mode ID
        if (!validModeIds.has(modeId)) {
          const errorMsg = `Invalid mode ID: ${modeId}. Valid mode IDs for this collection are: ${Array.from(validModeIds).join(', ')}`;
          console.error('[VariableHandlers]', errorMsg);
          errors.push(errorMsg);
          valuesSet[modeId] = false;
          continue;
        }

        let normalizedValue: VariableValue = value as VariableValue;

        // Normalize color values
        if (variableType === 'COLOR') {
          console.log('[VariableHandlers] Normalizing color value:', value);
          normalizedValue = normalizeColorValue(value) as VariableValue;
          console.log('[VariableHandlers] Normalized color value:', normalizedValue);
        }

        console.log(`[VariableHandlers] Setting value for mode ${modeId}:`, normalizedValue);
        variable.setValueForMode(modeId, normalizedValue);
        console.log(`[VariableHandlers] Successfully set value for mode ${modeId}`);
        valuesSet[modeId] = true;
      } catch (error) {
        const errorMsg = error instanceof Error ? error.message : String(error);
        console.error(`[VariableHandlers] Failed to set value for mode ${modeId}:`, errorMsg);
        errors.push(`Mode ${modeId}: ${errorMsg}`);
        valuesSet[modeId] = false;
      }
    }
  } else if (defaultValue !== undefined) {
    // Set default value for all modes
    const defaultModeId = collection.modes[0].modeId;
    let normalizedValue = defaultValue;

    if (variableType === 'COLOR') {
      normalizedValue = normalizeColorValue(defaultValue);
    }

    console.log('[VariableHandlers] Setting default value for mode:', defaultModeId);
    try {
      variable.setValueForMode(defaultModeId, normalizedValue);
      valuesSet[defaultModeId] = true;
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : String(error);
      errors.push(`Failed to set default value: ${errorMsg}`);
      valuesSet[defaultModeId] = false;
    }
  }

  console.log('[VariableHandlers] Created variable:', variable.id);
  console.log('[VariableHandlers] Values set status:', valuesSet);
  if (errors.length > 0) {
    console.warn('[VariableHandlers] Errors encountered:', errors);
  }

  const result = {
    ...serializeVariable(variable),
    valuesSet: Object.keys(valuesSet).length > 0 ? valuesSet : undefined,
    errors: errors.length > 0 ? errors : undefined
  };

  return result;
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
  console.log('[VariableHandlers] handleSetVariableValue called with params:', JSON.stringify(params));

  const variableId = params[ParamNames.VARIABLE_ID];
  let modeId = params[ParamNames.MODE_ID];
  const value = params[ParamNames.VALUE];

  if (!variableId) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VARIABLE_ID));
  }
  if (value === undefined) {
    throw new Error(ErrorMessages.missingParam(ParamNames.VALUE));
  }

  console.log('[VariableHandlers] Getting variable:', variableId);
  const variable = await getVariable(variableId);
  console.log('[VariableHandlers] Variable retrieved:', variable.id, 'type:', variable.resolvedType, 'collectionId:', variable.variableCollectionId);

  // If modeId not provided, use the first mode from the collection
  if (!modeId) {
    console.log('[VariableHandlers] No modeId provided, fetching collection to get first mode');
    const collection = await figma.variables.getVariableCollectionByIdAsync(variable.variableCollectionId);
    if (!collection || collection.modes.length === 0) {
      throw new Error('Variable collection has no modes');
    }
    modeId = collection.modes[0].modeId;
    console.log('[VariableHandlers] Using first mode:', modeId, 'from modes:', collection.modes.map(m => ({ id: m.modeId, name: m.name })));
  } else {
    console.log('[VariableHandlers] Using provided modeId:', modeId);
  }

  // Normalize value based on variable type
  let normalizedValue = value;
  if (variable.resolvedType === 'COLOR') {
    console.log('[VariableHandlers] Normalizing color value:', value);
    normalizedValue = normalizeColorValue(value);
    console.log('[VariableHandlers] Normalized color value:', normalizedValue);
  }

  // Set the value for the mode
  console.log('[VariableHandlers] Setting value for mode:', modeId, 'value:', normalizedValue);
  variable.setValueForMode(modeId, normalizedValue);

  console.log('[VariableHandlers] Successfully set variable value for:', variableId, 'mode:', modeId);
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
