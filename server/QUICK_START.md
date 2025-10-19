# Figma MCP Server - Quick Start

Get up and running with Claude Code + Figma in 5 minutes.

## TL;DR

```bash
# 1. Server is already built at:
server/build/libs/server-all.jar

# 2. Configure Claude Code
nano ~/.config/claude/mcp_config.json

# 3. Add this (with YOUR absolute path):
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [
        "-jar",
        "/ABSOLUTE/PATH/TO/FigmaMcp/server/build/libs/server-all.jar"
      ],
      "env": {
        "LOG_LEVEL": "info"
      }
    }
  }
}

# 4. Restart Claude Code

# 5. Start Figma plugin (in Figma Desktop App)

# 6. Test in Claude Code:
# "Create a red rectangle in Figma"
```

---

## Get Your Absolute Path

```bash
cd server
pwd
# Copy the output, example: /Users/you/Projects/FigmaMcp/server
# Use in config: /Users/you/Projects/FigmaMcp/server/build/libs/server-all.jar
```

---

## Verify Everything Works

### ✅ Check 1: MCP Server Listed
Open Claude Code → Check MCP servers → "figma" should appear

### ✅ Check 2: Ask Claude
```
What Figma tools do you have?
```

Should list:
- figma_create_rectangle
- figma_create_text
- figma_get_selection
- figma_set_properties
- figma_get_node_info

### ✅ Check 3: Create Something
```
Create a blue rectangle 300x200 pixels in Figma
```

**IMPORTANT**: Figma plugin must be running!

---

## Common Issues

### "No Figma plugin connected"
→ Start the Figma plugin in Figma Desktop App

### Claude doesn't see "figma" server
→ Check absolute path in config
→ Restart Claude Code

### Tools listed but don't work
→ Make sure Figma plugin is running
→ Check server logs for WebSocket connection

---

## Architecture in 30 Seconds

```
Claude Code
    ↓ (stdio: JSON-RPC 2.0)
MCP Server (Ktor) - Application.kt starts stdio automatically
    ↓ (WebSocket)
Figma Plugin
    ↓ (Plugin API)
Figma Document
```

The server runs **both** stdio (for Claude) and WebSocket (for Figma) simultaneously.

---

## Example Interactions

### Create Shapes
```
Create a red circle 100x100 in Figma
Create a text node saying "Hello World"
```

### Get Info
```
What is selected in Figma?
Get info about node 123:456
```

### Modify
```
Change the selected node to 500 pixels wide
Set the fill color to #3B82F6
```

---

## Need More Help?

See **SETUP_GUIDE.md** for detailed documentation.

---

**Ready? Let's build!** 🚀