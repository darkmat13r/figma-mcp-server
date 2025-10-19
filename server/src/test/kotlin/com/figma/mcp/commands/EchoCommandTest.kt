package com.figma.mcp.commands

import com.figma.mcp.core.CommandContext
import com.figma.mcp.core.ILogger
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.*

class EchoCommandTest {

    private lateinit var logger: ILogger
    private lateinit var command: EchoCommand

    @BeforeTest
    fun setup() {
        logger = mockk(relaxed = true)
        command = EchoCommand(logger)
    }

    @Test
    fun `should have correct command name`() {
        assertEquals("echo", command.commandName)
    }

    @Test
    fun `should return error if message is missing`() {
        val result = command.validate(null)
        assertEquals("Missing required parameter: message", result)
    }

    @Test
    fun `should return error if message field is missing`() {
        val params = buildJsonObject {
            put("other", "value")
        }

        val result = command.validate(params)
        assertEquals("Missing required parameter: message", result)
    }

    @Test
    fun `should return error if message is empty`() {
        val params = buildJsonObject {
            put("message", "")
        }

        val result = command.validate(params)
        assertEquals("Parameter 'message' cannot be empty", result)
    }

    @Test
    fun `should return null for valid message`() {
        val params = buildJsonObject {
            put("message", "Hello")
        }

        val result = command.validate(params)
        assertNull(result)
    }

    @Test
    fun `should echo back the message`() = runTest {
        val params = buildJsonObject {
            put("message", "Hello World")
        }

        val context = CommandContext(
            requestId = "test-request",
            clientId = "test-client",
            timestamp = System.currentTimeMillis(),
            params = params
        )

        val result = command.execute(context)

        assertTrue(result.success)
        assertNotNull(result.data)

        val data = result.data!!.jsonObject
        assertEquals("Hello World", data["echo"]?.jsonPrimitive?.content)
        assertEquals("test-client", data["clientId"]?.jsonPrimitive?.content)
        assertTrue(data.containsKey("receivedAt"))
    }
}
