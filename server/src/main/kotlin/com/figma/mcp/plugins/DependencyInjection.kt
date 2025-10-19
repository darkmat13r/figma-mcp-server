package com.figma.mcp.plugins

import com.figma.mcp.infrastructure.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * Configures Koin dependency injection
 */
fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
