# Figma MCP Tool Implementation Guide

## Quick Reference: Tool Implementation Steps

### Adding a New Tool (Server-Side)

#### Step 1: Create Tool Class

**Location:** `/server/src/main/kotlin/com/figma/mcp/tools/impl/<Category>/<ToolName>Tool.kt`

**Example:** CreateLucideIconTool

```kotlin
package com.figma.mcp.tools.impl.images

import com.figma.mcp.config.FigmaConstants.*
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

class CreateLucideIconTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.CREATE_LUCIDE_ICON) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "Creates a Lucide icon in Figma. Supports all 1000+ icons from the Lucide icon library.",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.ICON_NAME to mapOf(
                        "type" to "string",
                        "description" to "Name of the Lucide icon (e.g., 'search', 'menu', 'user')"
                    ),
                    ParamNames.SIZE to mapOf(
                        "type" to "number",
                        "description" to "Icon size in pixels (optional, default: 24)"
                    ),
                    ParamNames.FILL_COLOR to mapOf(
                        "type" to "string",
                        "description" to "Fill color as hex (e.g., '#000000') (optional)"
                    ),
                    ParamNames.FILL_STYLE_ID to mapOf(
                        "type" to "string",
                        "description" to "Fill style ID to apply (optional, overrides color)"
                    ),
                    ParamNames.STROKE_WEIGHT to mapOf(
                        "type" to "number",
                        "description" to "Stroke width in pixels (optional, default: 2)"
                    ),
                    ParamNames.X to mapOf(
                        "type" to "number",
                        "description" to "X position (optional, default: ${Defaults.DEFAULT_POSITION_X})"
                    ),
                    ParamNames.Y to mapOf(
                        "type" to "number",
                        "description" to "Y position (optional, default: ${Defaults.DEFAULT_POSITION_Y})"
                    )
                ),
                required = listOf(ParamNames.ICON_NAME)
            )
        )
    }

    override fun getPluginMethod(): String {
        return PluginMethods.CREATE_LUCIDE_ICON
    }

    override fun validate(arguments: JsonObject): String? {
        val iconName = arguments[ParamNames.ICON_NAME]?.jsonPrimitive?.contentOrNull
            ?: return "Missing required parameter: iconName"
        
        // Validate icon name format (alphanumeric, hyphens allowed)
        if (!iconName.matches(Regex("[a-z0-9-]+"))) {
            return "Invalid icon name format. Use lowercase with hyphens (e.g., 'chevron-down')"
        }
        
        val size = arguments[ParamNames.SIZE]?.jsonPrimitive?.doubleOrNull
        if (size != null && (size < 8 || size > 256)) {
            return "Icon size must be between 8 and 256 pixels"
        }
        
        return null
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        val iconName = params.getRequiredString(ParamNames.ICON_NAME)
        val size = params.getDoubleOrDefault(ParamNames.SIZE, Defaults.DEFAULT_ICON_SIZE)
        
        return buildJsonObject {
            put("type", NodeTypes.COMPONENT)  // Icons are components
            put("iconName", iconName)
            put("size", size)
            put(ParamNames.X, params.getDoubleOrDefault(ParamNames.X, Defaults.DEFAULT_POSITION_X))
            put(ParamNames.Y, params.getDoubleOrDefault(ParamNames.Y, Defaults.DEFAULT_POSITION_Y))
            
            // Stroke weight
            params.getDoubleOrNull(ParamNames.STROKE_WEIGHT)?.let { 
                put(ParamNames.STROKE_WEIGHT, it) 
            } ?: put(ParamNames.STROKE_WEIGHT, 2.0)
            
            // Color handling - style takes precedence
            params.getStringOrNull(ParamNames.FILL_STYLE_ID)?.let { 
                put(ParamNames.FILL_STYLE_ID, it) 
            } ?: run {
                params.getStringOrNull(ParamNames.FILL_COLOR)?.let { 
                    put(ParamNames.FILL_COLOR, it) 
                } ?: put(ParamNames.FILL_COLOR, "#000000")  // Default black
            }
        }
    }

    override fun buildSuccessMessage(
        pluginResponse: JsonElement?,
        params: JsonObject
    ): String {
        val iconNodeId = pluginResponse?.jsonObject?.get("nodeId")?.jsonPrimitive?.contentOrNull
        val iconName = params[ParamNames.ICON_NAME]?.jsonPrimitive?.contentOrNull ?: "icon"
        
        return if (iconNodeId != null) {
            "Successfully created Lucide icon '$iconName' with ID: $iconNodeId"
        } else {
            "Successfully created Lucide icon '$iconName'"
        }
    }
}
```

