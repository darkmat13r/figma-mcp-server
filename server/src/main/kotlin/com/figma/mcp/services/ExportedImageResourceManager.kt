package com.figma.mcp.services

import com.figma.mcp.protocol.Resource
import com.figma.mcp.protocol.ResourceContents
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages exported images as temporary file resources
 *
 * This allows the MCP server to save exported images to temporary files
 * and expose them via the resources protocol, avoiding token consumption
 * when Claude needs to read the image data.
 *
 * ## Benefits:
 * - Zero token consumption for image data transfer
 * - Claude can read files using standard MCP resources/read
 * - Automatic cleanup of old exports
 * - Support for multiple concurrent exports
 */
class ExportedImageResourceManager {

    private val tempDir: Path
    private val exports = ConcurrentHashMap<String, ExportedImage>()

    data class ExportedImage(
        val uri: String,
        val filePath: Path,
        val nodeId: String,
        val format: String,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        // Create temp directory for exports
        tempDir = Files.createTempDirectory("figma-mcp-exports")
        tempDir.toFile().deleteOnExit()

        // Clean up old exports on startup
        cleanupOldExports()
    }

    /**
     * Save exported image data to a temporary file and return resource info
     */
    fun saveExport(
        nodeId: String,
        imageData: ByteArray,
        format: String,
        mimeType: String,
        width: Int,
        height: Int
    ): ExportedImage {
        // Generate unique filename
        val timestamp = System.currentTimeMillis()
        val extension = when (format.uppercase()) {
            "PNG" -> "png"
            "JPG", "JPEG" -> "jpg"
            "SVG" -> "svg"
            "PDF" -> "pdf"
            else -> "png"
        }
        val filename = "export_${nodeId}_${timestamp}.${extension}"

        // Save to temp file
        val filePath = tempDir.resolve(filename)
        Files.write(filePath, imageData)

        // Create resource URI
        val uri = "figma://exports/${filename}"

        // Store in registry
        val export = ExportedImage(
            uri = uri,
            filePath = filePath,
            nodeId = nodeId,
            format = format,
            mimeType = mimeType,
            width = width,
            height = height,
            timestamp = timestamp
        )
        exports[uri] = export

        return export
    }

    /**
     * Get all available exported image resources
     */
    fun listResources(): List<Resource> {
        return exports.values.map { export ->
            Resource(
                uri = export.uri,
                name = "Export of node ${export.nodeId}",
                description = "Exported ${export.format} image (${export.width}x${export.height})",
                mimeType = export.mimeType
            )
        }
    }

    /**
     * Read a resource by URI
     */
    fun readResource(uri: String): ResourceContents? {
        val export = exports[uri] ?: return null

        // Check if file still exists
        if (!Files.exists(export.filePath)) {
            exports.remove(uri)
            return null
        }

        // Read file and encode to base64
        val fileBytes = Files.readAllBytes(export.filePath)
        val base64Data = Base64.getEncoder().encodeToString(fileBytes)

        return ResourceContents(
            uri = uri,
            mimeType = export.mimeType,
            blob = base64Data
        )
    }

    /**
     * Clean up exports older than 1 hour
     */
    private fun cleanupOldExports() {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)

        exports.entries.removeIf { (uri, export) ->
            if (export.timestamp < oneHourAgo) {
                // Delete file
                try {
                    Files.deleteIfExists(export.filePath)
                } catch (e: Exception) {
                    // Ignore errors
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Clean up all exports and temp directory
     */
    fun cleanup() {
        exports.values.forEach { export ->
            try {
                Files.deleteIfExists(export.filePath)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
        exports.clear()

        try {
            tempDir.toFile().deleteRecursively()
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}
