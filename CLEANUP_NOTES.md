# Cleanup Notes - Removed MCPStdioServer.kt

## What Was Removed

**File**: `server/src/main/kotlin/com/figma/mcp/MCPStdioServer.kt`

**Status**: ✅ **Deleted** (was redundant and unused)

---

## Why It Was Removed

The project had **two different entry points** for starting the MCP server:

### ❌ Removed: MCPStdioServer.kt (Standalone Approach)

```kotlin
object MCPStdioServer : KoinComponent {
    @JvmStatic
    fun main(args: Array<String>) {
        startKoin { modules(appModule) }
        runBlocking {
            stdioTransport.start()
            while (true) { Thread.sleep(1000) }
        }
    }
}
```

**Problems**:
- Not configured in `build.gradle.kts` (mainClass points to Ktor EngineMain)
- **Stdio-only** mode - no HTTP server, no WebSocket support
- Would require separate process for Figma plugin connection
- Never actually used or called
- Redundant with Application.kt implementation

### ✅ Kept: Application.kt (Integrated Approach)

```kotlin
fun Application.module() {
    // ... configure plugins ...

    val stdioTransport: StdioTransport by inject()

    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            stdioTransport.start()  // ← stdio starts automatically
        }
    }
}
```

**Advantages**:
- ✅ **Dual-mode**: HTTP server (port 8080) + stdio transport
- ✅ Single JAR file
- ✅ Single process
- ✅ Both transports share dependency injection
- ✅ Both transports share command registry
- ✅ Simpler deployment
- ✅ Actually configured and used

---

## What This Means

### Before Cleanup
```
Two entry points (confusing):
1. MCPStdioServer.kt ← NOT USED, stdio-only
2. Application.kt ← ACTUALLY USED, dual-mode
```

### After Cleanup
```
One entry point (clear):
1. Application.kt ← Single source of truth, dual-mode
```

---

## How It Works Now

When you run the JAR:

```bash
java -jar server-all.jar
```

**What happens** (in Application.kt:29-87):

1. **Ktor EngineMain starts** (line 30)
2. **HTTP server starts** on port 8080
3. **Application.module() runs** (line 48)
   - Configures DI, WebSocket, JSON, etc.
   - **Starts stdio transport** (line 76-81)
4. **Both transports run concurrently**:
   - Stdio ← for Claude Code (MCP protocol)
   - WebSocket ← for Figma Plugin

```
           ┌─────────────────┐
           │ Application.kt  │
           │    (main)       │
           └────────┬────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
    ┌────▼─────┐         ┌─────▼─────┐
    │  HTTP    │         │  Stdio    │
    │  :8080   │         │ Transport │
    └────┬─────┘         └─────┬─────┘
         │                     │
         │ WebSocket           │ JSON-RPC
         │                     │
    ┌────▼─────┐         ┌─────▼─────┐
    │  Figma   │         │  Claude   │
    │  Plugin  │         │   Code    │
    └──────────┘         └───────────┘
```

---

## Build Configuration

**build.gradle.kts** (line 10-12):
```kotlin
application {
    mainClass = "io.ktor.server.netty.EngineMain"
}
```

This points to **Ktor's EngineMain**, which reads `application.yaml` and calls `Application.module()`.

**NOT**: `MCPStdioServer.main()` ← Never used

---

## Impact on Documentation

Updated these files to remove references to MCPStdioServer.kt:

1. ✅ `MCP_IMPLEMENTATION.md` - File structure updated
2. ✅ `IMPLEMENTATION_SUMMARY.md` - Clarified single entry point
3. ✅ All other docs already referenced Application.kt correctly

---

## No Functional Changes

This was **pure cleanup**. No functionality lost because:

- MCPStdioServer.kt was **never called** (not in build config)
- Application.kt already did **everything** MCPStdioServer tried to do
- Application.kt does it **better** (dual-mode vs stdio-only)

---

## Verification

To verify the cleanup didn't break anything:

```bash
# Check the JAR still builds
cd server
./gradlew clean build shadowJar -x test

# Verify it runs
java -jar build/libs/server-all.jar
# Should start HTTP server on 8080
# Should start stdio transport
# Should work with Claude Code
```

---

## Summary

**Before**: Confusing dual entry points, one unused
**After**: Clean single entry point (Application.kt)
**Impact**: Cleaner codebase, no functional changes
**Status**: ✅ Complete

---

**Date**: October 19, 2025
**Action**: Removed redundant MCPStdioServer.kt
**Reason**: Never used, Application.kt does everything better