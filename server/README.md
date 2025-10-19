# Figma MCP WebSocket Server (Ktor + Kotlin)

A production-ready WebSocket server built with **Ktor** and **Kotlin** following **SOLID principles** for Figma plugin communication.

## Architecture - SOLID Principles

This server implements SOLID principles to ensure:
- ✅ **Single Responsibility**: Each class has one specific job
- ✅ **Open/Closed**: Add new commands without modifying existing code
- ✅ **Liskov Substitution**: All command handlers are interchangeable
- ✅ **Interface Segregation**: Small, focused interfaces (`ILogger`, `ICommandHandler`)
- ✅ **Dependency Inversion**: Components depend on abstractions, not concretions

### Project Structure

```
server/src/main/kotlin/com/figma/mcp/
├── core/                      # Core interfaces and types
│   ├── Types.kt              # MCP protocol types
│   ├── ICommandHandler.kt    # Command handler interface
│   ├── ILogger.kt            # Logger interface
│   └── CommandRegistry.kt    # Command registration system
│
├── commands/                  # Command handlers (add new ones here!)
│   ├── PingCommand.kt
│   ├── EchoCommand.kt
│   ├── GetInfoCommand.kt
│   └── CreateNodeCommand.kt
│
├── infrastructure/            # Infrastructure layer
│   ├── Slf4jLogger.kt        # Logger implementation
│   └── DiModule.kt           # Koin DI configuration
│
├── plugins/                   # Ktor plugins (one concern per file)
│   ├── DependencyInjection.kt # Koin setup
│   ├── WebSockets.kt         # WebSocket configuration
│   ├── Serialization.kt      # JSON serialization
│   ├── HTTP.kt               # CORS configuration
│   ├── Monitoring.kt         # Request logging
│   └── Routing.kt            # All routes (WebSocket + REST)
│
├── routes/                    # Routing layer
│   └── WebSocketRoutes.kt    # WebSocket message handling
│
└── Application.kt             # Main entry point (clean!)
```

## Available Commands

- **ping** - Health check
- **echo** - Echo message back with validation
- **getInfo** - Server information and capabilities
- **createNode** - Create Figma nodes with validation

## Adding a New Command

1. Create `src/main/kotlin/com/figma/mcp/commands/YourCommand.kt`
2. Implement `ICommandHandler` interface
3. Register in `infrastructure/DiModule.kt`
4. Done! No other changes needed.

Example:
```kotlin
class YourCommand(private val logger: ILogger) : ICommandHandler {
    override val commandName = "yourCommand"

    override suspend fun execute(context: CommandContext): CommandResult {
        return CommandResult(success = true, data = buildJsonObject {
            put("result", "success")
        })
    }
}
```

## Endpoints

### WebSocket Endpoint
**Connection**: `ws://localhost:8080/`

**Request**:
```json
{"id": "1", "method": "ping"}
```

**Response**:
```json
{"id": "1", "result": {"message": "pong", "timestamp": 1234567890}}
```

### REST Endpoints

**Health Check**:
```bash
GET /health
GET /api/health  # Detailed version
```

**Version Info**:
```bash
GET /api/version
```

## Useful Links

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- [Koin Dependency Injection](https://insert-koin.io/)

## Features

Here's a list of features included in this project:

| Name                                                                   | Description                                                                        |
| ------------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [AsyncAPI](https://start.ktor.io/p/asyncapi)                           | Generates and serves AsyncAPI documentation                                        |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routing DSL                                                  |
| [Server-Sent Events (SSE)](https://start.ktor.io/p/sse)                | Support for server push events                                                     |
| [Call Logging](https://start.ktor.io/p/call-logging)                   | Logs client requests                                                               |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Koin](https://start.ktor.io/p/koin)                                   | Provides dependency injection                                                      |
| [Raw Sockets](https://start.ktor.io/p/ktor-network)                    | Adds raw socket support for TCP and UDP                                            |
| [WebSockets](https://start.ktor.io/p/ktor-websockets)                  | Adds WebSocket protocol support for bidirectional client connections               |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

