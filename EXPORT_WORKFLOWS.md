# Figma MCP Export Workflows

The `figma_export_node` tool enables powerful workflows for both design iteration and asset management. When you export a node, the MCP server saves it to a temporary file and returns the file path, enabling you to view, copy, or manipulate the exported assets.

## 🎨 Workflow 1: Design Review & Iteration

### Use Case
Export designs from Figma, visually review them with Claude, identify issues, make fixes in Figma, and verify the changes - all in one seamless workflow.

### Step-by-Step

```
1. Export the design
   → figma_export_node(nodeId="1:123", format="PNG")

   Returns:
   ✅ Export successful!
   📁 File path: /tmp/figma-mcp-exports/export_1:123_1234567890.png
   📏 Dimensions: 375x812
   💾 Size: 45.3 KB

2. View the exported image
   → Read("/tmp/figma-mcp-exports/export_1:123_1234567890.png")

   Claude can now SEE the actual design visually

3. Claude analyzes the design
   "I can see several issues:
   - Button background is too dark (#1a1a1a)
   - Text color lacks contrast
   - Padding is inconsistent
   - Corner radius doesn't match design system"

4. Make fixes in Figma
   → figma_bind_variable(buttonId, "fills", lighterColorVariableId)
   → figma_bind_variable(buttonId, "cornerRadius", mediumRadiusVariableId)
   → figma_bind_variable(buttonId, "paddingLeft", spacingMdVariableId)

5. Verify the changes
   → figma_export_node(nodeId="1:123", format="PNG")
   → Read(new_file_path)

   "Perfect! The button now uses the correct design tokens."
```

### Benefits
- **Visual feedback loop**: See exactly what your changes look like
- **Catch design issues**: Colors, spacing, typography mistakes are visible
- **Iterative refinement**: Export → Review → Fix → Repeat
- **Design system compliance**: Verify variables are properly bound

---

## 📦 Workflow 2: Asset Import to Projects

### Use Case
Export icons, logos, illustrations, or mockups from Figma and import them directly into your project's codebase.

### Single Asset Export

```bash
# 1. Find the asset you want to export
figma_find_nodes(name="search-icon", type="COMPONENT")

# 2. Export as SVG (best for icons)
figma_export_node(nodeId="2:456", format="SVG")

# Returns:
# 📁 File path: /tmp/figma-mcp-exports/export_2:456_1234567890.svg

# 3. Copy to your project
Bash(cp /tmp/figma-mcp-exports/export_2:456_1234567890.svg ./src/assets/icons/search.svg)

# 4. Use in your app
<img src="/assets/icons/search.svg" alt="Search" />
```

### Batch Asset Export

Export multiple icons at once:

```javascript
// 1. Find all icon components
const icons = await figma_find_nodes({
  type: "COMPONENT",
  name: "icon"  // Matches: "icon-search", "icon-menu", etc.
});

// 2. Export each icon
for (const icon of icons) {
  // Export as SVG
  const result = await figma_export_node({
    nodeId: icon.id,
    format: "SVG"
  });

  // Extract icon name from component name
  const iconName = icon.name.replace("icon-", "");

  // Copy to project with proper naming
  await Bash(`cp ${result.filePath} ./src/assets/icons/${iconName}.svg`);
}

// Result: All icons imported to ./src/assets/icons/
// - search.svg
// - menu.svg
// - close.svg
// - etc.
```

### Format Recommendations

| Asset Type | Recommended Format | Why? |
|------------|-------------------|------|
| Icons | SVG | Scalable, small file size, perfect for web |
| Logos | SVG | Scalable, maintains quality at any size |
| Illustrations | SVG or PNG | SVG if simple, PNG if complex gradients |
| Screenshots | PNG | Supports transparency, good quality |
| Mockups | PNG | High quality, transparency support |
| Photos/Backgrounds | JPG | Smaller file size, photos don't need transparency |
| App Assets (iOS/Android) | PNG | Required format, can export at multiple scales |

### Export at Multiple Scales

For responsive images or retina displays:

```bash
# Export at 1x (default)
figma_export_node(nodeId="3:789", format="PNG", scale=1)
# → icon.png

# Export at 2x (retina)
figma_export_node(nodeId="3:789", format="PNG", scale=2)
# → icon@2x.png

# Export at 3x (high DPI)
figma_export_node(nodeId="3:789", format="PNG", scale=3)
# → icon@3x.png
```

---

## 🎯 Workflow 3: Design Documentation

### Use Case
Generate visual documentation of your design system components for developers and stakeholders.

```bash
# 1. Export all button variants
const buttons = await figma_find_nodes({type: "COMPONENT", name: "Button"});

for (const button of buttons) {
  const result = await figma_export_node({
    nodeId: button.id,
    format: "PNG",
    scale: 2  // High quality for docs
  });

  // Copy to documentation folder
  await Bash(`cp ${result.filePath} ./docs/components/${button.name}.png`);
}

# 2. Create markdown documentation
# Now your docs can reference: ![Primary Button](./components/Button-Primary.png)
```

