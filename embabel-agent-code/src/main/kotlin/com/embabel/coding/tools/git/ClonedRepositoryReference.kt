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
package com.embabel.coding.tools.git

import com.embabel.agent.tools.common.Reference
import com.embabel.agent.tools.file.DefaultFileReadLog
import com.embabel.agent.tools.file.FileReadLog
import com.embabel.agent.tools.file.FileReadTools
import com.embabel.agent.tools.file.WellKnownFileContentTransformers
import com.embabel.common.util.StringTransformer
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Reference to a cloned Git repository with automatic cleanup capabilities.
 * Exposes LLM tools.
 * @param localPath The local path where the repository is cloned.
 * @param shouldDeleteOnClose If true, the repository will be deleted when closed.

 */
data class ClonedRepositoryReference(
    val localPath: Path,
    val shouldDeleteOnClose: Boolean = true,
    val fileFormatLimits: FileFormatLimits = FileFormatLimits(),
) : AutoCloseable, FileReadTools, FileReadLog by DefaultFileReadLog(), Reference {

    override val fileContentTransformers: List<StringTransformer>
        get() = listOf(WellKnownFileContentTransformers.removeApacheLicenseHeader)

    override val root = localPath.toAbsolutePath().toString()

    override fun close() {
        if (shouldDeleteOnClose && Files.exists(localPath)) {
            try {
                localPath.toFile().deleteRecursively()
            } catch (e: Exception) {
                // Log warning but don't fail
                System.err.println("Warning: Could not delete temporary git repository at $localPath: ${e.message}")
            }
        }
    }

    /**
     * Write all files in the repository to a single string.
     * Only use this for small repositories as it loads everything into memory.
     * Applies file content transformers (like removing Apache license headers).
     * Uses FileVisitor for cross-platform compatibility.
     */
    fun writeAllFilesToString(): String {
        val result = StringBuilder()
        val files = mutableListOf<Path>()

        try {
            // Use FileVisitor for cross-platform .git exclusion
            Files.walkFileTree(localPath, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val dirName = dir.fileName?.toString()
                    return if (dirName == ".git") {
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (attrs.isRegularFile) {
                        files.add(file)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ): FileVisitResult {
                    // Log but continue processing other files
                    return FileVisitResult.CONTINUE
                }
            })

            // Sort files and process them
            files.sorted().forEach { path ->
                val relativePath = localPath.relativize(path)
                val content = try {
                    if (Files.size(path) > fileFormatLimits.fileSizeLimit) {
                        "// File too large to read: $relativePath"
                    } else {
                        val rawContent = Files.readString(path)
                        WellKnownFileContentTransformers.removeApacheLicenseHeader.transform(rawContent)
                    }
                } catch (e: Exception) {
                    "// Error reading file: ${e.message}"
                }

                result.append("=== $relativePath ===\n")
                result.append(content)
                result.append("\n\n")
            }
        } catch (e: Exception) {
            result.append("Error walking repository: ${e.message}")
        }

        return result.toString()
    }

    /**
     * Return a prompt contributor if the repo is small enough
     */
    override fun contribution(): String {
        if (fileCount() > 1000 || writeAllFilesToString().length > fileFormatLimits.fileSizeLimit) {
            return """
                Use exposed tools. The repository is too large to include in the prompt.
            """.trimIndent()
        }
        return """
                |Cloned repository at: $localPath
                |Use file tools to read files in the repository.
                |Read limit is set to ${fileFormatLimits.fileSizeLimit} bytes.
                """.trimMargin()
    }

}
