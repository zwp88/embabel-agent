package com.embabel.agent.toolgroups.file

import org.springframework.ai.tool.annotation.Tool
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Use at your own risk: This makes changes!!
 * @param root local files
 */
class FileTools(
    val root: String,
) {

    /**
     * Resolves a relative path against the root directory
     * Prevents path traversal attacks by ensuring the resolved path is within the root
     */
    private fun resolvePath(path: String): Path {
        val basePath = Paths.get(root).toAbsolutePath().normalize()
        val resolvedPath = basePath.resolve(path).normalize().toAbsolutePath()

        if (!resolvedPath.startsWith(basePath)) {
            throw SecurityException("Path traversal attempt detected: $path")
        }
        return resolvedPath
    }

    @Tool(description = "Read a file at the relative path")
    fun readFile(path: String): String {
        val resolvedPath = resolvePath(path)
        if (!Files.exists(resolvedPath)) {
            throw IllegalArgumentException("File does not exist: $path")
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw IllegalArgumentException("Path is not a regular file: $path")
        }
        return Files.readString(resolvedPath)
    }

    @Tool(description = "List files and directories at a given path. Prefix is f: for file or d: for directory")
    fun listFiles(path: String): List<String> {
        val resolvedPath = resolvePath(path)
        if (!Files.exists(resolvedPath)) {
            throw IllegalArgumentException("Directory does not exist: $path")
        }
        if (!Files.isDirectory(resolvedPath)) {
            throw IllegalArgumentException("Path is not a directory: $path")
        }

        return Files.list(resolvedPath).use { stream ->
            stream.map {
                val prefix = if (Files.isDirectory(it)) "d:" else "f:"
                prefix + it.fileName.toString()
            }.sorted().toList()
        }
    }

    @Tool(description = "Create a file with the given content")
    fun createFile(path: String, content: String): String {
        val resolvedPath = resolvePath(path)
        if (Files.exists(resolvedPath)) {
            throw IllegalArgumentException("File already exists: $path")
        }

        // Ensure parent directories exist
        Files.createDirectories(resolvedPath.parent)

        // Write content to file
        Files.writeString(resolvedPath, content)
        return "file created"
    }

    @Tool(description = "Edit the file at the given location")
    fun editFile(path: String, oldContent: String, newContent: String): String {
        val resolvedPath = resolvePath(path)
        if (!Files.exists(resolvedPath)) {
            throw IllegalArgumentException("File does not exist: $path")
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw IllegalArgumentException("Path is not a regular file: $path")
        }

        val currentContent = Files.readString(resolvedPath)
        if (currentContent != oldContent) {
            throw IllegalStateException("Current file content does not match the expected old content")
        }

        Files.writeString(resolvedPath, newContent)
        return "file edited"
    }
}