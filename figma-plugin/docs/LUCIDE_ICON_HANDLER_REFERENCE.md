# Lucide Icon Handler - Quick Reference

## Overview

The Lucide Icon Handler creates vector icons in Figma from Lucide SVG content. It uses Figma's native SVG parser for maximum compatibility and supports custom sizing, positioning, and coloring.

## Command

**Method**: `createLucideIcon`

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `iconName` | string | ✅ Yes | - | Name for the icon node in Figma |
| `svgContent` | string | ✅ Yes | - | Complete SVG markup string |
| `size` | number | ❌ No | 24 | Target size in pixels (maintains aspect ratio) |
| `color` | string | ❌ No | - | Hex color code for recoloring (e.g., "#FF0000") |
| `x` | number | ❌ No | 0 | X position on canvas |
| `y` | number | ❌ No | 0 | Y position on canvas |

## Response

### Success Response
```json
{
  "success": true,
  "iconNodeId": "123:456",
  "iconName": "heart",
  "width": 48,
  "height": 48,
  "message": "Successfully created Lucide icon 'heart' with ID: 123:456"
}
```

### Error Response
```json
{
  "success": false,
  "error": "Missing required parameter: iconName"
}
```

## Examples

### Basic Icon Creation
```javascript
{
  "method": "createLucideIcon",
  "params": {
    "iconName": "heart",
    "svgContent": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z\"/></svg>"
  }
}
```

### Icon with Custom Size and Color
```javascript
{
  "method": "createLucideIcon",
  "params": {
    "iconName": "star",
    "svgContent": "<svg viewBox=\"0 0 24 24\"...>...</svg>",
    "size": 64,
    "color": "#FFD700"
  }
}
```

### Icon with Positioning
```javascript
{
  "method": "createLucideIcon",
  "params": {
    "iconName": "check",
    "svgContent": "<svg viewBox=\"0 0 24 24\"...>...</svg>",
    "x": 100,
    "y": 200,
    "size": 32,
    "color": "#00FF00"
  }
}
```

## Lucide Icon SVG Format

Lucide icons typically follow this format:

```xml
<svg
  xmlns="http://www.w3.org/2000/svg"
  width="24"
  height="24"
  viewBox="0 0 24 24"
  fill="none"
  stroke="currentColor"
  stroke-width="2"
  stroke-linecap="round"
  stroke-linejoin="round"
>
  <path d="..."/>
  <!-- Additional paths or shapes -->
</svg>
```

**Key Requirements:**
- Must have `viewBox` attribute
- Can have one or more `<path>` elements
- Supports `stroke` and `fill` attributes
- `currentColor` will be replaced by custom color if provided

## Error Codes

| Error Message | Cause | Solution |
|--------------|-------|----------|
| `Missing required parameter: iconName` | No icon name provided | Provide `iconName` parameter |
| `Missing required parameter: svgContent` | No SVG content provided | Provide valid `svgContent` |
| `Invalid SVG content` | Malformed SVG | Check SVG syntax and structure |
| `SVG must have a viewBox attribute` | Missing viewBox | Ensure SVG has viewBox attribute |
| `Failed to create icon` | Internal creation error | Check console logs for details |

## Color Handling

### Without Custom Color
- Uses original SVG colors
- `currentColor` rendered as black by default
- Preserves gradient and pattern fills

### With Custom Color
- Replaces all solid fills with custom color
- Replaces all solid strokes with custom color
- Applies recursively to all nested elements
- Preserves opacity and blend modes

### Supported Color Formats
- 6-digit hex: `#FF5733`
- 3-digit hex: `#F53`
- Without hash: `FF5733` or `F53`

## Size Behavior

The `size` parameter controls the largest dimension:
- **Square icons**: Both width and height become `size`
- **Rectangular icons**: Largest dimension becomes `size`, other dimension scaled proportionally
- **Maintains aspect ratio**: Icon proportions are preserved

### Examples:
- Input: 24x24 SVG, `size: 48` → Output: 48x48
- Input: 24x16 SVG, `size: 48` → Output: 48x32
- Input: 16x24 SVG, `size: 48` → Output: 32x48

