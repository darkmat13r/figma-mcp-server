# Remaining UI Updates Checklist

## Quick Find-and-Replace Patterns

Use these patterns to quickly update the remaining panels:

### 1. Label Styling
**Find**: `<Label>`
**Replace**: `<Label className="text-xs font-medium">`

**Find**: `<Label className="text-xs">`
**Replace**: `<Label className="text-xs font-medium">`

### 2. Spacing Updates
**Find**: `<div className="space-y-2">`
**Replace**: `<div className="space-y-1">`
(Only for direct label-input pairs, not section-level spacing)

### 3. Input Heights
**Find**: `className="text-xs h-8"`
**Replace**: `className="text-xs h-9"`

**Find**: Input elements without explicit height
**Add**: `h-9` to className

### 4. Color Input Width
**Find**: `className="w-16 h-8 p-1"` or `className="w-16 h-9 p-1"`
**Replace**: `className="w-20 h-9 p-1"`

### 5. Select Triggers
**Find**: `<SelectTrigger className="text-xs h-8">`
**Replace**: `<SelectTrigger className="text-xs h-9">`

**Find**: `<SelectTrigger className="text-xs">`
**Replace**: `<SelectTrigger className="text-xs h-9">`

### 6. Button Heights
**Find**: `className="w-full h-8 text-xs"`
**Replace**: `className="w-full text-xs"`

**Find**: `className="flex-1 h-8 text-xs"`
**Replace**: `className="flex-1 text-xs"`

---

## Panel-Specific Updates

### TypographyTestPanel

**Lines to Update**:
- **199-215**: Node Selection section - update label and spacing
- **220-234**: Text Content section - standardize form layout
- **238-376**: Text Style section - large form with many controls
  - Update grid layouts (lines 240-265, 267-296)
  - Line height controls (298-319)
  - Letter spacing controls (321-340)
  - Text case and decoration (342-370)
- **380-413**: Text Alignment section
- **417-436**: Text Auto Resize section
- **440-470**: Text Truncation section
- **474-511**: Load Font section

**Color Pickers**: None (text-only form)

**Key Changes**:
- Change all `space-y-2` to `space-y-1` for label-input pairs
- Add `font-medium` to all Labels
- Update all input heights to `h-9`
- Ensure Select components have `text-xs h-9`

---

### HierarchyTestPanel

**Lines to Update**:
- **261-279**: Node Selection Helper
- **284-334**: Section 1 - Node Information
  - Cards at lines 287-305, 308-316, 319-332
- **339-436**: Section 2 - Node Search & Query
  - Find Nodes card (342-391)
  - Search Nodes card (394-435)
- **442-570**: Section 3 - Node Hierarchy
  - Get Children (445-475)
  - Get Parent (478-496)
  - Move Node (499-535)
  - Clone Node (538-568)
- **575-654**: Section 4 - Page Management
  - Multiple cards with form controls

**Color Pickers**: None

**Key Changes**:
- Update Card internal padding: `className="p-4 space-y-3"`
- All form fields need `space-y-1` instead of `space-y-2`
- Update all input/button heights
- Checkbox sections may need slight adjustment

---

### ComponentsTestPanel

**Lines to Update**:
- **295-313**: Node Selection Helper
- **318-384**: Section 1 - Component Creation
  - Create Component card (321-355)
  - Create Component Set card (358-382)
- **389-488**: Section 2 - Component Properties
  - Large property builder (392-487)
  - Grid layouts and list management
- **494-626**: Section 3 - Instance Management
  - Set Instance Properties (497-571)
  - Detach Instance (574-595)
  - Swap Instance (598-624)
- **631-705**: Section 4 - Variants
  - Add Variant card with property builder

**Color Pickers**: None

**Key Changes**:
- Focus on consistent card spacing
- Property list displays need attention
- Grid layouts in property builders
- All form controls need height/spacing updates

---

### VariablesTestPanel

**Lines to Update**:
- **291-309**: Node Selection Helper
- **314-373**: Section 1 - Collection Management
- **379-527**: Section 2 - Variable Creation
  - **CRITICAL**: Multiple color pickers (lines 454-516)
  - RGBA grid inputs (468-513)
- **532-633**: Section 3 - Variable Binding
  - Bind Variable (535-591)
  - Unbind Variable (594-632)
- **639-791**: Section 4 - Variable Values
  - **CRITICAL**: More color pickers (lines 705-766)
  - RGBA grid inputs (720-765)

**Color Pickers** (PRIORITY):
1. Lines 454-467: Create Variable color picker
2. Lines 468-513: RGBA inputs grid
3. Lines 705-718: Set Value color picker
4. Lines 720-765: RGBA inputs grid

