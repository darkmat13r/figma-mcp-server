# Design Pattern Examples - Before & After

## Overview

This document provides clear before/after examples of the UI improvements applied to the Figma MCP plugin test panels.

---

## 1. Label Styling

### ❌ Before
```tsx
<Label>Fill Color</Label>
<Label className="text-xs">Font Size</Label>
<label className="text-xs font-medium text-muted-foreground">
  Request ID
</label>
```

### ✅ After
```tsx
<Label className="text-xs font-medium">Fill Color</Label>
<Label className="text-xs font-medium">Font Size</Label>
<Label className="text-xs font-medium">Request ID</Label>
```

**Benefit**: Consistent, readable labels throughout all panels.

---

## 2. Label-Input Spacing

### ❌ Before
```tsx
<div className="space-y-2">
  <Label>Opacity</Label>
  <Input
    type="number"
    min="0"
    max="1"
    step="0.1"
    value={opacity}
    onChange={(e) => setOpacity(e.target.value)}
  />
</div>
```

### ✅ After
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Opacity</Label>
  <Input
    type="number"
    min="0"
    max="1"
    step="0.1"
    value={opacity}
    onChange={(e) => setOpacity(e.target.value)}
    className="text-xs h-9"
  />
</div>
```

**Benefit**: Tighter label-input relationship, consistent input sizing.

---

## 3. Color Picker Layout

### ❌ Before
```tsx
<div className="space-y-2">
  <Label>Fill Color</Label>
  <div className="flex gap-2">
    <Input
      type="color"
      value={fillColor}
      onChange={(e) => setFillColor(e.target.value)}
      className="w-20"
    />
    <Input
      value={fillColor}
      onChange={(e) => setFillColor(e.target.value)}
      placeholder="#FF0000"
    />
  </div>
</div>
```

### ✅ After
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Fill Color</Label>
  <div className="flex gap-2">
    <Input
      type="color"
      value={fillColor}
      onChange={(e) => setFillColor(e.target.value)}
      className="w-20 h-9 p-1"
    />
    <Input
      value={fillColor}
      onChange={(e) => setFillColor(e.target.value)}
      placeholder="#FF0000"
      className="flex-1 text-xs h-9"
    />
  </div>
</div>
```

**Benefit**: Color swatch and hex input are properly sized and aligned.

---

## 4. Grid Layouts for Paired Inputs

### ❌ Before
```tsx
<div className="space-y-2">
  <Label>X Offset</Label>
  <Input
    type="number"
    value={shadowX}
    onChange={(e) => setShadowX(e.target.value)}
  />
</div>
<div className="space-y-2">
  <Label>Y Offset</Label>
  <Input
    type="number"
    value={shadowY}
    onChange={(e) => setShadowY(e.target.value)}
  />
</div>
```

### ✅ After
```tsx
<div className="grid grid-cols-2 gap-2">
  <div className="space-y-1">
    <Label className="text-xs font-medium">X Offset</Label>
    <Input
      type="number"
      value={shadowX}
      onChange={(e) => setShadowX(e.target.value)}
      className="text-xs h-9"
    />
  </div>
  <div className="space-y-1">
    <Label className="text-xs font-medium">Y Offset</Label>
    <Input
      type="number"
      value={shadowY}
      onChange={(e) => setShadowY(e.target.value)}
      className="text-xs h-9"
    />
  </div>
</div>
```

**Benefit**: Related inputs are visually grouped and space-efficient.

---

## 5. Select Dropdown

### ❌ Before
```tsx
<div className="space-y-2">
  <Label>Stroke Align</Label>
  <Select value={strokeAlign} onValueChange={setStrokeAlign}>
    <SelectTrigger>
      <SelectValue />
    </SelectTrigger>
    <SelectContent>
      <SelectItem value="INSIDE">Inside</SelectItem>
      <SelectItem value="CENTER">Center</SelectItem>
      <SelectItem value="OUTSIDE">Outside</SelectItem>
    </SelectContent>
  </Select>
</div>
```

### ✅ After
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Stroke Align</Label>
  <Select value={strokeAlign} onValueChange={setStrokeAlign}>
    <SelectTrigger className="text-xs h-9">
      <SelectValue />
    </SelectTrigger>
    <SelectContent>
      <SelectItem value="INSIDE">Inside</SelectItem>
      <SelectItem value="CENTER">Center</SelectItem>
      <SelectItem value="OUTSIDE">Outside</SelectItem>
    </SelectContent>
  </Select>
</div>
```

**Benefit**: Consistent height with inputs, better text sizing.

---

## 6. Button Sizing

### ❌ Before
```tsx
<Button
  onClick={testSetFills}
  disabled={!nodeId}
  className="w-full h-8 text-xs"
>
  Apply Fills
</Button>

<div className="flex gap-2">
  <Button className="flex-1 h-8 text-xs">Apply</Button>
  <Button variant="outline" className="flex-1 h-8 text-xs">
    Cancel
  </Button>
</div>
```

### ✅ After
```tsx
<Button
  onClick={testSetFills}
  disabled={!nodeId}
  className="w-full"