## Best Practices

### 1. Icon Naming
```javascript
// Good - Descriptive names
iconName: "user-profile-avatar"
iconName: "navigation-menu-icon"

// Bad - Generic names
iconName: "icon1"
iconName: "temp"
```

### 2. Size Selection
```javascript
// Standard sizes (recommended)
size: 16  // Small UI icons
size: 24  // Default size (matches Lucide default)
size: 32  // Medium icons
size: 48  // Large icons
size: 64  // Hero icons

// Custom sizes (use when needed)
size: 28  // Specific design requirement
```

### 3. Color Consistency
```javascript
// Use design system colors
color: "#FF5733"  // Brand primary
color: "#3498DB"  // Brand accent
color: "#2ECC71"  // Success green
color: "#E74C3C"  // Error red

// Avoid
color: "red"      // Not supported - use hex
color: "rgb(...)" // Not supported - use hex
```

### 4. Positioning
```javascript
// Absolute positioning
{ x: 100, y: 200 }

// Grid layout (calculate programmatically)
{ x: col * 80, y: row * 80 }

// Default (top-left)
{ x: 0, y: 0 }  // or omit x and y
```

## Performance Considerations

- **Single Icon**: ~10-50ms creation time
- **Batch Operations**: Consider creating multiple icons in sequence
- **Complex SVGs**: Native parser handles efficiently
- **Memory**: Each icon is a lightweight Figma node

## Troubleshooting

### Issue: Icon not appearing
**Check:**
1. Is SVG content valid?
2. Does SVG have a viewBox?
3. Are there any console errors?
4. Is the icon outside visible canvas?

### Issue: Wrong color
**Check:**
1. Is color in hex format?
2. Is color parameter spelled correctly?
3. Does SVG have `fill="none"` or `stroke="none"`?

### Issue: Wrong size
**Check:**
1. Is size a positive number?
2. Does SVG have valid viewBox dimensions?
3. Check console logs for resize calculations

### Issue: Missing paths
**Check:**
1. Does SVG contain `<path>` elements?
2. Are paths visible (not transparent)?
3. Check for `display: none` or `opacity: 0`

## Integration Examples

### JavaScript/TypeScript Client
```typescript
async function createIcon(
  iconName: string,
  svgContent: string,
  options?: { size?: number; color?: string; x?: number; y?: number }
) {
  const params = {
    iconName,
    svgContent,
    ...options
  };

  const response = await sendCommand('createLucideIcon', params);
  return response;
}

// Usage
const result = await createIcon('heart', lucideHeartSvg, {
  size: 48,
  color: '#FF0000',
  x: 100,
  y: 200
});

console.log(`Created icon: ${result.iconNodeId}`);
```

### Kotlin Server
```kotlin
fun createLucideIcon(
    iconName: String,
    svgContent: String,
    size: Int = 24,
    color: String? = null,
    x: Int = 0,
    y: Int = 0
): IconResult {
    val params = buildMap {
        put("iconName", iconName)
        put("svgContent", svgContent)
        put("size", size)
        color?.let { put("color", it) }
        put("x", x)
        put("y", y)
    }

    return sendCommand("createLucideIcon", params)
}

// Usage
val result = createLucideIcon(
    iconName = "heart",
    svgContent = loadLucideIcon("heart"),
    size = 48,
    color = "#FF0000",
    x = 100,
    y = 200
)
```

## Related Documentation

- [Lucide Icons Library](https://lucide.dev/icons/)
- [Figma Plugin API - createNodeFromSvg](https://www.figma.com/plugin-docs/api/figma/#createnodeFromsvg)
- [SVG Specification](https://www.w3.org/TR/SVG2/)
- [Icon Handlers Test Specification](../src/plugin/iconHandlers.test.md)

## Support

For issues or questions:
1. Check console logs for detailed error messages
2. Verify SVG content is valid
3. Review parameter types and requirements
4. Check Figma Plugin API documentation

---

**Last Updated**: 2025-11-01
**Version**: 1.0.0
**Status**: Production Ready
