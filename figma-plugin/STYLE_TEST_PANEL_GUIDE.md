# Style Test Panel Guide

## Overview

The Style Test Panel is a comprehensive testing interface for creating and applying Figma styles (Paint Styles, Text Styles, and Effect Styles) via the MCP server.

## Location

**App Tab**: "Style Management" (between "Styling" and "Typography")

## Features

### 1. Create Fill Styles

Test creating paint styles for fills:

**Fields:**
- **Style Name**: Name for the style (e.g., "Primary/Red/500")
- **Fill Color**: Color picker + hex input

**Example:**
1. Enter name: `Colors/Primary/Blue`
2. Select color: `#3B82F6`
3. Click "Create Fill Style"
4. Check Figma's local styles panel for the new style

### 2. Create Stroke Styles

Test creating paint styles for strokes:

**Fields:**
- **Style Name**: Name for the stroke style
- **Stroke Color**: Color picker + hex input
- **Stroke Width**: Width in pixels

**Example:**
1. Enter name: `Strokes/Border/Medium`
2. Select color: `#64748B`
3. Set width: `2`
4. Click "Create Stroke Style"

### 3. Create Text Styles

Test creating text styles:

**Fields:**
- **Style Name**: Name for the text style
- **Font Size**: Size in pixels
- **Font Weight**: Dropdown (100-900)
- **Font Family**: Font name (e.g., "Inter", "Roboto")

**Example:**
1. Enter name: `Typography/Heading/H1`
2. Font size: `32`
3. Font weight: `700` (Bold)
4. Font family: `Inter`
5. Click "Create Text Style"

### 4. Apply Styles to Selection

Test applying existing styles to selected nodes:

**Steps:**
1. **Create or Select a Node** in Figma
2. **Get Style ID**:
   - Click "Get All Styles" to see available styles in console
   - Copy a style ID (format: `S:abc123...`)
3. **Select Style Type**: Fill, Stroke, or Text
4. **Paste Style ID** into the input field
5. **Click "Apply to Selection"**

**Alternative - Create Test Node:**
- Click "Create Test Rectangle" to create a test shape
- Select it in Figma
- Apply styles to it

### 5. Create Effect Styles

Test creating shadow and blur effects:

**Effect Types:**
- **Drop Shadow**: Shadow outside the layer
- **Inner Shadow**: Shadow inside the layer
- **Layer Blur**: Blur effect on the entire layer

**Drop Shadow Fields:**
- **Effect Style Name**: Name for the effect
- **Effect Type**: Dropdown selector
- **Shadow Color**: Color picker + opacity slider
- **X Offset**: Horizontal offset
- **Y Offset**: Vertical offset
- **Blur**: Blur radius

**Example - Elevation Shadow:**
1. Enter name: `Shadows/Elevation/1`
2. Type: `Drop Shadow`
3. Color: `#000000`, Opacity: `0.1`
4. X: `0`, Y: `4`, Blur: `8`
5. Click "Create Effect Style"

**Example - Layer Blur:**
1. Enter name: `Effects/Blur/Light`
2. Type: `Layer Blur`
3. Blur: `4`
4. Click "Create Effect Style"

## Testing Workflow

### Complete Style System Test

**1. Create a Design System:**
```
Fill Styles:
- Colors/Primary/500 (#3B82F6)
- Colors/Secondary/500 (#8B5CF6)
- Colors/Success/500 (#10B981)

Text Styles:
- Typography/Heading/H1 (32px, Bold)
- Typography/Body/Regular (16px, Regular)

Effect Styles:
- Shadows/Elevation/1 (0, 4, 8, #000 10%)
- Shadows/Elevation/2 (0, 8, 16, #000 15%)
```

**2. Test Application:**
```
1. Click "Create Test Rectangle"
2. Get all styles (click "Get All Styles")
3. Copy a fill style ID from console
4. Apply it to the rectangle
5. Verify the style is applied in Figma
```

**3. Verify in Figma:**
- Open the "Design" panel in Figma
- Check "Local styles" section
- Your created styles should appear
- Test detaching and reapplying

## MCP Commands Used

The panel sends these MCP commands:

### Create Paint Style (Fill/Stroke)
```json
{
  "method": "figma_create_paint_style",
  "params": {
    "name": "Colors/Primary/500",
    "paints": [{
      "type": "SOLID",
      "color": { "r": 0.23, "g": 0.51, "b": 0.96 },
      "opacity": 1
    }]
  }
}
```

### Create Text Style
```json
{
  "method": "figma_create_text_style",
  "params": {
    "name": "Typography/Heading/H1",
    "fontSize": 32,
    "fontFamily": "Inter",
    "fontWeight": 700,
    "lineHeight": { "value": 1.5, "unit": "AUTO" },
    "letterSpacing": { "value": 0, "unit": "PIXELS" }
  }
}
```

