# Quick Guide: Adding New Figma MCP Tools

**Estimated Time**: 30 minutes per tool
**Difficulty**: Easy (thanks to SOLID refactoring!)

---

## Prerequisites

- Kotlin knowledge
- TypeScript knowledge
- Understanding of Figma Plugin API
- Familiarity with the tool specification in `/docs/TOOL_DESIGN_PLAN.md`

---

## Step-by-Step Process

### Step 1: Add Constants (5 minutes)

If your tool uses new constants, add them to `/server/src/main/kotlin/com/figma/mcp/config/FigmaConstants.kt`:

```kotlin
object ToolNames {
    // ... existing tools ...
    const val YOUR_NEW_TOOL = "figma_your_new_tool"
}

object ParamNames {
    // ... existing params ...
    const val YOUR_NEW_PARAM = "yourNewParam"
}
```

---

### Step 2: Create Tool Class (15 minutes)

Create a new file in `/server/src/main/kotlin/com/figma/mcp/tools/impl/`:

```kotlin
package com.figma.mcp.tools.impl

import com.figma.mcp.config.FigmaConstants.*
import com.figma.mcp.core.ILogger
import com.figma.mcp.protocol.JSONSchema
import com.figma.mcp.protocol.Tool
import com.figma.mcp.services.FigmaConnectionManager
import com.figma.mcp.tools.BaseFigmaTool
import kotlinx.serialization.json.*

/**
 * Your New Tool
 *
 * ## Purpose
 * Brief description of what this tool does
 *
 * ## Parameters
 * - param1: type (required/optional) - description
 * - param2: type (required/optional) - description
 */
class YourNewTool(
    logger: ILogger,
    connectionManager: FigmaConnectionManager
) : BaseFigmaTool(logger, connectionManager, ToolNames.YOUR_NEW_TOOL) {

    override fun getDefinition(): Tool {
        return Tool(
            name = toolName,
            description = "User-friendly description for Claude Code",
            inputSchema = JSONSchema.createObjectSchema(
                properties = mapOf(
                    ParamNames.PARAM1 to mapOf(
                        "type" to "string",
                        "description" to "What this parameter does"
                    ),
                    ParamNames.PARAM2 to mapOf(
                        "type" to "number",
                        "description" to "What this parameter does"
                    )
                ),
                required = listOf(ParamNames.PARAM1)  // Only required params
            )
        )
    }

    override fun buildCommandParams(params: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", NodeTypes.YOUR_NODE_TYPE)  // Or skip if not a node creation tool
            put(ParamNames.PARAM1, params.getRequiredString(ParamNames.PARAM1))
            put(ParamNames.PARAM2, params.getDoubleOrDefault(ParamNames.PARAM2, Defaults.SOME_DEFAULT))

            // Optional parameters
            params.getStringOrNull(ParamNames.OPTIONAL_PARAM)?.let { put(ParamNames.OPTIONAL_PARAM, it) }
        }
    }

    // Optional: Override if using different plugin method
    override fun getPluginMethod(): String {
        return PluginMethods.YOUR_CUSTOM_METHOD  // Default is CREATE_NODE
    }

    // Optional: Override for custom validation
    override fun validate(arguments: JsonObject): String? {
        // Custom validation logic
        return null  // Return error message if invalid, null if valid
    }

    // Optional: Override for custom success messages
    override fun buildSuccessMessage(pluginResponse: JsonElement?, params: JsonObject): String {
        return "Custom success message"
    }
}
```

---

### Step 3: Register Tool (1 minute)

Add ONE line to `/server/src/main/kotlin/com/figma/mcp/infrastructure/Di Module.kt`:

```kotlin
single {
    val toolRegistry = FigmaToolRegistry(get())

    toolRegistry.registerAll(
        // ... existing 12 tools ...
        YourNewTool(get(), get())  // ✅ That's it!
    )

    toolRegistry
}
```

---

### Step 4: Implement Plugin Handler (10 minutes)

#### Option A: Node Creation Tool (uses existing handler)

If your tool creates a new node type, add to `/figma-plugin/src/plugin/nodeHandlers.ts`:

```typescript
import { NodeTypes, ParamNames, ErrorMessages } from './constants';

/**
 * Create Your New Node
 */
export async function createYourNode(params: Record<string, any>): Promise<YourNodeType> {
  // Get required parameters
  const param1 = params[ParamNames.PARAM1];
  if (!param1) {
    throw new Error(ErrorMessages.missingParam(ParamNames.PARAM1));
  }

  // Create the node
  const node = figma.createYourNode();

  // Configure the node
  if (params[ParamNames.PARAM2]) {
    node.someProperty = params[ParamNames.PARAM2];
  }

  // Apply common properties
  applyCommonProperties(node, params);

  // Add to page and select
  addToPage(node);

  return node;
}
```

Then register the handler in `/figma-plugin/src/plugin/code-refactored.ts`:

```typescript
const nodeCreationHandlers: Record<string, (params: Record<string, any>) => Promise<SceneNode>> = {
  // ... existing handlers ...
  [NodeTypes.YOUR_NODE_TYPE]: NodeHandlers.createYourNode,
};
```

#### Option B: Non-Node Creation Tool (custom handler)

Add a new handler function to `/figma-plugin/src/plugin/code-refactored.ts`:

```typescript
async function handleYourCustomCommand(params: Record<string, any>, requestId: string): Promise<void> {
  try {
    // Your custom logic here
    const result = doSomething(params);

    sendWSResponse(requestId, {
      success: true,
      result: result,
      message: SuccessMessages.customSuccess(result),
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    sendWSResponse(requestId, {
      success: false,
      error: errorMessage,
    });
  }
}
```

Register in the switch statement:

