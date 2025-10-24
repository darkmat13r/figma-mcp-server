# MCP Configuration Examples

This document provides example configurations for connecting Claude Code to specific Figma files using the file-specific routing feature.

## Prerequisites

1. Figma MCP Server running on `localhost:1234` (SSE) and `localhost:8080` (WebSocket)
2. Figma desktop app with files open
3. Figma MCP plugin installed and running in your target files

## Getting Your File ID

### Method 1: From Figma Plugin UI

1. Open your Figma file
2. Run the Figma MCP plugin (Plugins → Development → Figma MCP Server)
3. Look at the "File ID" field in the Connection panel
4. Click "Copy" to copy the file ID

### Method 2: From Figma URL

The file ID is in your browser URL when viewing the file:
```
https://www.figma.com/file/abc123xyz/My-Design-File
                          ^^^^^^^^^
                          File ID
```

## Configuration Location

### macOS
```
~/Library/Application Support/Claude/claude_desktop_config.json
```

### Windows
```
%APPDATA%\Claude\claude_desktop_config.json
```

### Linux
```
~/.config/Claude/claude_desktop_config.json
```

## Configuration Examples

### Example 1: Single Figma File

Configure Claude to work with one Figma file (e.g., your design system).

**File ID:** `abc123xyz789`
**File Name:** Design System Components

```json
{
  "mcpServers": {
    "figma-design-system": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123xyz789"
      }
    }
  }
}
```

**Usage:**
- Open Claude Desktop
- Ask: "Create a button component in Figma"
- Components will be created in the Design System file

### Example 2: Multiple Figma Files

Configure Claude to work with multiple files (e.g., design system + marketing site).

**File 1:** `abc123` (Design System)
**File 2:** `def456` (Marketing Site)

```json
{
  "mcpServers": {
    "figma-design-system": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123"
      }
    },
    "figma-marketing-site": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=def456"
      }
    }
  }
}
```

**Usage:**
- Specify which file when asking Claude:
  - "Create a hero section in the marketing site"
  - "Add a new icon to the design system"
- Claude will route commands to the correct file based on context

### Example 3: Team Workflow

Multiple team members working on different files.

**Designer A:** Working on Mobile App (`mobile123`)
**Designer B:** Working on Web App (`webapp456`)

**Designer A's Config:**
```json
{
  "mcpServers": {
    "figma-mobile-app": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=mobile123"
      }
    }
  }
}
```

**Designer B's Config:**
```json
{
  "mcpServers": {
    "figma-web-app": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=webapp456"
      }
    }
  }
}
```

Both designers can work simultaneously without commands being sent to the wrong file.

### Example 4: Development vs Production

Separate configurations for different environments.

**Dev File:** `dev789` (Experimental designs)
**Prod File:** `prod012` (Production design system)

```json
{
  "mcpServers": {
    "figma-dev": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=dev789",
        "ENVIRONMENT": "development"
      }
    },
    "figma-prod": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=prod012",
        "ENVIRONMENT": "production"
      }
    }
  }
}
```

### Example 5: Custom Server Ports

If running the MCP server on non-default ports.

**SSE Port:** `3000`
**WebSocket Port:** `9000`
**File ID:** `custom123`

```json
{
  "mcpServers": {
    "figma-custom": {
      "command": "node",
      "args": ["/path/to/figma-mcp-server/dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:3000/sse?fileId=custom123"
      }
    }
  }
}
```

**Note:** You'll also need to update the Figma plugin's WebSocket URL to `ws://localhost:9000/figma?fileId=custom123`

## Setup Steps

### Step 1: Start the MCP Server

```bash
cd /path/to/figma-mcp-server
./gradlew run
```

or if using Docker:

```bash
docker run -p 1234:1234 -p 8080:8080 figma-mcp-server
```

### Step 2: Open Figma File and Plugin

1. Open your target Figma file
2. Go to Plugins → Development → Figma MCP Server
3. Copy the "MCP URL" from the plugin UI

### Step 3: Configure Claude Desktop

1. Open `claude_desktop_config.json`
2. Add/update the MCP server configuration
3. Paste the MCP URL you copied from the plugin
4. Save the file

### Step 4: Restart Claude Desktop

1. Quit Claude Desktop completely
2. Reopen Claude Desktop
3. Verify the MCP server shows in Settings → Features → MCP Servers

### Step 5: Test the Connection

Ask Claude:
```
Can you create a test rectangle in Figma?
```

