package com.figma.mcp.core

/**
 * Logger Interface
 *
 * Dependency Inversion Principle - depend on abstractions, not concretions
 * This allows easy replacement of logging implementation
 */
interface ILogger {
    fun debug(message: String, vararg args: Pair<String, Any?>)
    fun info(message: String, vararg args: Pair<String, Any?>)
    fun warn(message: String, vararg args: Pair<String, Any?>)
    fun error(message: String, throwable: Throwable? = null, vararg args: Pair<String, Any?>)
}