```typescript
async function handleWSCommand(command: any): Promise<void> {
  const { id, method, params } = command;

  switch (method) {
    // ... existing cases ...
    case PluginMethods.YOUR_CUSTOM_METHOD:
      await handleYourCustomCommand(params, id);
      break;
  }
}
```

---

### Step 5: Build and Test (5 minutes)

```bash
# Compile Kotlin
cd /Volumes/ExtStorage/Projects/FigmaMcp/server
./gradlew build -x test

# If compilation succeeds:
BUILD SUCCESSFUL in Xs
```

---

## Common Patterns

### Pattern 1: Required Parameter

```kotlin
override fun buildCommandParams(params: JsonObject): JsonObject {
    return buildJsonObject {
        put("nodeId", params.getRequiredString("nodeId"))  // Throws if missing
    }
}
```

### Pattern 2: Optional Parameter with Default

```kotlin
override fun buildCommandParams(params: JsonObject): JsonObject {
    return buildJsonObject {
        put("width", params.getDoubleOrDefault("width", Defaults.DEFAULT_WIDTH))
    }
}
```

### Pattern 3: Optional Parameter (no default)

```kotlin
override fun buildCommandParams(params: JsonObject): JsonObject {
    return buildJsonObject {
        params.getStringOrNull("name")?.let { put("name", it) }
    }
}
```

### Pattern 4: Array Parameter

```kotlin
override fun buildCommandParams(params: JsonObject): JsonObject {
    val nodeIds = params.getRequiredStringArray("nodeIds")
    return buildJsonObject {
        put("nodeIds", params.getRequiredArray("nodeIds"))
    }
}
```

### Pattern 5: Custom Validation

```kotlin
override fun validate(arguments: JsonObject): String? {
    val value = arguments["count"]?.jsonPrimitive?.intOrNull
        ?: return "Missing or invalid parameter: count"

    if (value < 1 || value > 100) {
        return "Parameter 'count' must be between 1 and 100"
    }

    return null  // Valid
}
```

---

## Helper Methods Available in BaseFigmaTool

### Parameter Extraction

```kotlin
// Strings
params.getRequiredString("key")          // Throws if missing
params.getStringOrDefault("key", "default")
params.getStringOrNull("key")            // Returns null if missing

// Numbers
params.getRequiredDouble("key")
params.getDoubleOrDefault("key", 0.0)
params.getDoubleOrNull("key")

// Integers
params.getRequiredInt("key")
params.getIntOrDefault("key", 0)

// Arrays
params.getRequiredStringArray("key")
params.getRequiredArray("key")

// Objects
params.getObjectOrNull("key")
```

### Validation

```kotlin
validateRange(value, min, max, "paramName")  // Throws if out of range
```

### Error Handling

```kotlin
errorResult("Error message")  // Returns error CallToolResult
```

---

## Testing Your Tool

### Unit Test Template

```kotlin
class YourNewToolTest {
    private val mockLogger = mockk<ILogger>()
    private val mockConnectionManager = mockk<FigmaConnectionManager>()
    private val tool = YourNewTool(mockLogger, mockConnectionManager)

    @Test
    fun `should execute successfully with valid parameters`() {
        // Arrange
        val args = buildJsonObject {
            put("param1", "value1")
            put("param2", 42)
        }
        coEvery { mockConnectionManager.hasConnections() } returns true
        coEvery { mockConnectionManager.sendCommand(any(), any(), any()) } returns buildJsonObject {
            put("success", true)
            put("nodeId", "123:456")
        }

        // Act
        val result = runBlocking { tool.execute(args) }

        // Assert
        assertFalse(result.isError)
        assertTrue(result.content.first().toString().contains("Successfully"))
    }

    @Test
    fun `should reject missing required parameters`() {
        // Arrange
        val args = JsonObject(emptyMap())

        // Act
        val result = runBlocking { tool.execute(args) }

        // Assert
        assertTrue(result.isError)
        assertTrue(result.content.first().toString().contains("Missing required parameter"))
    }
}
```

---

## Troubleshooting

### Compilation Error: "Unresolved reference"

**Problem**: Missing import
**Solution**: Add to imports:
```kotlin
import kotlinx.serialization.json.*
```

### Runtime Error: "Tool not registered"

**Problem**: Forgot to register in DI module
**Solution**: Add to `toolRegistry.registerAll(...)` in `Di Module.kt`

### Plugin Error: "Unknown method"

**Problem**: Plugin doesn't have handler for new method
**Solution**: Add handler to `code-refactored.ts` switch statement

---

## Checklist

Before committing your new tool:

- [ ] Constants added to FigmaConstants.kt (if needed)
- [ ] Tool class created in `/tools/impl/`
- [ ] Tool registered in Di Module.kt
- [ ] Plugin handler implemented
- [ ] Plugin handler registered in switch statement
- [ ] Kotlin code compiles (`./gradlew build`)
- [ ] Unit tests added
- [ ] Tool tested end-to-end in Figma
- [ ] Documentation updated (if public API)

---

## Examples

See existing tools for reference:

**Simple tool**: `CreateRectangleTool.kt` - Basic shape creation
**Complex tool**: `CreateBooleanOperationTool.kt` - Multiple nodes, validation
**Custom method**: `CreateGroupTool.kt` - Uses groupNodes method instead of createNode
**Advanced validation**: `CreatePolygonTool.kt` - Range validation

---

## Need Help?

1. Check `/docs/TOOL_DESIGN_PLAN.md` for tool specifications
2. Review `/docs/REFACTORING_SUMMARY.md` for architecture overview
3. Look at existing tool implementations for patterns
4. Check Figma Plugin API docs: https://www.figma.com/plugin-docs/

---

**Last Updated**: 2025-10-19
**Version**: 1.0