>
  Apply Fills
</Button>

<div className="flex gap-2">
  <Button className="flex-1 text-xs">Apply</Button>
  <Button variant="outline" className="flex-1 text-xs">
    Cancel
  </Button>
</div>
```

**Benefit**: Default button height (more comfortable), consistent sizing.

---

## 7. Node Selection Helper

### ❌ Before
```tsx
<Section title="Node Selection">
  <div className="space-y-2">
    <Label>Node ID <span className="text-destructive">*</span></Label>
    <div className="flex gap-2">
      <Input
        value={nodeId}
        onChange={(e) => setNodeId(e.target.value)}
        placeholder="Enter node ID or use selection"
      />
      <Button onClick={getSelectedNode} size="sm">
        Get Selected
      </Button>
    </div>
  </div>
</Section>
```

### ✅ After
```tsx
<Section title="Node Selection">
  <div className="space-y-1">
    <Label className="text-xs font-medium">
      Node ID <span className="text-destructive">*</span>
    </Label>
    <div className="flex gap-2">
      <Input
        value={nodeId}
        onChange={(e) => setNodeId(e.target.value)}
        placeholder="Enter node ID or use selection"
        className="text-xs h-9"
      />
      <Button onClick={getSelectedNode} size="sm" className="h-9">
        Get Selected
      </Button>
    </div>
    <p className="text-xs text-muted-foreground">
      Select a node in Figma to auto-fill the Node ID
    </p>
  </div>
</Section>
```

**Benefit**: Added helpful context, consistent sizing, better alignment.

---

## 8. Alert/Feedback Messages

### ❌ Before
```tsx
{!isConnected && (
  <p className="text-xs text-yellow-500/80">
    Connect to WebSocket server to send messages
  </p>
)}
```

### ✅ After
```tsx
{!isConnected && (
  <Alert variant="destructive">
    <AlertDescription className="text-xs">
      Connect to WebSocket server to send messages
    </AlertDescription>
  </Alert>
)}
```

**Benefit**: More prominent, uses theme colors, better accessibility.

---

## 9. RGBA Color Input Grid (Variables Panel)

### ❌ Before
```tsx
<div className="grid grid-cols-4 gap-2">
  <div className="space-y-2">
    <Label className="text-xs">R (0-1)</Label>
    <Input
      type="number"
      step="0.01"
      min="0"
      max="1"
      value={colorR}
      onChange={(e) => setColorR(e.target.value)}
    />
  </div>
  <div className="space-y-2">
    <Label className="text-xs">G (0-1)</Label>
    <Input
      type="number"
      step="0.01"
      min="0"
      max="1"
      value={colorG}
      onChange={(e) => setColorG(e.target.value)}
    />
  </div>
  <!-- B and A similar -->
</div>
```

### ✅ After
```tsx
<div className="grid grid-cols-4 gap-2">
  <div className="space-y-1">
    <Label className="text-xs font-medium">R (0-1)</Label>
    <Input
      type="number"
      step="0.01"
      min="0"
      max="1"
      value={colorR}
      onChange={(e) => setColorR(e.target.value)}
      className="text-xs h-9"
    />
  </div>
  <div className="space-y-1">
    <Label className="text-xs font-medium">G (0-1)</Label>
    <Input
      type="number"
      step="0.01"
      min="0"
      max="1"
      value={colorG}
      onChange={(e) => setColorG(e.target.value)}
      className="text-xs h-9"
    />
  </div>
  <!-- B and A similar -->
</div>
```

**Benefit**: Tighter spacing, consistent input heights, better label weight.

---

## 10. Section with Description

### ❌ Before
```tsx
<Section title="1. Set Fills" description="Apply solid colors to nodes">
  <div className="space-y-3">
    <div className="space-y-2"><Label>Fill Color</Label>
      <!-- inputs -->
    </div>
  </div>
</Section>
```

### ✅ After
```tsx
<Section title="1. Set Fills" description="Apply solid colors to nodes">
  <div className="space-y-3">
    <div className="space-y-1">
      <Label className="text-xs font-medium">Fill Color</Label>
      <!-- inputs -->
    </div>
  </div>
</Section>
```

**Benefit**: Section spacing (space-y-3) separates form groups, while label-input pairs are tighter (space-y-1).

---

## 11. Card-Based Layout (Hierarchy Panel)

### ❌ Before
```tsx
<Card className="p-4 space-y-3">
  <h4 className="font-semibold">1. Get Node Info</h4>
  <div className="space-y-2">
    <Label>Node ID</Label>
    <Input value={nodeId} onChange={handleChange} />
  </div>
  <Button onClick={handler} className="w-full">
    Get Node Info
  </Button>
</Card>
```

### ✅ After
```tsx
<Card className="p-4 space-y-3">
  <h4 className="text-sm font-semibold">1. Get Node Info</h4>
  <div className="space-y-1">
    <Label className="text-xs font-medium">Node ID</Label>
    <Input
      value={nodeId}
      onChange={handleChange}
      className="text-xs h-9"
    />
  </div>
  <Button onClick={handler} className="w-full">
    Get Node Info
  </Button>
