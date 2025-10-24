# Style Priority System - MCP Server Prompt Update

## Overview

The MCP server prompt has been updated to prioritize **Styles** over **Variables** over **Hardcoded Values**, ensuring Claude Code creates more consistent, maintainable, and design-system-compliant Figma designs.

## Priority Hierarchy

### 🥇 Priority 1: STYLES (Highest)
**Always check and use styles first**

Styles are named, reusable design properties that can be applied to multiple elements:
- **Paint Styles**: Fill and stroke colors
- **Text Styles**: Typography (font, size, weight, line height)
- **Effect Styles**: Shadows, blurs, and other effects

**Why Styles First?**
- ✅ Centralized design decisions
- ✅ One-click updates across all instances
- ✅ Better design system governance
- ✅ Easier maintenance and consistency
- ✅ Industry standard best practice

### 🥈 Priority 2: VARIABLES (Second)
**Use variables for properties that don't have styles**

Variables (design tokens) are dynamic values that can be bound to properties:
- Colors, spacing, typography, corner radius
- Used for properties like padding, item spacing, border radius
- More flexible than hardcoded values, but less discoverable than styles

### 🥉 Priority 3: HARDCODED VALUES (Last Resort)
**Avoid unless absolutely necessary**
- Should be rare in well-organized design systems
- Only use when neither styles nor variables exist
- Document why styles/variables weren't used

## Workflow Changes

### Old Workflow (Variables-First)
```
1. Query variables (figma_get_variables)
2. Create nodes with temporary values
3. Bind variables after creation
4. Manual style application (if remembered)
```

### New Workflow (Styles-First) ✅
```
Step 0: Query Styles FIRST
  - figma_get_local_paint_styles()
  - figma_get_local_text_styles()
  - figma_get_local_effect_styles()

Step 1: Query Variables (Second)
  - figma_get_variables()

Step 2: Create nodes using style IDs directly
  - Pass fillStyleId parameter
  - Pass strokeStyleId parameter
  - Pass textStyleId parameter

Step 3: If NO styles exist, create them first
  - figma_create_paint_style()
  - figma_create_text_style()
  - figma_create_effect_style()

Step 4: Bind variables for non-style properties
  - Padding, spacing, corner radius
```

## Style-First Node Creation

### Example 1: Creating a Button

**OLD WAY (Variables):**
```kotlin
// Create button with temporary color
val button = figma_create_frame(
  fills: [{ color: { r: 0.23, g: 0.51, b: 0.96 } }]
)

// Bind variable after creation
figma_bind_variable(button.id, "fills", primaryColorVariableId)
```

**NEW WAY (Styles):** ✅
```kotlin
// Query styles first
val paintStyles = figma_get_local_paint_styles()
val buttonStyle = paintStyles.find { it.name.contains("Primary/Button") }

// If style exists, use it directly
if (buttonStyle != null) {
  val button = figma_create_frame(
    fillStyleId: buttonStyle.id  // ← Style applied during creation
  )
} else {
  // Create style first, then use it
  val newStyle = figma_create_paint_style(
    name: "Primary/Button",
    paints: [{ type: "SOLID", color: { r: 0.23, g: 0.51, b: 0.96 } }]
  )

  val button = figma_create_frame(
    fillStyleId: newStyle.id
  )
}
```

### Example 2: Creating Text

**NEW WAY (Styles):** ✅
```kotlin
// Query text styles first
val textStyles = figma_get_local_text_styles()
val headingStyle = textStyles.find { it.name.contains("Heading/H1") }

// Use text style directly
val heading = figma_create_text(
  content: "Welcome",
  textStyleId: headingStyle?.id  // ← Text style applied during creation
)

// If no style exists, create one first
if (headingStyle == null) {
  val newTextStyle = figma_create_text_style(
    name: "Text/Heading/H1",
    fontSize: 32,
    fontWeight: 700,
    fontFamily: "Inter"
  )

  // Now create text with style
  val heading = figma_create_text(
    content: "Welcome",
    textStyleId: newTextStyle.id
  )
}
```

## Tool Updates Required

To fully support this style-first approach, the following tools need to accept style ID parameters:

### Node Creation Tools
All shape creation tools should accept:
- `fillStyleId?: string` - Paint style for fills
- `strokeStyleId?: string` - Paint style for strokes
- `effectStyleId?: string` - Effect style for shadows/effects

**Tools to Update:**
- ✅ `figma_create_rectangle` - Add fillStyleId, strokeStyleId
- ✅ `figma_create_frame` - Add fillStyleId, strokeStyleId
- ✅ `figma_create_ellipse` - Add fillStyleId, strokeStyleId
- ✅ `figma_create_polygon` - Add fillStyleId, strokeStyleId
- ✅ `figma_create_star` - Add fillStyleId, strokeStyleId
- ✅ `figma_create_line` - Add strokeStyleId
- ✅ `figma_create_text` - Add textStyleId, fillStyleId

### Style Query Tools (Already Exist)
- ✅ `figma_get_local_paint_styles()` - Get all fill/stroke styles
- ✅ `figma_get_local_text_styles()` - Get all text styles
- ✅ `figma_get_local_effect_styles()` - Get all effect styles

### Style Creation Tools (Already Exist)
- ✅ `figma_create_paint_style()` - Create color styles
- ✅ `figma_create_text_style()` - Create typography styles
- ✅ `figma_create_effect_style()` - Create shadow/effect styles

