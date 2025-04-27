/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.toolgroups.file

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.spi.support.SelfToolGroup
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Use at your own risk: This makes changes!!
 * @param root local files
 */
class FileTools(
    override val description: ToolGroupDescription = ToolGroup.FILE_DESCRIPTION,
    val root: String,
) : SelfToolGroup {

    private val logger = LoggerFactory.getLogger(FileTools::class.java)

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

    @Tool(description = "Find files using glob patterns. Return absolute paths")
    fun findFiles(glob: String): List<String> {
        val basePath = Paths.get(root).toAbsolutePath().normalize()

        // Prepare glob pattern - ensure it uses the correct syntax
        val syntaxAndPattern = if (glob.startsWith("glob:") || glob.startsWith("regex:")) {
            glob
        } else {
            "glob:$glob"
        }

        val matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern)

        val results = mutableListOf<String>()
        Files.walk(basePath).use { paths ->
            paths.forEach { path ->
                // Match against the relative path (from base) to properly work with glob patterns
                val relPath = basePath.relativize(path)
                if (matcher.matches(relPath)) {
                    results.add(path.toAbsolutePath().toString())
                }
            }
        }

        return results
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
            logger.warn("File already exists at {}", path)
            throw IllegalArgumentException("File already exists: $path")
        }

        // Ensure parent directories exist
        Files.createDirectories(resolvedPath.parent)

        // Write content to file
        Files.writeString(resolvedPath, content)
        return "file created"
    }

    @Tool(description = "Edit the file at the given location. Replace oldContent with newContent. oldContent is typically just a part of the file. e.g. use it to replace a particular method to add another method")
    fun editFile(path: String, oldContent: String, newContent: String): String {
        logger.info("Editing file at path: $path: $oldContent -> $newContent")
        val resolvedPath = resolvePath(path)
        if (!Files.exists(resolvedPath)) {
            throw IllegalArgumentException("File does not exist: $path")
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw IllegalArgumentException("Path is not a regular file: $path")
        }

        val currentContent = Files.readString(resolvedPath)
//        if (currentContent != oldContent) {
//            throw IllegalStateException("Current file content does not match the expected old content")
//        }
        val newFileContent = currentContent.replace(oldContent, newContent)

        Files.writeString(resolvedPath, newFileContent)
        logger.info("Edited file at path: $path")
        return "file edited"
    }

    // April 25 2005: This method is the first method added to
    // an Embabel project by an Embabel agent
    @Tool(description = "Create a directory at the given path")
    fun createDirectory(path: String): String {
        val resolvedPath = resolvePath(path)
        if (Files.exists(resolvedPath)) {
            if (Files.isDirectory(resolvedPath)) {
                return "directory already exists"
            }
            throw IllegalArgumentException("A file already exists at this path: $path")
        }

        Files.createDirectories(resolvedPath)
        logger.info("Created directory at path: $path")
        return "directory created"
    }

}
