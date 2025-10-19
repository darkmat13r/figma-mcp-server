package com.figma.mcp.test

import com.figma.mcp.protocol.JSONSchema
import kotlinx.serialization.json.Json

fun main() {
    val schema = JSONSchema.createObjectSchema(
        properties = mapOf(
            "operation" to mapOf(
                "type" to "string",
                "enum" to listOf("UNION", "SUBTRACT", "INTERSECT", "EXCLUDE"),
                "description" to "Boolean operation type"
            ),
            "nodeIds" to mapOf(
                "type" to "array",
                "items" to mapOf("type" to "string"),
                "description" to "Array of node IDs"
            )
        ),
        required = listOf("operation", "nodeIds")
    )

    val json = Json { prettyPrint = true }
    println(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), schema))
}
