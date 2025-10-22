# Figma Export Quick Start Guide

## 🚀 Two Main Workflows

### 1️⃣ Design Review & Iteration
**Goal**: Export → View → Fix → Verify

```bash
# Export design
figma_export_node(nodeId="1:123", format="PNG")

# View it
Read("/tmp/figma-mcp-exports/export_1:123_xyz.png")

# Make fixes based on what you see
figma_bind_variable(buttonId, "fills", colorVariableId)

# Verify changes
figma_export_node(nodeId="1:123", format="PNG")
```

### 2️⃣ Asset Import to Projects
**Goal**: Export assets from Figma → Copy to your project

```bash
# Export an icon
figma_export_node(nodeId="2:456", format="SVG")

# Copy to your project
cp /tmp/figma-mcp-exports/export_2:456_xyz.svg ./src/assets/icons/search.svg

# Use in your app
<img src="/assets/icons/search.svg" />
```

---

## 📝 Quick Commands

### Export Current Selection
```bash
figma_get_selection()
# Returns: {nodeId: "1:123", name: "Button"}

figma_export_node(nodeId="1:123", format="PNG")
# Returns: File path + metadata
```

### Find & Export Icon
```bash
figma_find_nodes(name="search-icon")
# Returns: {id: "2:456", name: "icon-search"}

figma_export_node(nodeId="2:456", format="SVG")
# Returns: /tmp/figma-mcp-exports/export_2:456_timestamp.svg
```

### Batch Export Icons
```bash
# Find all icons
figma_search_nodes(query="icon", type="COMPONENT")

# Loop and export each
for each icon:
  figma_export_node(nodeId=icon.id, format="SVG")
  cp <file_path> ./src/assets/icons/<icon_name>.svg
```

---

## 🎨 Format Guide

| What | Format | Why |
|------|--------|-----|
| Icons | **SVG** | Scalable, small size |
| Logos | **SVG** | Scalable, crisp at any size |
| Screenshots | **PNG** | Transparency + quality |
| Photos | **JPG** | Smaller file size |
| Print | **PDF** | Vector-based documents |

---

## 💡 Pro Tips

### 1. Export at Different Scales
```bash
# Standard (1x)
figma_export_node(nodeId, format="PNG", scale=1)

# Retina (2x)
figma_export_node(nodeId, format="PNG", scale=2)

# High DPI (3x)
figma_export_node(nodeId, format="PNG", scale=3)
```

### 2. Organize Project Assets
```
src/assets/
├── icons/          # SVG icons
├── images/         # PNG/JPG images
├── logos/          # Brand logos
└── illustrations/  # Complex graphics
```

### 3. Use Clear Names in Figma
✅ Good: `icon-search`, `icon-menu`, `logo-primary`
❌ Bad: `Icon 1`, `Rectangle`, `Component 2`

---

## 🎯 Common Use Cases

### Export All Icons from Component Library
```javascript
// 1. Switch to Component Library
figma_get_all_pages()
figma_switch_page("Component Library")

// 2. Find all icons
icons = figma_find_nodes(type="COMPONENT", name="icon")

// 3. Export each
icons.forEach(icon => {
  result = figma_export_node(icon.id, "SVG")
  name = icon.name.replace("icon-", "")
  cp(result.filePath, `./src/assets/icons/${name}.svg`)
})
```

### Export Design for Review
```bash
# Get what's selected
selection = figma_get_selection()

# Export it
export = figma_export_node(selection.nodeId, "PNG")

# View it
Read(export.filePath)

# Claude analyzes: "The button's background color is too dark..."

# Fix it
figma_bind_variable(buttonId, "fills", lighterColorVar)

# Verify
export2 = figma_export_node(selection.nodeId, "PNG")
Read(export2.filePath)

# Claude confirms: "Perfect! Much better contrast now."
```

### Export App Mockup for Handoff
```bash
# Export mobile screen at 2x
figma_export_node(screenId, format="PNG", scale=2)

# Copy to project docs
cp /tmp/.../export.png ./docs/designs/home-screen@2x.png

# Developers can now reference the exact design
```

---

## 📚 Full Documentation

See [EXPORT_WORKFLOWS.md](./EXPORT_WORKFLOWS.md) for comprehensive examples and advanced workflows.

---

## 🎉 Example Session

```
User: "Export the search icon and add it to my project"

Claude:
1. Finding the search icon...
   figma_find_nodes(name="search-icon")
   → Found: {id: "2:456", name: "icon-search"}

2. Exporting as SVG...
   figma_export_node(nodeId="2:456", format="SVG")
   → Exported to: /tmp/figma-mcp-exports/export_2:456_1234567890.svg

3. Copying to your project...
   cp /tmp/figma-mcp-exports/export_2:456_1234567890.svg ./src/assets/icons/search.svg
   → ✅ Done!

Your search icon is now at: ./src/assets/icons/search.svg

You can use it in your app:
<img src="/assets/icons/search.svg" alt="Search" width="24" height="24" />
```

---

**Ready to start?** Try: `figma_get_selection()` then `figma_export_node()`! 🚀
