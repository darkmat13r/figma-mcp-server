package com.figma.mcp.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

/**
 * Configures WebSocket support with ping/pong for connection health
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 30.seconds      // Send ping every 30 seconds
        timeout = 15.seconds          // Close connection if no pong after 15 seconds
        maxFrameSize = Long.MAX_VALUE // Maximum frame size
        masking = false               // Disable masking for server-side
    }
}
