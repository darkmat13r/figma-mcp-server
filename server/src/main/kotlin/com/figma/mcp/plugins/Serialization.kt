package com.figma.mcp.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * Configures JSON serialization for REST endpoints
 * Note: WebSocket messages use kotlinx.serialization directly
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