### Create Effect Style
```json
{
  "method": "figma_create_effect_style",
  "params": {
    "name": "Shadows/Elevation/1",
    "effects": [{
      "type": "DROP_SHADOW",
      "color": { "r": 0, "g": 0, "b": 0, "a": 0.1 },
      "offset": { "x": 0, "y": 4 },
      "radius": 8,
      "visible": true,
      "blendMode": "NORMAL"
    }]
  }
}
```

### Apply Fill Style
```json
{
  "method": "figma_set_fill_style_id",
  "params": {
    "styleId": "S:abc123..."
  }
}
```

### Apply Stroke Style
```json
{
  "method": "figma_set_stroke_style_id",
  "params": {
    "styleId": "S:def456..."
  }
}
```

### Apply Text Style
```json
{
  "method": "figma_set_text_style_id",
  "params": {
    "styleId": "S:ghi789..."
  }
}
```

### Get All Paint Styles
```json
{
  "method": "figma_get_local_paint_styles",
  "params": {}
}
```

## Tips

### Getting Style IDs
1. **From Console**: Click "Get All Styles" and check browser console
2. **From Figma**: Right-click a style in Figma → Copy link → Extract ID from URL
3. **From Response**: When you create a style, the response contains the style ID

### Testing Best Practices
1. **Name Styles Clearly**: Use hierarchical names like `Colors/Primary/500`
2. **Test on Simple Shapes**: Use "Create Test Rectangle" for quick testing
3. **Check Console**: All responses are logged to the browser console
4. **Verify in Figma**: Always check the Figma design panel to confirm

### Common Issues

**"Unknown" file ID:**
- Save your Figma file to the cloud
- Reload the plugin
- File ID will update to the real Figma file key

**Style not applying:**
- Make sure you've selected a node in Figma first
- Verify the style ID is correct (format: `S:...`)
- Check that the node type supports that style type

**Font not found:**
- Ensure the font is installed on your system
- Use exact font family names
- Inter, Roboto, and system fonts work best

## Color Format Reference

**Hex to RGB Conversion:**
The panel automatically converts hex colors to Figma's RGB format (0-1 range):
- `#FF0000` → `{r: 1, g: 0, b: 0}`
- `#00FF00` → `{r: 0, g: 1, b: 0}`
- `#3B82F6` → `{r: 0.23, g: 0.51, b: 0.96}`

## Advanced Usage

### Creating a Complete Design Token System

**1. Color Palette:**
```
Colors/Gray/50   → #F9FAFB
Colors/Gray/100  → #F3F4F6
Colors/Gray/500  → #6B7280
Colors/Gray/900  → #111827

Colors/Blue/500  → #3B82F6
Colors/Blue/600  → #2563EB

Colors/Green/500 → #10B981
Colors/Red/500   → #EF4444
```

**2. Typography Scale:**
```
Typography/Display   → 48px, Bold (700)
Typography/Heading/1 → 32px, Bold (700)
Typography/Heading/2 → 24px, Semibold (600)
Typography/Body/L    → 18px, Regular (400)
Typography/Body/M    → 16px, Regular (400)
Typography/Body/S    → 14px, Regular (400)
Typography/Caption   → 12px, Regular (400)
```

**3. Shadow System:**
```
Shadows/SM  → Y:1, Blur:2,  Opacity:0.05
Shadows/MD  → Y:4, Blur:8,  Opacity:0.1
Shadows/LG  → Y:8, Blur:16, Opacity:0.15
Shadows/XL  → Y:16, Blur:32, Opacity:0.2
```

## Result Messages

The panel shows these result messages:

- **"Creating [type] style..."** - Request is being sent
- **"[Type] style creation sent!"** - Request sent successfully
- **"[Type] style application sent!"** - Apply request sent
- **"Style fetch request sent!"** - Get styles request sent

Check the browser console and Figma for actual results.

## Troubleshooting

### Styles not appearing in Figma
1. Check that server is connected
2. Verify file ID is correct (not "unknown")
3. Check browser console for errors
4. Ensure MCP server is running

### Cannot apply style
1. Select a node in Figma first
2. Verify style ID format (should start with `S:`)
3. Ensure node type matches style type (e.g., text styles only work on text nodes)

### Font errors
1. Ensure font is installed
2. Try system fonts: "Inter", "Arial", "Helvetica"
3. Check exact font family name in Figma

## Related Documentation

- Main README: `/README.md`
- File Routing: `/FILE_ROUTING_IMPLEMENTATION.md`
- MCP Server Docs: `/server/docs/`
