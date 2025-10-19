package com.figma.mcp.infrastructure

import com.figma.mcp.core.ILogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * SLF4J Logger Implementation
 *
 * Concrete implementation of ILogger interface using SLF4J
 * Can be easily replaced with any other logging framework
 */
class Slf4jLogger(private val logger: Logger) : ILogger {

    constructor(clazz: Class<*>) : this(LoggerFactory.getLogger(clazz))
    constructor(name: String) : this(LoggerFactory.getLogger(name))

    override fun debug(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isDebugEnabled) {
            logger.debug(formatMessage(message, *args))
        }
    }

    override fun info(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isInfoEnabled) {
            logger.info(formatMessage(message, *args))
        }
    }

    override fun warn(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isWarnEnabled) {
            logger.warn(formatMessage(message, *args))
        }
    }

    override fun error(message: String, throwable: Throwable?, vararg args: Pair<String, Any?>) {
        if (logger.isErrorEnabled) {
            if (throwable != null) {
                logger.error(formatMessage(message, *args), throwable)
            } else {
                logger.error(formatMessage(message, *args))
            }
        }
    }

    private fun formatMessage(message: String, vararg args: Pair<String, Any?>): String {
        return if (args.isEmpty()) {
            message
        } else {
            val argsStr = args.joinToString(", ") { "${it.first}=${it.second}" }
            "$message [$argsStr]"
        }
    }
}
