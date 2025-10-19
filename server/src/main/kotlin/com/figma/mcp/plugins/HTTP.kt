package com.figma.mcp.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/**
 * Configures CORS (Cross-Origin Resource Sharing)
 *
 * Security Note: anyHost() allows all origins - suitable for development only.
 * In production, replace with specific allowed hosts using allowHost("example.com")
 */
fun Application.configureHTTP() {
    install(CORS) {
        anyHost() // WARNING: For development only. Restrict in production!

        // Allow common headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        // Allow common HTTP methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
}
