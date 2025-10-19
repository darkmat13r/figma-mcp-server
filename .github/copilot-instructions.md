# GitHub Copilot Instructions for FigmaMcp

## Documentation Policy

**Do NOT add step-by-step documentation to code files.**

### What to Do
- ✅ Update API catalog in `docs/api/` when public interfaces change
- ✅ Add KDoc/JSDoc for public APIs only
- ✅ Write self-documenting code with clear names

### What NOT to Do
- ❌ Add "Step 1", "Step 2" comments
- ❌ Write implementation narratives in comments
- ❌ Document internal implementation details
- ❌ Add change logs in code files

## Code Style

- Follow SOLID principles
- Use dependency injection (Koin for server, React context for plugin)
- Keep functions small and focused
- Prefer composition over inheritance

## Quick Reference

### Build Commands
```bash
# Server (skip tests for speed)
cd server && ./gradlew build -x test

# Plugin
cd figma-plugin && npm run build
```

### Documentation Location
- Server API: `docs/api/server/`
- Plugin API: `docs/api/plugin/`
- MCP Protocol: `docs/api/mcp/`

## Architecture

```
Claude Code ←→ MCP Server (port 1234) ←→ WebSocket (port 8080) ←→ Figma Plugin
```

Only document changes to this architecture in `docs/api/`.
