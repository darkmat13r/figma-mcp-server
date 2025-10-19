package com.figma.mcp.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.path
import org.slf4j.event.Level

/**
 * Configures request/response logging for monitoring and debugging
 */
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO

        // Optional: Filter which calls to log
        filter { call ->
            call.request.path().startsWith("/")
        }
    }
}