</Card>
```

**Benefit**: Consistent card title sizing, proper form field spacing.

---

## 12. Tab Content (Style Panel)

### ❌ Before
```tsx
<TabsContent value="create-fill" className="space-y-3 mt-3">
  <div className="space-y-2">
    <Label className="text-xs">Style Name</Label>
    <Input
      type="text"
      placeholder="Primary/Red/500"
      value={fillStyleName}
      onChange={(e) => setFillStyleName(e.target.value)}
      className="text-xs h-8"
    />
  </div>
  <Button className="w-full h-8 text-xs">
    Create Fill Style
  </Button>
</TabsContent>
```

### ✅ After
```tsx
<TabsContent value="create-fill" className="space-y-3 mt-3">
  <div className="space-y-1">
    <Label className="text-xs font-medium">Style Name</Label>
    <Input
      type="text"
      placeholder="Primary/Red/500"
      value={fillStyleName}
      onChange={(e) => setFillStyleName(e.target.value)}
      className="text-xs h-9"
    />
  </div>
  <Button className="w-full text-xs">
    Create Fill Style
  </Button>
</TabsContent>
```

**Benefit**: Consistent heights, better label styling, natural button height.

---

## Visual Comparison Summary

| Element | Before | After | Improvement |
|---------|--------|-------|-------------|
| Label-Input Spacing | `space-y-2` (8px) | `space-y-1` (4px) | Tighter, clearer relationship |
| Input Height | Mixed (h-8, default) | `h-9` (36px) | Consistent, comfortable |
| Label Style | Inconsistent | `text-xs font-medium` | Professional, readable |
| Color Picker Width | `w-16` (64px) | `w-20` (80px) | Easier to click |
| Button Height | Explicit `h-8` | Default (40px) | More comfortable |
| Hex Input | No flex-1 | `flex-1` | Fills available space |
| Helper Text | Often missing | Added where helpful | Better guidance |
| Grid Gaps | Mixed | Consistent `gap-2` | Uniform spacing |

---

## Accessibility Improvements

1. **Better Label Association**: Labels are now consistently styled and clearly associated with inputs
2. **Improved Contrast**: Using font-medium improves readability
3. **Consistent Touch Targets**: h-9 (36px) meets minimum touch target guidelines
4. **Clear Hierarchy**: Visual weight helps users scan forms quickly
5. **Helper Text**: Additional context reduces errors

---

## Design System Benefits

### Consistency
- Every panel follows the same patterns
- Developers know what to expect
- Easier to maintain and extend

### Efficiency
- Grid layouts reduce vertical scrolling
- Logical grouping improves workflow
- Clear visual hierarchy aids scanning

### Professional Appearance
- Polished, cohesive design
- Modern UI/UX standards
- Builds user confidence

### Developer Experience
- Clear patterns to follow
- Reusable component structures
- Easier to onboard new developers

---

## Implementation Tips

1. **Start with Labels**: Update all labels first for immediate visual improvement
2. **Then Spacing**: Adjust space-y values systematically
3. **Input Heights**: Update all inputs for consistency
4. **Color Pickers**: Follow the pattern exactly for best results
5. **Grids Last**: Grid layouts benefit from consistent inner spacing
6. **Test Often**: Check each section after updating

---

## Common Patterns Reference

### Pattern 1: Simple Text Input
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Label</Label>
  <Input className="text-xs h-9" placeholder="..." />
</div>
```

### Pattern 2: Number Input with Range
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Opacity</Label>
  <Input
    type="number"
    min="0"
    max="1"
    step="0.1"
    className="text-xs h-9"
  />
</div>
```

### Pattern 3: Select Dropdown
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Type</Label>
  <Select value={value} onValueChange={setValue}>
    <SelectTrigger className="text-xs h-9">
      <SelectValue />
    </SelectTrigger>
    <SelectContent>
      <SelectItem value="option">Option</SelectItem>
    </SelectContent>
  </Select>
</div>
```

### Pattern 4: Color Picker
```tsx
<div className="space-y-1">
  <Label className="text-xs font-medium">Color</Label>
  <div className="flex gap-2">
    <Input
      type="color"
      value={color}
      onChange={(e) => setColor(e.target.value)}
      className="w-20 h-9 p-1"
    />
    <Input
      type="text"
      value={color}
      onChange={(e) => setColor(e.target.value)}
      placeholder="#FF0000"
      className="flex-1 text-xs h-9"
    />
  </div>
</div>
```

### Pattern 5: Grid of Inputs
```tsx
<div className="grid grid-cols-2 gap-2">
  <div className="space-y-1">
    <Label className="text-xs font-medium">Width</Label>
    <Input type="number" className="text-xs h-9" />
  </div>
  <div className="space-y-1">
    <Label className="text-xs font-medium">Height</Label>
    <Input type="number" className="text-xs h-9" />
  </div>
</div>
```

---

**Document Purpose**: Provide clear examples for implementing consistent UI improvements

**Usage**: Reference these patterns when updating remaining panels