The rectangle should appear in your target Figma file.

## Switching Between Files

### Method 1: Multiple Configurations

Configure multiple MCP servers (as shown in Example 2) and specify which file in your prompts.

### Method 2: Update Configuration

1. Update the `fileId` in your config file
2. Restart Claude Desktop
3. Now commands will target the new file

### Method 3: Named Contexts

Use descriptive names that make it clear which file you're targeting:

```json
{
  "mcpServers": {
    "figma-components-library": {
      ...
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123"
      }
    },
    "figma-landing-page": {
      ...
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=def456"
      }
    }
  }
}
```

Then in Claude:
- "Add a button to the components library"
- "Update the hero section on the landing page"

## Troubleshooting

### Claude Can't Connect to MCP Server

**Symptoms:** MCP server doesn't appear in Claude settings

**Solutions:**
1. Check `claude_desktop_config.json` syntax (use a JSON validator)
2. Verify file paths in `command` and `args`
3. Restart Claude Desktop completely
4. Check MCP server logs for errors

### Commands Go to Wrong File

**Symptoms:** Shapes appear in the wrong Figma file

**Solutions:**
1. Verify `fileId` matches the file you want to target
2. Copy file ID directly from plugin UI
3. Check for typos in the file ID
4. Ensure only one Claude session per file ID

### Plugin Shows "No Active WebSocket Connection"

**Symptoms:** Claude tries to execute commands but gets errors

**Solutions:**
1. Ensure Figma plugin is running
2. Click "Connect" in the plugin UI
3. Verify connection status shows "Connected"
4. Check firewall isn't blocking port 8080

### "File ID Not Found" Error

**Symptoms:** Server returns errors about missing file ID

**Solutions:**
1. Ensure URL includes `?fileId=<id>`
2. File ID should not be empty or "unknown"
3. Restart Figma and plugin if file ID shows as "unknown"

## Best Practices

### 1. Use Descriptive MCP Server Names

❌ Bad:
```json
"mcp1": { ... }
"mcp2": { ... }
```

✅ Good:
```json
"figma-design-system": { ... }
"figma-marketing-site": { ... }
```

### 2. Document Your File IDs

Keep a reference document with your file IDs:

```markdown
# Figma File IDs

- Design System: abc123xyz
- Marketing Site: def456uvw
- Mobile App: ghi789rst
```

### 3. One Plugin Instance Per File

Only run the MCP plugin once per Figma file to avoid conflicts.

### 4. Restart Claude After Config Changes

Always restart Claude Desktop after modifying `claude_desktop_config.json`.

### 5. Use Version Control for Configs

If working in a team, commit example configs (with placeholder file IDs) to your repository:

```json
{
  "mcpServers": {
    "figma-design-system": {
      "command": "node",
      "args": ["./dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=YOUR_FILE_ID_HERE"
      }
    }
  }
}
```

## Advanced Usage

### Environment Variables

You can use environment variables in your config:

```json
{
  "mcpServers": {
    "figma": {
      "command": "node",
      "args": ["./dist/index.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=${FIGMA_FILE_ID}",
        "LOG_LEVEL": "debug"
      }
    }
  }
}
```

Then set the environment variable before starting Claude:
```bash
export FIGMA_FILE_ID=abc123
```

### Custom Server Configuration

If you need to customize server settings:

```json
{
  "mcpServers": {
    "figma": {
      "command": "node",
      "args": [
        "./dist/index.js",
        "--port=1234",
        "--ws-port=8080",
        "--log-level=debug"
      ],
      "env": {
        "MCP_SERVER_URL": "http://localhost:1234/sse?fileId=abc123",
        "NODE_ENV": "production"
      }
    }
  }
}
```

## Getting Help

If you encounter issues not covered here:

1. Check server logs: `./gradlew run` output
2. Check plugin console: Figma → Plugins → Development → Console
3. Check Claude Desktop logs
4. Visit the diagnostics endpoint: `http://localhost:8080/diagnostics/sessions`
5. Review the troubleshooting guide in `FILE_SPECIFIC_ROUTING.md`

## Summary

The file-specific routing feature enables:
- ✅ Multiple Claude sessions working on different files
- ✅ Team collaboration without interference
- ✅ Precise control over which file receives commands
- ✅ Easy setup with copy-paste configuration

Simply copy the MCP URL from the Figma plugin and paste it into your Claude Desktop configuration!