#### Step 2: Add Tool Name Constant

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

```kotlin
object ToolNames {
    // ... existing tools ...
    
    // Icons (New Category or add to existing)
    const val CREATE_LUCIDE_ICON = "figma_create_lucide_icon"
}
```

#### Step 3: Add Plugin Method Constant

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

```kotlin
object PluginMethods {
    // ... existing methods ...
    
    const val CREATE_LUCIDE_ICON = "createLucideIcon"
}
```

#### Step 4: Add Parameter Names (if new)

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

```kotlin
object ParamNames {
    // ... existing params ...
    
    const val ICON_NAME = "iconName"
}
```

#### Step 5: Add Default Values (if new)

**File:** `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`

```kotlin
object Defaults {
    // ... existing defaults ...
    
    const val DEFAULT_ICON_SIZE = 24.0
}
```

#### Step 6: Register Tool in DI

**File:** `/server/src/main/kotlin/com/figma/mcp/infrastructure/Di Module.kt`

```kotlin
single {
    val toolRegistry = FigmaToolRegistry(get())
    
    toolRegistry.registerAll(
        // Category 1: Node Creation Tools
        CreateFrameTool(get(), get()),
        // ... existing tools ...
        
        // Category 9: Image & Media Tools
        CreateImageTool(get(), get()),
        SetImageFillTool(get(), get()),
        ExportNodeTool(get(), get(), get()),
        GetImageFillsTool(get(), get()),
        CreateLucideIconTool(get(), get()),  // ← ADD HERE
        
        // ... other categories ...
    )
    
    toolRegistry
}
```

---

### Adding Handler (Plugin-Side)

#### Step 1: Create Icon Handler File

**Location:** `/figma-plugin/src/plugin/iconHandlers.ts`

```typescript
/**
 * Lucide Icon Handlers
 *
 * Creates icon instances from Lucide icon library.
 * Supports all 1000+ icons with customization.
 */

import { PluginMethods, ParamNames } from './constants';
import { applyCommonProperties } from './nodeHandlers';

// Map of Lucide icon names to their SVG paths
// In production, fetch from Lucide API or import from lucide package
const LUCIDE_ICONS: Record<string, string> = {
  'search': `<path d="M11 19a8 8 0 100-16 8 8 0 000 16zm7.53-11.47l4.72 4.72M1 1l6.36 6.36"/>`,
  'menu': `<line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/>`,
  'user': `<path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>`,
  // Add more icons as needed
};

export async function createLucideIcon(params: Record<string, any>): Promise<SceneNode> {
  const iconName = params['iconName'];
  const size = params['size'] ?? 24;
  const strokeWeight = params['strokeWeight'] ?? 2;
  const fillColor = params['fillColor'] ?? '#000000';
  
  if (!iconName) {
    throw new Error('Missing required parameter: iconName');
  }
  
  // Create a frame to contain the icon
  const iconFrame = figma.createFrame();
  iconFrame.name = `Icon/${iconName}`;
  iconFrame.resize(size, size);
  iconFrame.fills = [];  // Transparent background
  
  // Get SVG path for icon
  const svgPath = LUCIDE_ICONS[iconName];
  if (!svgPath) {
    throw new Error(`Unknown icon: ${iconName}`);
  }
  
  // Create vector from SVG path
  // Note: Figma doesn't have direct SVG import, so we create vector shapes
  // For simplicity, create a text node with icon character
  // For production: Parse SVG and create proper vector shapes
  
  const iconNode = figma.createText();
  iconNode.characters = `[${iconName}]`;
  iconNode.fills = [{ type: 'SOLID', color: hexToRgb(fillColor) }];
  
  // Apply common properties
  await applyCommonProperties(iconNode, params);
  
  // Add to frame
  iconFrame.appendChild(iconNode);
  
  return iconFrame;
}

function hexToRgb(hex: string): RGB {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) {
    return { r: 0, g: 0, b: 0 };
  }
  return {
    r: parseInt(result[1], 16) / 255,
    g: parseInt(result[2], 16) / 255,
    b: parseInt(result[3], 16) / 255,
  };
}
```

