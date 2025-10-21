## Figma MCP Bridge Plugin

A modern Figma plugin built with TypeScript, React, and Tailwind CSS that connects to an MCP server via WebSocket.

## Features

- 🔌 WebSocket connection to MCP server
- 🎨 Design system operations (colors, typography)
- 📦 Node creation and manipulation
- 👁️ Real-time selection monitoring
- 🎯 Type-safe TypeScript implementation
- 💅 Beautiful UI with Tailwind CSS

## Prerequisites

- Node.js 18+
- npm or yarn
- Figma Desktop App

## Installation

```bash
# Install dependencies
npm install

# Build the plugin
npm run build
```

## Development

```bash
# Watch mode for development
npm run dev

# Lint code
npm run lint

# Type check
npm run typecheck
```

## Figma Installation

1. Open Figma Desktop App
2. Go to `Plugins` → `Development` → `Import plugin from manifest...`
3. Select `manifest.json` from this directory
4. Run from `Plugins` → `Development` → `Figma MCP Bridge`

## Usage

1. Ensure MCP server is running on `ws://localhost:8081/mcp`
2. Open the plugin in Figma
3. Click "Connect" to establish WebSocket connection
4. Use the Design System panel to create styles
5. Monitor selection in the Selection panel

## Architecture

### Plugin Thread (`src/plugin/code.ts`)
- Runs in Figma's sandbox environment
- Has access to Figma Plugin API
- Manages WebSocket connection
- Executes Figma operations

### UI Thread (`src/ui/`)
- Runs in iframe with browser APIs
- React-based component architecture
- Tailwind CSS for styling
- Communicates with plugin via `postMessage`

## Project Structure

```
src/
├── plugin/
│   └── code.ts              # Main plugin thread
└── ui/
    ├── components/          # React components
    ├── hooks/              # Custom hooks
    ├── types/              # TypeScript types
    ├── App.tsx             # Main app component
    ├── index.tsx           # Entry point
    ├── index.html          # HTML template
    └── styles.css          # Tailwind styles
```

## Configuration

### Network Access

Edit `manifest.json` to allow your MCP server domain:

```json
{
  "networkAccess": {
    "allowedDomains": [
      "ws://localhost:8081",
      "wss://your-server.com"
    ]
  }
}
```

### Server URL

Default: `ws://localhost:8081/mcp`

Change in the ConnectionPanel component or store in plugin data.

## Available Operations

- **Create Color Styles**: Generate color styles from tokens
- **Create Text Styles**: Generate typography styles for mobile/tablet
- **Create Nodes**: Create rectangles, frames, text, etc.
- **Get Selection**: Retrieve information about selected nodes

## Troubleshooting

**Build errors:**
```bash
rm -rf node_modules dist
npm install
npm run build
```

**Plugin not loading:**
- Verify you're using Figma Desktop (not browser)
- Check console for errors
- Reimport manifest.json

**WebSocket connection fails:**
- Ensure MCP server is running
- Check server URL matches allowed domains
- Verify no firewall blocking connection

## License

MIT
