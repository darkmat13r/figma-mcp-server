# Plugin Architecture

This document explains how the Ktor server is organized using the plugin pattern.

## Overview

Instead of configuring everything in `Application.kt`, each concern is separated into its own plugin file in the `plugins/` package. This follows the **Single Responsibility Principle**.

## Plugin Files

### 1. DependencyInjection.kt
**Purpose**: Configures Koin for dependency injection

```kotlin
fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
```

**What it does**:
- Sets up Koin DI container
- Loads the `appModule` which contains all service definitions
- Enables SLF4J logging for Koin

### 2. WebSockets.kt
**Purpose**: Configures WebSocket support

```kotlin
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
```

**What it does**:
- Enables WebSocket protocol
- Configures ping/pong for connection health
- Sets timeout and frame size limits

**Configuration options**:
- `pingPeriod`: How often to send ping frames (30 seconds)
- `timeout`: How long to wait for pong before closing (15 seconds)
- `maxFrameSize`: Maximum size of WebSocket frames
- `masking`: Whether to mask frames (false for server-side)

### 3. Serialization.kt
**Purpose**: Configures JSON serialization for REST endpoints

```kotlin
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
```

**What it does**:
- Enables automatic JSON serialization/deserialization
- Uses kotlinx.serialization
- Applies to REST endpoints only (WebSocket uses manual JSON handling)

### 4. HTTP.kt
**Purpose**: Configures CORS and HTTP settings

```kotlin
fun Application.configureHTTP() {
    install(CORS) {
        anyHost() // WARNING: Development only!
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        // ... more methods
    }
}
```

**What it does**:
- Enables Cross-Origin Resource Sharing (CORS)
- Allows all origins (development only - **change in production!**)
- Configures allowed headers and HTTP methods

**Production considerations**:
```kotlin
// Replace anyHost() with specific hosts:
allowHost("your-domain.com", schemes = listOf("https"))
allowHost("app.your-domain.com", schemes = listOf("https"))
```

### 5. Monitoring.kt
**Purpose**: Configures request/response logging

```kotlin
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            call.request.path().startsWith("/")
        }
    }
}
```

**What it does**:
- Logs all incoming HTTP requests
- Logs responses with status codes
- Can be filtered to only log specific paths

**Log levels**:
- `Level.DEBUG`: Very verbose
- `Level.INFO`: Standard logging (default)
- `Level.WARN`: Only warnings and errors

### 6. Routing.kt
**Purpose**: Configures all application routes

```kotlin
fun Application.configureRouting() {
    val webSocketRoutes: WebSocketRoutes by inject()

    routing {
        // WebSocket endpoint
        with(webSocketRoutes) {
            configureWebSocketRoutes()
        }

        // REST endpoints
        route("/api") {
            get("/health") { /* ... */ }
            get("/version") { /* ... */ }
        }
    }
}
```

**What it does**:
- Defines WebSocket endpoint at `/`
- Defines REST endpoints under `/api`
- Injects `WebSocketRoutes` from Koin

**Endpoints defined**:
- `ws://localhost:8080/` - WebSocket endpoint
- `GET /health` - Simple health check
- `GET /api/health` - Detailed health check
- `GET /api/version` - Version information

## Application.kt

The main application file is now very clean:

```kotlin
fun Application.module() {
    configureDependencyInjection()  // 1. DI first
    configureWebSockets()           // 2. WebSocket support
    configureSerialization()        // 3. JSON handling
    configureHTTP()                 // 4. CORS
    configureMonitoring()           // 5. Logging
    configureRouting()              // 6. Routes last (depends on DI)
}
```

**Order matters**:
1. **DI must be first** - other plugins depend on injected services
2. **Routing must be last** - it uses services from DI
3. Everything else can be in any order

## Benefits of This Architecture

### 1. Single Responsibility
Each file has one specific concern:
- `HTTP.kt` only handles CORS
- `WebSockets.kt` only handles WebSocket config
- etc.

### 2. Easy to Find and Modify
Want to change CORS settings? Go to `plugins/HTTP.kt`
Want to adjust WebSocket timeout? Go to `plugins/WebSockets.kt`

### 3. Easy to Test
Each plugin function can be tested independently:

```kotlin
@Test
fun `test CORS configuration`() {
    testApplication {
        application {
            configureHTTP()
        }

        client.get("/").apply {
            assertTrue(headers.contains("Access-Control-Allow-Origin"))
        }
    }
}
```

### 4. Easy to Add New Plugins
Need authentication? Create `plugins/Security.kt`:

```kotlin
fun Application.configureSecurity() {
    install(Authentication) {
        // ... config
    }
}
```

Then add to `Application.kt`:
```kotlin
fun Application.module() {
    configureDependencyInjection()
    configureSecurity()  // ← Add here
    configureWebSockets()
    // ... rest
}
```

### 5. Clear Dependencies
Looking at `Application.kt` shows exactly what plugins are used and in what order.

## Adding a New Plugin

1. **Create file** in `plugins/YourFeature.kt`:
```kotlin
package com.figma.mcp.plugins

import io.ktor.server.application.*

fun Application.configureYourFeature() {
    install(YourPlugin) {
        // configuration
    }
}
```

2. **Register** in `Application.kt`:
```kotlin
fun Application.module() {
    configureDependencyInjection()
    configureYourFeature()  // ← Add here
    // ... rest
}
```

3. **Document** in this file!

## Ktor Plugin Order Best Practices

### Must Be First
- **DependencyInjection** - Everything else may need injected services

### Should Be Early
- **Security/Authentication** - Protect routes early
- **CORS** - Handle CORS before processing requests

### Can Be Anywhere (Middle)
- **Serialization** - JSON handling
- **WebSockets** - WebSocket support
- **Monitoring** - Logging

### Should Be Last
- **Routing** - Needs all other plugins configured first

## Configuration vs Routes

**plugins/HTTP.kt** = Configuration (CORS settings)
**plugins/Routing.kt** = Route definitions (actual endpoints)

This separation makes it clear:
- How the server behaves (plugins)
- What endpoints exist (routing)

## Summary

This plugin architecture provides:
- ✅ **Clear separation of concerns**
- ✅ **Easy to understand** (one file = one concern)
- ✅ **Easy to modify** (change one thing, edit one file)
- ✅ **Easy to test** (test plugins independently)
- ✅ **SOLID principles** (Single Responsibility!)

The `Application.kt` file becomes a **table of contents** showing what plugins are used, while the actual configuration lives in focused plugin files.