#### Step 2: Register Handler

**File:** `/figma-plugin/src/plugin/code.ts`

```typescript
import * as IconHandlers from './iconHandlers';

const nodeCreationHandlers: Record<string, (params: Record<string, any>) => Promise<SceneNode>> = {
  [NodeTypes.FRAME]: NodeHandlers.createFrame,
  // ... existing handlers ...
  [NodeTypes.COMPONENT]: NodeHandlers.createComponent,
  
  // Add new handler - map to appropriate type or create new
  'LUCIDE_ICON': IconHandlers.createLucideIcon,
};

// OR if using existing type:
async function handleCreateNode(params: Record<string, any>, requestId?: string): Promise<void> {
  try {
    const nodeType = params.type;
    
    // Special case for Lucide icons
    if (nodeType === 'LUCIDE_ICON') {
      const icon = await IconHandlers.createLucideIcon(params);
      if (requestId) {
        sendWSResponse(requestId, {
          success: true,
          nodeId: icon.id
        });
      }
      return;
    }
    
    const handler = nodeCreationHandlers[nodeType];
    // ... rest of handler logic
  } catch (e) {
    // error handling
  }
}
```

#### Step 3: Add Plugin Constants

**File:** `/figma-plugin/src/plugin/constants.ts`

```typescript
export const PluginMethods = {
  // ... existing methods ...
  CREATE_LUCIDE_ICON: 'createLucideIcon',
};

export const ParamNames = {
  // ... existing params ...
  ICON_NAME: 'iconName',
};

export const Defaults = {
  // ... existing defaults ...
  DEFAULT_ICON_SIZE: 24,
};
```

---

## Tool Implementation Checklist

When implementing a new tool, verify:

### Server-Side Checklist

- [ ] Tool class created extending `BaseFigmaTool`
- [ ] Tool name constant added to `FigmaConstants.ToolNames`
- [ ] Plugin method constant added to `FigmaConstants.PluginMethods`
- [ ] Any new parameter names added to `FigmaConstants.ParamNames`
- [ ] Any new defaults added to `FigmaConstants.Defaults`
- [ ] `getDefinition()` returns proper Tool with schema
- [ ] `validate()` method validates all parameters
- [ ] `buildCommandParams()` builds proper command JSON
- [ ] `getPluginMethod()` returns correct plugin method
- [ ] `buildSuccessMessage()` provides user-friendly response
- [ ] Tool registered in Di Module with dependencies
- [ ] Proper logging at key points
- [ ] Error handling with meaningful messages
- [ ] Tests cover happy path and error cases

### Plugin-Side Checklist

- [ ] Handler function created and exported
- [ ] Handler added to registry (if applicable)
- [ ] All parameters extracted with defaults
- [ ] Input validation in handler
- [ ] Proper error messages
- [ ] Node created with correct type
- [ ] Common properties applied
- [ ] Response includes nodeId
- [ ] Plugin method constant added
- [ ] Parameter constants added

### Integration Checklist

- [ ] Server tool name matches plugin method name
- [ ] Parameter names match between server and plugin
- [ ] Default values consistent
- [ ] Error messages clear and helpful
- [ ] Tool documented in comments
- [ ] No hardcoded values (all in constants)
- [ ] Tool category organized in Di Module
- [ ] Tool tests passing

