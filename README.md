# Figma MCP Server

> Control Figma with natural language through Claude Code. Build design systems, generate components, and maintain design-code consistency—all through conversational AI.

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org/)
[![MCP](https://img.shields.io/badge/MCP-2024--11--05-blue.svg)](https://spec.modelcontextprotocol.io/)

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Support-yellow?style=for-the-badge&logo=buy-me-a-coffee)](https://buymeacoffee.com/developeru)

## What Problem Does This Solve?

Creating consistent, scalable design systems is time-consuming and error-prone. This project solves that by enabling you to **control Figma through natural language prompts**, making design system creation and maintenance dramatically faster and more consistent.

### Primary Use Cases

**1. Design System Creation & Management**
- Create design tokens (colors, typography, spacing) through prompts
- Build variable collections and styles systematically
- Ensure consistency across your entire design system
- Update tokens globally with simple commands

**2. AI-Powered Design Generation**
- Generate UI components from descriptions
- Create consistent designs following your design system
- Iterate on designs through conversational prompts
- Automate repetitive design tasks

**3. Design-to-Code Workflow**
- Import designs with full access to design tokens
- Generate code with accurate design system values
- Maintain consistency between design and implementation
- Access all variables, styles, and components programmatically

### Why This Matters

When you have programmatic access to design tokens, variables, and components, code generation becomes:
- **Consistent**: Uses actual design system values, not approximations
- **Accurate**: References real component libraries and styles
- **Maintainable**: Updates automatically when design system changes
- **Scalable**: Works seamlessly across large design systems

## Features

- ✅ Create and modify Figma nodes using natural language
- ✅ Build complete design systems through prompts
- ✅ Manage variables, styles, and components programmatically
- ✅ Query design properties and selections
- ✅ Generate consistent, token-based designs
- ✅ Export designs with full design system context

## Quick Start

### Prerequisites

- Java 21+
- Claude Code installed
- Figma Desktop App
- (Optional) Docker and Docker Compose for containerized deployment

### 1. Build the Server

```bash
cd server
./gradlew clean assemble shadowJar -x test
```

This will create `server/build/libs/server-all.jar`

### 2. Build and Load Figma Plugin

First, build the plugin:

```bash
cd figma-plugin
npm install
npm run build
```

Then in Figma Desktop:
1. **Plugins → Development → Import plugin from manifest...**
2. Select `figma-plugin/manifest.json`
3. Run the plugin in any Figma file

### 3. Start the MCP Server

**Option A: Using Gradle (Development)**

```bash
cd server
./gradlew run
```

**Option B: Using Docker Compose (Production)**

```bash
docker-compose up -d
```

The server will start on port 8080.

### 4. Connect Plugin to Server

Back in the Figma plugin UI:
1. **Click the "Connect" button** to establish WebSocket connection
2. Once connected, **copy the MCP server URL** displayed in the plugin UI
   - Example: `http://localhost:8080/sse?fileId=local-design-test`

### 5. Configure Claude Code

Add the MCP server to Claude Code using the URL from the Figma plugin:

```bash
claude mcp add --transport sse figma "http://localhost:8080/sse?fileId=YOUR_FILE_ID"
```

**Note:** Replace `YOUR_FILE_ID` with the file ID shown in the Figma plugin (e.g., `local-design-test`)

### 6. Test with Claude Code

Restart Claude Code, then ask:
```
"Create a red rectangle 200x100 pixels in Figma"
```

Claude should create the rectangle! 🎉

## Example Prompts

Here's what you can do with natural language:

**Design System Creation:**
```
"Create a color palette with primary blue (#0066FF), secondary green (#00CC66),
and neutral grays from 100 to 900"

"Set up typography styles: Heading 1 (32px bold), Heading 2 (24px semibold),
Body (16px regular)"

"Create spacing variables: xs=4px, sm=8px, md=16px, lg=24px, xl=32px"
```

**Component Generation:**
```
"Create a button component with primary, secondary, and ghost variants"

"Design a card component with an image, title, description, and CTA button"

"Generate a navigation bar with logo, menu items, and a search input"
```

**Design Tasks:**
```
"Create a 3-column layout with cards showcasing our product features"

"Update all headings to use the new Inter font family"

"Export the selected components as PNG at 2x resolution"
```

## Available Tools (30+)

<details>
<summary><strong>Node Creation & Manipulation</strong></summary>

- `figma_create_rectangle` - Create rectangles and squares
- `figma_create_text` - Create text nodes
- `figma_create_frame` - Create frames/containers
- `figma_create_ellipse` - Create circles and ellipses
- `figma_create_star` - Create star/polygon shapes
- `figma_create_image` - Create image nodes
- `figma_get_selection` - Get currently selected nodes
- `figma_get_node_info` - Query node properties by ID
- `figma_move_node` - Reposition nodes
- `figma_rename_node` - Rename nodes
</details>

<details>
<summary><strong>Styling & Effects</strong></summary>

- `figma_set_fills` - Set fill colors/gradients
- `figma_set_strokes` - Set stroke properties
- `figma_set_corner_radius` - Set border radius
- `figma_set_effects` - Add shadows, blurs, etc.
- `figma_set_image_fill` - Apply images as fills
- `figma_load_font` - Load custom fonts
</details>

<details>
<summary><strong>Variables & Design Tokens</strong></summary>

- `figma_get_variables` - List all variables
- `figma_create_variable_collection` - Create variable sets
- `figma_create_variable` - Create design tokens
- `figma_set_variable_value` - Update token values
- `figma_bind_variable` - Bind tokens to properties
</details>

<details>
<summary><strong>Styles Management</strong></summary>

- `figma_get_styles` - List all styles
- `figma_create_color_style` - Create color styles
- `figma_create_text_style` - Create text styles
- `figma_set_fill_style_id` - Apply color styles
- `figma_set_text_style_id` - Apply text styles
</details>

<details>
<summary><strong>Hierarchy & Navigation</strong></summary>

- `figma_get_all_pages` - List all pages
- `figma_get_current_page_nodes` - Get nodes on current page
- `figma_get_children` - Get child nodes
- `figma_find_nodes` - Search nodes by name
</details>

<details>
<summary><strong>Export & Utilities</strong></summary>

- `figma_export_node` - Export as PNG/SVG/JPG
- `figma_notify` - Show notifications in Figma
</details>

## How It Works

```
┌─────────────┐   MCP/SSE    ┌──────────────┐   WebSocket   ┌──────────────┐   Plugin API   ┌────────┐
│ Claude Code │◄────────────►│ Ktor Server  │◄─────────────►│Figma Plugin  │◄──────────────►│ Figma  │
│             │              │  (Port 8080) │               │  (React UI)  │                │Desktop │
└─────────────┘              └──────────────┘               └──────────────┘                └────────┘
```

### Architecture Highlights

1. **Dual Transport System**: The server supports both SSE (for Claude Code) and WebSocket (for Figma plugin) simultaneously
2. **File-Specific Routing**: Each Figma file gets its own isolated connection via fileId parameter
3. **Real-time Communication**: Bidirectional communication between all components
4. **Type-Safe**: Full TypeScript/Kotlin type safety throughout the stack

## Documentation

| Document | Purpose |
|----------|---------|
| **[QUICK_START.md](server/QUICK_START.md)** | 5-minute setup guide |
| **[SETUP_GUIDE.md](server/SETUP_GUIDE.md)** | Complete setup instructions |
| **[INTEGRATION_FLOW.md](INTEGRATION_FLOW.md)** | How everything works together |
| **[IMPLEMENTATION_SUMMARY.md](server/IMPLEMENTATION_SUMMARY.md)** | Technical implementation details |
| **[MCP_IMPLEMENTATION.md](server/MCP_IMPLEMENTATION.md)** | MCP protocol specifics |

## Project Structure

```
FigmaMcp/
├── server/                          # MCP Server (Kotlin/Ktor)
│   ├── src/main/kotlin/com/figma/mcp/
│   │   ├── Application.kt           # Main entry (dual transport)
│   │   ├── protocol/
│   │   │   └── MCPTypes.kt          # MCP protocol types
│   │   ├── transport/
│   │   │   ├── StdioTransport.kt    # Stdio communication
│   │   │   └── SseTransport.kt      # SSE communication
│   │   ├── commands/mcp/
│   │   │   ├── InitializeCommand.kt # MCP handshake
│   │   │   ├── ListToolsCommand.kt  # Available tools
│   │   │   └── CallToolCommand.kt   # Execute tool
│   │   ├── services/
│   │   │   ├── FigmaToolExecutor.kt # Tool implementations
│   │   │   └── FigmaConnectionManager.kt # WebSocket manager
│   │   └── infrastructure/
│   │       └── DiModule.kt          # Dependency injection
│   ├── build.gradle.kts
│   └── Dockerfile
│
├── figma-plugin/                    # Figma Plugin (TypeScript)
│   ├── src/
│   │   ├── plugin/                  # Plugin code
│   │   └── ui/                      # React UI
│   ├── manifest.json
│   └── package.json
│
├── docker-compose.yml               # Docker deployment config
└── Documentation files (see above)
```

## Key Features

### 🎨 Design System First
- Create and manage design tokens (colors, typography, spacing)
- Build variable collections with modes (light/dark themes)
- Generate styles programmatically
- Maintain consistency across all designs

### 🤖 AI-Powered Workflows
- Natural language interface for all Figma operations
- Conversational design iteration
- Automated component generation
- Batch operations through prompts

### 🔗 Design-Code Consistency
- Direct access to design tokens for code generation
- Export components with full design system context
- Programmatic access to all variables and styles
- Bridge design and development workflows

### ⚡ Production Ready
- Built with Java 21 and Kotlin 2.0
- Docker support for easy deployment
- Real-time WebSocket communication
- Comprehensive error handling and logging

## Technology Stack

- **Java 21** - LTS runtime with modern features
- **Kotlin 2.0** - Modern, type-safe JVM language
- **Ktor 3.3** - Async web framework
- **Kotlinx Serialization** - Type-safe JSON
- **Koin** - Dependency injection
- **Coroutines** - Async/await for Kotlin
- **MCP Kotlin SDK** - MCP protocol support
- **WebSockets** - Real-time Figma communication
- **Docker** - Containerized deployment

## Development

### Build
```bash
cd server
./gradlew clean build shadowJar -x test
```

### Run Tests
```bash
./gradlew test
```

### Run with Docker Compose
```bash
# Start server in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop server
docker-compose down
```

### Manual stdio Testing
```bash
cd server
echo '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"},"capabilities":{}}}' | java -jar build/libs/server-all.jar
```

### Debug Logging

**For Gradle:**
```bash
LOG_LEVEL=debug ./gradlew run
```

**For Docker Compose:**
Edit `docker-compose.yml` to set environment variable:
```yaml
environment:
  - LOG_LEVEL=debug
```

## Troubleshooting

### Claude doesn't see the server
- Verify the server is running with `./gradlew run`
- Check that the URL in `claude mcp add` matches the plugin's display
- Ensure the fileId parameter matches your Figma file
- Restart Claude Code after adding the MCP server

### Tools don't work
- **Make sure Figma plugin is running!**
- Check WebSocket connection in logs
- Verify port 8080 is available (or check Docker logs if using Docker Compose)
- Ensure you're using the correct fileId from the plugin UI

### "No Figma plugin connected"
- Start the Figma plugin in Figma Desktop App
- Check plugin console for connection status
- Verify the plugin is running in the correct Figma file

### Docker issues
- Ensure Docker daemon is running
- Check logs: `docker-compose logs -f`
- Verify port 8080 is not in use: `lsof -i :8080`
- Rebuild container: `docker-compose up -d --build`

See **[SETUP_GUIDE.md](server/SETUP_GUIDE.md)** for detailed troubleshooting.

## Roadmap

### ✅ Current Features
- 30+ Figma tools covering all major operations
- Design system creation (variables, styles, tokens)
- Component generation and manipulation
- File-specific routing for multi-file support
- SSE and WebSocket dual transport
- Docker deployment support
- Comprehensive documentation

### 🚀 Upcoming Features
- **Component Variants**: Create and manage component variants
- **Auto Layout**: Advanced auto-layout configuration
- **Design Import**: Import and parse existing designs
- **Template Library**: Pre-built design system templates
- **Batch Operations**: Process multiple files simultaneously
- **Version Control**: Track design system changes
- **API Extensions**: Additional Figma API coverage

## Contributing

Contributions are welcome! Here's how to get started:

### Adding New Tools

1. **Implement the tool** in `server/src/main/kotlin/com/figma/mcp/services/FigmaToolExecutor.kt`
2. **Register the tool** in `server/src/main/kotlin/com/figma/mcp/commands/mcp/ListToolsCommand.kt`
3. **Route the tool** in `server/src/main/kotlin/com/figma/mcp/commands/mcp/CallToolCommand.kt`
4. **Update documentation** in relevant files

### Guidelines

- Follow existing code patterns and SOLID principles
- Add comprehensive error handling
- Include TypeScript types for plugin communication
- Update tests and documentation
- Submit PRs with clear descriptions

### Getting Help

- Open an issue for bugs or feature requests
- Check existing documentation in the `/server` directory
- Review the MCP specification for protocol details

## Resources

- **MCP Specification**: https://spec.modelcontextprotocol.io/
- **Claude Code Docs**: https://docs.claude.com/claude-code/mcp
- **Ktor Documentation**: https://ktor.io/
- **Figma Plugin API**: https://www.figma.com/plugin-docs/

## License

MIT License

Copyright (c) 2024 Figma MCP Bridge

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Credits

Built with:
- Model Context Protocol (Anthropic)
- Ktor (JetBrains)
- Figma Plugin API (Figma)

---

## Stats

![GitHub stars](https://img.shields.io/github/stars/yourusername/figma-mcp?style=social)
![GitHub forks](https://img.shields.io/github/forks/yourusername/figma-mcp?style=social)

- **Language**: Kotlin 2.0 / TypeScript
- **Tools**: 30+ Figma operations
- **Protocol**: MCP 2024-11-05
- **Status**: ✅ Production Ready
- **License**: MIT

---

<div align="center">

**Ready to revolutionize your design workflow?**

[Get Started](#quick-start) • [View Examples](#example-prompts) • [Read Docs](#documentation) • [Contribute](#contributing)

Made with ❤️ by the Figma MCP community

</div>