**Key Changes**:
```tsx
// Update color picker pattern (appears twice):
<div className="flex gap-2 items-center">  // Keep items-center
  <Input
    type="color"
    value={colorHex}
    onChange={(e) => setColorHex(e.target.value)}
    className="w-20 h-9 p-1"  // Changed from w-16, added h-9
  />
  <Input
    value={colorHex}
    onChange={(e) => setColorHex(e.target.value)}
    placeholder="#FF0000"
    className="flex-1 text-xs h-9"  // Added flex-1 and h-9
  />
</div>

// RGBA grid needs consistent spacing:
<div className="grid grid-cols-4 gap-2">
  <div className="space-y-1">  // Changed from space-y-2 if needed
    <Label className="text-xs font-medium">R (0-1)</Label>  // Added font-medium
    <Input className="text-xs h-9" />  // Ensure h-9
  </div>
  // ... repeat for G, B, A
</div>
```

---

### ImageTestPanel

**Lines to Update**:
- **254-272**: Node Selection Helper
- **276-356**: Section 1 - Create Image
  - Grid layouts for width/height (290-311)
  - Grid layouts for X/Y position (313-332)
- **360-429**: Section 2 - Set Image Fill
  - Fill controls (362-398)
  - Rotation/Opacity grid (400-424)
- **434-546**: Section 3 - Export Node
  - Format/Scale grid (450-476)
  - Constraint controls (478-500)
- **551-623**: Section 4 - Get Image Fills

**Color Pickers**: None (image-focused)

**Key Changes**:
- Grid layouts need consistent spacing
- All form fields need updates
- Button patterns in results display
- Image preview section styling

---

## Verification Checklist

After updating each panel, verify:

- [ ] All `Label` components have `text-xs font-medium`
- [ ] Direct label-input pairs use `space-y-1`
- [ ] All inputs have `text-xs h-9`
- [ ] All Select triggers have `text-xs h-9`
- [ ] Color pickers use `w-20 h-9 p-1` for color input
- [ ] Color hex inputs use `flex-1 text-xs h-9`
- [ ] Buttons have removed explicit `h-8`
- [ ] Grid layouts use `gap-2`
- [ ] Grid items maintain `space-y-1` pattern
- [ ] Helper text uses `text-xs text-muted-foreground`
- [ ] Card internal spacing is `space-y-3`
- [ ] Section-level spacing remains `space-y-4`

## Testing Each Panel

1. Open the panel in Figma plugin
2. Check visual alignment of all form fields
3. Verify color pickers (if any) look consistent
4. Test grid layouts align properly
5. Check button sizing is uniform
6. Verify labels are clearly readable
7. Test form submission and interactions

---

## Order of Implementation

**Recommended order** (easiest to hardest):

1. ✅ TestPanel (DONE)
2. ✅ StylingTestPanel (DONE)
3. ✅ StyleTestPanel (DONE)
4. ⏳ TypographyTestPanel (straightforward, no color pickers)
5. ⏳ ImageTestPanel (moderate, good grid layout practice)
6. ⏳ HierarchyTestPanel (many cards, repetitive patterns)
7. ⏳ ComponentsTestPanel (complex property builders)
8. ⏳ VariablesTestPanel (most complex, multiple color pickers with RGBA grids)

---

## Time Estimates

- **TypographyTestPanel**: ~30 minutes
- **ImageTestPanel**: ~30 minutes
- **HierarchyTestPanel**: ~45 minutes
- **ComponentsTestPanel**: ~45 minutes
- **VariablesTestPanel**: ~60 minutes (due to complexity)

**Total remaining**: ~3.5 hours

---

## Common Mistakes to Avoid

1. ❌ Don't change section-level `space-y-4` to `space-y-1`
2. ❌ Don't remove `items-center` from color picker flex containers
3. ❌ Don't change Card padding `p-4`
4. ❌ Don't modify Alert structures
5. ❌ Don't change grid column counts (`grid-cols-2`, `grid-cols-3`, etc.)
6. ❌ Don't remove `flex-1` from hex inputs in color pickers
7. ❌ Don't change `gap-2` to other values

✅ **DO** maintain existing logic and state management
✅ **DO** keep all event handlers unchanged
✅ **DO** preserve all accessibility attributes
✅ **DO** maintain existing component structure

---

## Final Validation

Before considering complete:

1. Side-by-side comparison of all 8 panels
2. Screenshot each panel for documentation
3. Test each panel's primary functionality
4. Verify no console errors
5. Check responsive behavior
6. Validate in both light and dark modes (if applicable)
7. User acceptance testing with actual workflows

---

**Document Version**: 1.0
**Last Updated**: 2025-10-24
**Status**: 3 of 8 panels complete