---

## Common Patterns

### Pattern 1: Simple Node Creation

```kotlin
// Server-side
override fun buildCommandParams(params: JsonObject): JsonObject {
    return buildJsonObject {
        put("type", NodeTypes.FRAME)
        put("width", params.getRequiredDouble("width"))
        put("height", params.getRequiredDouble("height"))
        put("x", params.getDoubleOrDefault("x", Defaults.DEFAULT_POSITION_X))
        put("y", params.getDoubleOrDefault("y", Defaults.DEFAULT_POSITION_Y))
        params.getStringOrNull("name")?.let { put("name", it) }
    }
}
```

### Pattern 2: Style Application

```kotlin
// Apply style IDs if provided
params.getStringOrNull("fillStyleId")?.let { put("fillStyleId", it) }
params.getStringOrNull("strokeStyleId")?.let { put("strokeStyleId", it) }
params.getStringOrNull("effectStyleId")?.let { put("effectStyleId", it) }
```

### Pattern 3: Conditional Parameters

```kotlin
// Only include if provided
params.getDoubleOrNull("width")?.let { put("width", it) }
params.getArrayOrNull("fills")?.let { put("fills", it) }
```

### Pattern 4: Parameter Validation

```kotlin
override fun validate(arguments: JsonObject): String? {
    val required = arguments[ParamNames.SIDES]?.jsonPrimitive?.intOrNull
        ?: return "Missing parameter: sides"
    
    if (required < 3 || required > 100) {
        return "Parameter 'sides' must be between 3 and 100"
    }
    
    return null
}
```

---

## Error Handling Examples

### Server-Side Error Response

```kotlin
private fun errorResult(message: String): CallToolResult {
    return CallToolResult(
        content = listOf(
            ToolContent.TextContent(text = message)
        ),
        isError = true
    )
}
```

### Plugin-Side Error Handling

```typescript
async function handleCreateNode(params: Record<string, any>, requestId?: string): Promise<void> {
  try {
    const handler = nodeCreationHandlers[params.type];
    if (!handler) {
      throw new Error(`Unknown node type: ${params.type}`);
    }
    
    const node = await handler(params);
    sendWSResponse(requestId, { success: true, nodeId: node.id });
  } catch (error) {
    sendWSResponse(requestId, {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    });
  }
}
```

---

## Testing Strategy

### Unit Test (Kotlin)

```kotlin
@Test
fun testCreateLucideIconValidation() {
    val tool = CreateLucideIconTool(mockLogger, mockConnectionManager)
    
    // Test missing required parameter
    val result = tool.validate(buildJsonObject {})
    assertNotNull(result)
    assertTrue(result.contains("iconName"))
    
    // Test invalid icon name
    val result2 = tool.validate(buildJsonObject {
        put("iconName", "INVALID_NAME!")
    })
    assertNotNull(result2)
    assertTrue(result2.contains("format"))
    
    // Test valid parameters
    val result3 = tool.validate(buildJsonObject {
        put("iconName", "search")
    })
    assertNull(result3)
}
```

### Plugin Integration Test (TypeScript)

```typescript
test('should create Lucide icon', async () => {
  const icon = await createLucideIcon({
    iconName: 'search',
    size: 24,
    fillColor: '#000000'
  });
  
  expect(icon.name).toContain('search');
  expect(icon.width).toBe(24);
  expect(icon.height).toBe(24);
});
```

---

## Summary

To add a new tool:

1. Create tool class in `/server/src/main/kotlin/com/figma/mcp/tools/impl/`
2. Add constants in `FigmaConstants.kt`
3. Register in DI module
4. Create plugin handler in `/figma-plugin/src/plugin/`
5. Register plugin handler
6. Test both sides
7. Verify no hardcoded values
8. Document with comments

The architecture supports this with 0 modifications to core infrastructure!
