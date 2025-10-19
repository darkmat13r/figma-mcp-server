package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.ILogger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PingCommandTest {

    private lateinit var logger: ILogger
    private lateinit var command: PingCommand

    @BeforeTest
    fun setup() {
        logger = mockk(relaxed = true)
        command = PingCommand(logger)
    }

    @Test
    fun `should have correct command name`() {
        assertEquals("ping", command.commandName)
    }

    @Test
    fun `should execute successfully`() = runTest {
        val context = CommandContext(
            requestId = "test-request",
            clientId = "test-client",
            timestamp = System.currentTimeMillis()
        )

        val result = command.execute(context)

        assertTrue(result.success)
        assertNotNull(result.data)
        assertNull(result.error)
    }

    @Test
    fun `should log debug message on execution`() = runTest {
        val context = CommandContext(
            requestId = "test-request",
            clientId = "test-client",
            timestamp = System.currentTimeMillis()
        )

        command.execute(context)

        verify { logger.debug("Executing ping command", "clientId" to "test-client") }
    }

    @Test
    fun `should return message pong`() = runTest {
        val context = CommandContext(
            requestId = "test-request",
            clientId = "test-client",
            timestamp = System.currentTimeMillis()
        )

        val result = command.execute(context)

        assertTrue(result.success)
        assertNotNull(result.data)
        // Response contains "pong", timestamp, and serverTime
        val jsonObject = result.data!!.kotlinx.serialization.json.jsonObject
        assertEquals("pong", jsonObject["message"]?.kotlinx.serialization.json.jsonPrimitive?.content)
        assertTrue(jsonObject.containsKey("timestamp"))
        assertTrue(jsonObject.containsKey("serverTime"))
    }
}
