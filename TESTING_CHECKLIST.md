# Figma MCP Server - Testing Checklist

Use this checklist to verify your installation and test all features.

## Pre-Flight Checklist

### ✅ Prerequisites

- [ ] Java 17+ installed
  ```bash
  java -version
  # Should show version 17 or higher
  ```

- [ ] Claude Code installed and running
  ```bash
  # Download from: https://claude.ai/download
  ```

- [ ] Figma Desktop App installed
  ```bash
  # Download from: https://www.figma.com/downloads/
  ```

- [ ] Server JAR exists
  ```bash
  ls -lh server/build/libs/server-all.jar
  # Should show ~25MB file
  ```

---

## Installation Checklist

### ✅ Step 1: Get Absolute Path

- [ ] Navigate to server directory
  ```bash
  cd server
  ```

- [ ] Get absolute path
  ```bash
  pwd
  # Example output: /Users/you/Projects/FigmaMcp/server
  ```

- [ ] Construct JAR path
  ```
  [Your pwd output]/build/libs/server-all.jar
  Example: /Users/you/Projects/FigmaMcp/server/build/libs/server-all.jar
  ```

### ✅ Step 2: Configure Claude Code

- [ ] Create/edit MCP config file
  ```bash
  mkdir -p ~/.config/claude
  nano ~/.config/claude/mcp_config.json
  ```

- [ ] Add configuration (use YOUR absolute path!)
  ```json
  {
    "mcpServers": {
      "figma": {
        "command": "java",
        "args": [
          "-jar",
          "/ABSOLUTE/PATH/FROM/STEP/1/server-all.jar"
        ],
        "env": {
          "LOG_LEVEL": "info"
        }
      }
    }
  }
  ```

- [ ] Save and verify
  ```bash
  cat ~/.config/claude/mcp_config.json
  # Check that path is correct
  ```

### ✅ Step 3: Restart Claude Code

- [ ] Quit Claude Code completely
- [ ] Restart Claude Code
- [ ] Wait for startup to complete

### ✅ Step 4: Build Figma Plugin

- [ ] Navigate to plugin directory
  ```bash
  cd figma-plugin
  ```

- [ ] Install dependencies
  ```bash
  npm install
  ```

- [ ] Build plugin
  ```bash
  npm run build
  ```

### ✅ Step 5: Load Plugin in Figma

- [ ] Open Figma Desktop App
- [ ] Go to: **Plugins → Development → Import plugin from manifest...**
- [ ] Navigate to and select: `figma-plugin/manifest.json`
- [ ] Plugin should now appear in development plugins list

---

## Verification Tests

### ✅ Test 1: MCP Server Discovery

**What**: Verify Claude Code sees the MCP server

**Steps**:
- [ ] Open Claude Code
- [ ] Check MCP servers list/status
- [ ] "figma" server should appear

**Expected**: ✅ Figma MCP server is listed and running

**If Failed**:
- Check `mcp_config.json` path is absolute
- Check JAR file exists at that path
- Check Claude Code logs: `~/Library/Logs/Claude Code/`
- Restart Claude Code

---

### ✅ Test 2: List Available Tools

**What**: Verify Claude can see Figma tools

**Steps**:
- [ ] Open Claude Code chat
- [ ] Ask: `"What Figma tools do you have available?"`

**Expected**: Claude lists:
- [ ] figma_create_rectangle
- [ ] figma_create_text
- [ ] figma_get_selection
- [ ] figma_set_properties
- [ ] figma_get_node_info

**If Failed**:
- Server may not be starting properly
- Check server logs
- Try manual stdio test (see below)

---

### ✅ Test 3: Start Figma Plugin

**What**: Connect Figma plugin to server

**Steps**:
- [ ] Open Figma Desktop App
- [ ] Open any Figma file (or create new)
- [ ] Go to: **Plugins → Development → [Your Plugin Name]**
- [ ] Plugin UI should open

**Expected**:
- [ ] Plugin UI opens
- [ ] Shows "Connected to server" or similar status
- [ ] No connection errors

**If Failed**:
- Check server is running (MCP servers in Claude Code)
- Check port 8080 is not in use:
  ```bash
  lsof -i :8080
  ```
- Check plugin console for errors (Plugins → Development → Show console)

---

### ✅ Test 4: Create Rectangle (Full Integration)

**What**: End-to-end test of complete system

**Prerequisites**:
- [ ] Claude Code running with figma MCP server
- [ ] Figma plugin running in Figma Desktop App

**Steps**:
- [ ] In Claude Code chat, ask:
  ```
  "Create a red rectangle 200x100 pixels in Figma"
  ```

**Expected**:
- [ ] Claude responds positively
- [ ] A red rectangle appears in your Figma canvas
- [ ] Rectangle is 200px wide, 100px tall
- [ ] Rectangle has red fill color

**If Failed**:
- Check "No Figma plugin connected" error → Start Figma plugin
- Check WebSocket connection in server logs
- Check plugin console for errors

---

### ✅ Test 5: Create Text Node