---

## 💡 Pro Tips

### 1. Use Descriptive Names in Figma
Name your components clearly so batch exports are organized:
- ✅ `icon-search`, `icon-menu`, `icon-close`
- ❌ `Icon 1`, `Icon 2`, `Icon 3`

### 2. Export from Component Library
Always export from the Component Library page to get the canonical version, not instances.

### 3. SVG Optimization
After exporting SVGs, you can optimize them:
```bash
# Install SVGO
npm install -g svgo

# Optimize exported SVG
svgo /tmp/figma-mcp-exports/export_123.svg -o ./src/assets/icons/search.svg
```

### 4. Organize Exports by Type
Structure your project assets:
```
src/assets/
├── icons/          # SVG icons
├── images/         # PNG/JPG images
├── logos/          # Brand logos (SVG)
└── illustrations/  # Complex graphics
```

### 5. Automate with Scripts
Create a script to export all design system assets:
```bash
#!/bin/bash
# export-design-system.sh

# Export all icons
figma_find_nodes type="COMPONENT" name="icon" | \
  xargs -I {} figma_export_node nodeId={} format="SVG"

# Export all illustrations
figma_find_nodes type="COMPONENT" name="illustration" | \
  xargs -I {} figma_export_node nodeId={} format="PNG" scale=2
```

---

## 🔄 Complete Example: Icon Library Import

Here's a complete workflow for importing an entire icon library from Figma:

```javascript
// Step 1: Switch to Component Library page
await figma_get_all_pages();
await figma_switch_page("Component Library");

// Step 2: Find all icon components
const icons = await figma_search_nodes({
  query: "icon",
  type: "COMPONENT"
});

console.log(`Found ${icons.length} icons to export`);

// Step 3: Create destination directory
await Bash("mkdir -p ./src/assets/icons");

// Step 4: Export and import each icon
const results = [];
for (const icon of icons) {
  try {
    // Export as SVG
    const exportResult = await figma_export_node({
      nodeId: icon.id,
      format: "SVG"
    });

    // Extract clean name (remove "Icon/" prefix, convert to kebab-case)
    const cleanName = icon.name
      .replace(/^Icon\//, '')
      .toLowerCase()
      .replace(/\s+/g, '-');

    // Copy to project with clean name
    await Bash(`cp "${exportResult.filePath}" "./src/assets/icons/${cleanName}.svg"`);

    results.push({
      name: icon.name,
      fileName: `${cleanName}.svg`,
      size: exportResult.size,
      success: true
    });

    console.log(`✅ Exported: ${icon.name} → ${cleanName}.svg`);
  } catch (error) {
    console.error(`❌ Failed to export ${icon.name}:`, error);
    results.push({
      name: icon.name,
      success: false,
      error: error.message
    });
  }
}

// Step 5: Generate index file for easy importing
const indexContent = results
  .filter(r => r.success)
  .map(r => `export { default as ${r.fileName.replace('.svg', '')} } from './${r.fileName}';`)
  .join('\n');

await Write("./src/assets/icons/index.ts", indexContent);

console.log(`\n🎉 Successfully imported ${results.filter(r => r.success).length} icons!`);
```

---

## 📊 Export Comparison

| Workflow | Primary Use | Format | Scale | Destination |
|----------|-------------|--------|-------|-------------|
| Design Review | Visual QA | PNG | 1x-2x | Temporary (review only) |
| Icon Import | Web/Mobile | SVG | N/A | `./src/assets/icons/` |
| Image Assets | App Assets | PNG | 1x-3x | `./src/assets/images/` |
| Documentation | Docs/Guides | PNG | 2x | `./docs/components/` |
| Print/PDF | Marketing | PDF | N/A | `./assets/print/` |

---

## 🚀 Getting Started

Try these commands to get started:

```bash
# 1. Export current selection for review
figma_get_selection()
figma_export_node(nodeId=<returned_id>, format="PNG")
Read(<returned_file_path>)

# 2. Export an icon to your project
figma_find_nodes(name="search-icon")
figma_export_node(nodeId=<returned_id>, format="SVG")
Bash(cp <returned_file_path> ./src/assets/icons/search.svg)

# 3. See the export in action
Read("./src/assets/icons/search.svg")
```

---

## 🎓 Learn More

- **Design System Integration**: Use with `figma_get_variables` to ensure exported assets use design tokens
- **Component Library**: Always export from Component Library page for canonical versions
- **Batch Processing**: Use `figma_find_nodes` + loops for bulk exports
- **File Organization**: Structure your project assets logically for maintainability

Happy exporting! 🎨✨
