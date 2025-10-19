package com.figma.mcp.core

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlin.test.*

class CommandRegistryTest {

    private lateinit var logger: ILogger
    private lateinit var registry: CommandRegistry

    @BeforeTest
    fun setup() {
        logger = mockk(relaxed = true)
        registry = CommandRegistry(logger)
    }

    @Test
    fun `should register command handler`() {
        val handler = TestCommand("test")

        registry.register(handler)

        assertTrue(registry.has("test"))
        assertEquals(handler, registry.get("test"))
        verify { logger.info("Registered command handler: test") }
    }

    @Test
    fun `should warn when replacing existing handler`() {
        val handler1 = TestCommand("test")
        val handler2 = TestCommand("test")

        registry.register(handler1)
        registry.register(handler2)

        verify { logger.warn("Command handler for 'test' is being replaced") }
        assertEquals(handler2, registry.get("test"))
    }

    @Test
    fun `should register multiple handlers`() {
        val handler1 = TestCommand("cmd1")
        val handler2 = TestCommand("cmd2")
        val handler3 = TestCommand("cmd3")

        registry.registerAll(handler1, handler2, handler3)

        assertEquals(3, registry.count)
        assertTrue(registry.has("cmd1"))
        assertTrue(registry.has("cmd2"))
        assertTrue(registry.has("cmd3"))
    }

    @Test
    fun `should return null for non-existent command`() {
        assertNull(registry.get("nonexistent"))
    }

    @Test
    fun `should return false for non-existent command check`() {
        assertFalse(registry.has("nonexistent"))
    }

    @Test
    fun `should return all command names`() {
        registry.registerAll(
            TestCommand("cmd1"),
            TestCommand("cmd2"),
            TestCommand("cmd3")
        )

        val names = registry.getCommandNames()
        assertEquals(3, names.size)
        assertTrue(names.contains("cmd1"))
        assertTrue(names.contains("cmd2"))
        assertTrue(names.contains("cmd3"))
    }

    @Test
    fun `should unregister command`() {
        val handler = TestCommand("test")
        registry.register(handler)

        val result = registry.unregister("test")

        assertTrue(result)
        assertFalse(registry.has("test"))
        verify { logger.info("Unregistered command handler: test") }
    }

    @Test
    fun `should return false when unregistering non-existent command`() {
        val result = registry.unregister("nonexistent")
        assertFalse(result)
    }

    @Test
    fun `should clear all handlers`() {
        registry.registerAll(
            TestCommand("cmd1"),
            TestCommand("cmd2")
        )

        registry.clear()

        assertEquals(0, registry.count)
        verify { logger.info("Cleared all command handlers") }
    }

    @Test
    fun `should return correct count`() {
        assertEquals(0, registry.count)

        registry.register(TestCommand("cmd1"))
        assertEquals(1, registry.count)

        registry.register(TestCommand("cmd2"))
        assertEquals(2, registry.count)

        registry.unregister("cmd1")
        assertEquals(1, registry.count)
    }

    // Test command handler
    private class TestCommand(override val commandName: String) : ICommandHandler {
        override suspend fun execute(context: CommandContext): CommandResult {
            return CommandResult(success = true, data = JsonNull)
        }
    }
}