**Steps**:
- [ ] Ask Claude:
  ```
  "Create a text node in Figma that says 'Hello World' with font size 24"
  ```

**Expected**:
- [ ] Text node appears with "Hello World"
- [ ] Font size is 24px

---

### ✅ Test 6: Get Selection

**Steps**:
- [ ] Select any node in Figma
- [ ] Ask Claude:
  ```
  "What is currently selected in Figma?"
  ```

**Expected**:
- [ ] Claude describes the selected node
- [ ] Includes node type, name, properties

---

### ✅ Test 7: Set Properties

**Prerequisites**: Have a node selected in Figma

**Steps**:
- [ ] Ask Claude:
  ```
  "Change the selected node to 500 pixels wide"
  ```

**Expected**:
- [ ] Selected node resizes to 500px width
- [ ] Claude confirms the change

---

### ✅ Test 8: Error Handling

**What**: Verify graceful error handling

**Steps**:
- [ ] Close Figma plugin
- [ ] Ask Claude to create a rectangle

**Expected**:
- [ ] Claude reports error about no Figma plugin connected
- [ ] No crash or hang
- [ ] Error message is clear

---

## Advanced Tests

### ✅ Test 9: Multiple Shapes

**Steps**:
- [ ] Ask Claude:
  ```
  "Create three rectangles in Figma: a red one 100x100, a blue one 150x150, and a green one 200x200"
  ```

**Expected**:
- [ ] Three rectangles created
- [ ] Correct sizes and colors

---

### ✅ Test 10: Complex Workflow

**Steps**:
- [ ] Ask Claude:
  ```
  "Create a red rectangle 200x100, then create a text node above it that says 'Button', then tell me what's selected"
  ```

**Expected**:
- [ ] Rectangle created
- [ ] Text created
- [ ] Claude reports current selection

---

## Manual Stdio Test (Advanced)

If Claude Code integration fails, test stdio directly:

### ✅ Test Initialize

```bash
cd server
echo '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"},"capabilities":{}}}' | java -jar build/libs/server-all.jar
```

**Expected Output** (first few lines):
```
Starting Figma MCP Server...
{"jsonrpc":"2.0","id":"1","result":{"protocolVersion":"2024-11-05"...}}
```

---

## Troubleshooting Guide

### Problem: "figma" doesn't appear in Claude Code

**Solutions**:
- [ ] Check config file exists: `cat ~/.config/claude/mcp_config.json`
- [ ] Verify absolute path is correct
- [ ] Check JAR exists at path: `ls [your-path]/server-all.jar`
- [ ] Restart Claude Code completely
- [ ] Check Claude Code logs for errors

---

### Problem: Tools listed but don't execute

**Solutions**:
- [ ] **Start Figma plugin!** (Most common issue)
- [ ] Check plugin shows "Connected"
- [ ] Verify server is running: `lsof -i :8080`
- [ ] Check server logs for WebSocket connection

---

### Problem: "No Figma plugin connected"

**Solutions**:
- [ ] Open Figma Desktop App
- [ ] Run the plugin: Plugins → Development → [Plugin Name]
- [ ] Wait for "Connected" status
- [ ] Check plugin console for errors

---

### Problem: Port 8080 already in use

**Solutions**:
- [ ] Find what's using it: `lsof -i :8080`
- [ ] Kill the process or change server port in `application.yaml`

---

## Debug Mode

Enable detailed logging:

### ✅ Update MCP Config

```json
{
  "mcpServers": {
    "figma": {
      "command": "java",
      "args": [...],
      "env": {
        "LOG_LEVEL": "debug"  ← Change to debug
      }
    }
  }
}
```

### ✅ Restart Claude Code

### ✅ Check Logs

**Server logs**: stdout/stderr in Claude Code
**Plugin logs**: Figma → Plugins → Development → Show console

---

## Success Criteria

All tests passing means:
- ✅ MCP server discovered by Claude Code
- ✅ Tools listed correctly
- ✅ Figma plugin connects
- ✅ End-to-end tool execution works
- ✅ Error handling is graceful

---

## Test Results Template

Use this to track your test results:

```
Date: _______________
Tester: _______________

Pre-Flight:
[ ] Java 17+
[ ] Claude Code installed
[ ] Figma Desktop installed
[ ] JAR exists

Installation:
[ ] MCP config created
[ ] Absolute path correct
[ ] Claude Code restarted
[ ] Plugin built
[ ] Plugin loaded

Verification:
[ ] Test 1: Server discovery ______
[ ] Test 2: List tools ______
[ ] Test 3: Plugin connection ______
[ ] Test 4: Create rectangle ______
[ ] Test 5: Create text ______
[ ] Test 6: Get selection ______
[ ] Test 7: Set properties ______
[ ] Test 8: Error handling ______

Advanced:
[ ] Test 9: Multiple shapes ______
[ ] Test 10: Complex workflow ______

Status: ✅ PASS / ❌ FAIL
Notes: _______________________________
```

---

**Happy Testing!** 🧪

If all tests pass, you're ready to build amazing things with Claude Code + Figma!