### Style Application Tools (Already Exist)
- ✅ `figma_set_fill_style_id()` - Apply style after creation
- ✅ `figma_set_stroke_style_id()` - Apply stroke style
- ✅ `figma_set_text_style_id()` - Apply text style

## Benefits of Style-First Approach

### 1. Better Design System Governance
- Styles are visible in Figma's UI
- Designers can see all styles at a glance
- Easier to audit and maintain

### 2. Easier Updates
- Change style once, updates everywhere
- No need to find and update each instance
- Reduces maintenance burden

### 3. Consistency Enforcement
- Impossible to use colors not in the style library
- Forces thoughtful style creation
- Prevents color proliferation

### 4. Better Collaboration
- Designers and developers see same styles
- Clear naming conventions
- Easier handoff and communication

### 5. Industry Standard
- Follows Figma best practices
- Matches how professional design systems work
- Familiar to all Figma users

## Migration Guide

### For Existing Prompts
If you have custom prompts that don't use styles:

**Before:**
```
1. Query variables
2. Create nodes
3. Bind variables
```

**After:**
```
1. Query styles (new step)
2. Query variables
3. Create nodes with style IDs
4. Bind variables for non-style properties
```

### For Existing Designs
- Existing designs with variables still work
- New designs will use styles first
- Gradually migrate to style-based approach

## Verification Checklist

When Claude Code creates designs, verify:

**Styles (Highest Priority):**
- ✅ Executed `figma_get_local_paint_styles()` first
- ✅ Executed `figma_get_local_text_styles()` first
- ✅ Executed `figma_get_local_effect_styles()` first
- ✅ Used `fillStyleId` when creating nodes
- ✅ Used `textStyleId` when creating text
- ✅ Created styles if they didn't exist
- ✅ NO hardcoded colors (#3B82F6) when styles available

**Variables (Second Priority):**
- ✅ Executed `figma_get_variables()` after checking styles
- ✅ Used variables for padding, spacing, radius
- ✅ Bound variables with `figma_bind_variable()`

**Hardcoded Values (Avoid):**
- ⚠️ Only used when neither styles nor variables exist
- 📝 Documented reason for using hardcoded values

## Prompt Updates Summary

### New Sections Added
1. **Design System Priority Hierarchy** - Explains styles > variables > hardcoded
2. **Style-First Node Creation Pattern** - Complete examples
3. **Priority 1: STYLES** - Detailed style usage guidelines
4. **Priority 2: VARIABLES** - When to use variables
5. **Priority 3: HARDCODED VALUES** - Avoid unless necessary

### Updated Sections
1. **Workflow** - Added "Step 0: Query Styles FIRST"
2. **Verification Checklist** - Added style checks at top
3. **Design System & Variables Rules** - Reordered to prioritize styles

### Code Examples Added
- Button creation with styles (complete workflow)
- Input field creation with styles (complete workflow)
- Style checking and fallback logic
- Style creation before node creation

## Testing the Updated Prompt

### Test Case 1: Create a Button
```
User: "Create a primary button"

Expected Behavior:
1. Query paint styles first
2. Look for "Primary" or "Button" style
3. If found: Use fillStyleId in creation
4. If not found: Create "Primary/Button" style first
5. Never use hardcoded #3B82F6
```

### Test Case 2: Create Text
```
User: "Create a heading"

Expected Behavior:
1. Query text styles first
2. Look for "Heading" or "H1" style
3. If found: Use textStyleId in creation
4. If not found: Create "Text/Heading/H1" style first
5. Apply style, not hardcoded fontSize/fontWeight
```

### Test Case 3: Multiple Buttons
```
User: "Create 3 buttons: primary, secondary, danger"

Expected Behavior:
1. Query styles once
2. Create 3 styles if they don't exist
3. Use style IDs for all 3 buttons
4. All buttons use styles, not hardcoded values
5. Easy to update all buttons by changing styles
```

## Implementation Status

### ✅ Completed
- [x] Updated MCP server prompt with style priority
- [x] Added style-first workflow documentation
- [x] Added code examples
- [x] Updated verification checklist
- [x] Server builds successfully

### 🚧 Future Enhancements
- [ ] Update node creation tools to accept styleId parameters
- [ ] Add style suggestion logic (match by color similarity)
- [ ] Add style validation (warn if not using existing styles)
- [ ] Add style coverage metrics

## File Modified

**`/Volumes/ExtStorage/Projects/FigmaMcp/server/src/main/kotlin/com/figma/mcp/transport/McpServer.kt`**
- Updated `registerPrompts()` method
- Modified "figma-designer" prompt
- Added ~200 lines of style-first guidance
- Build status: ✅ Successful

## Next Steps

1. **Test the Prompt**: Use Claude Code to create designs, verify style-first behavior
2. **Update Tools** (if needed): Add styleId parameters to creation tools
3. **Document Examples**: Create more real-world examples
4. **Monitor Usage**: Check if Claude consistently uses styles
5. **Iterate**: Refine prompt based on actual usage patterns

## Summary

The MCP server now instructs Claude Code to:
1. **Check styles first** - Query all style libraries before creating
2. **Use style IDs** - Pass styleId parameters during node creation
3. **Create styles** - If styles don't exist, create them first
4. **Use variables second** - For properties without styles (padding, spacing)
5. **Avoid hardcoded** - Only use hardcoded values as last resort

This results in:
- ✅ More consistent designs
- ✅ Easier maintenance
- ✅ Better design system governance
- ✅ Industry-standard best practices
- ✅ Cleaner, more professional output
