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
package com.embabel.coding.tools

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reference to a cloned Git repository with automatic cleanup capabilities.
 */
data class ClonedRepository(
    val localPath: Path,
    internal val shouldDeleteOnClose: Boolean = true,
) : AutoCloseable {

    val absolutePath: String = localPath.toAbsolutePath().toString()

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
}

class GitReference {

    /**
     * Clone a Git repository from the given URL to a temporary directory.
     *
     * @param url The Git repository URL (supports both HTTP/HTTPS and SSH)
     * @param branch Optional specific branch to check out (defaults to repository default)
     * @param depth Optional shallow clone depth (null for full clone)
     * @return ClonedRepository with absolute path and cleanup capabilities
     * @throws GitAPIException if the clone operation fails
     */
    fun cloneRepository(
        url: String,
        branch: String? = null,
        depth: Int? = null,
    ): ClonedRepository {
        val tempDir = createTempDirectory()

        try {
            val cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(tempDir.toFile())
                .setCloneAllBranches(branch == null)

            branch?.let { cloneCommand.setBranch(it) }
            depth?.let { cloneCommand.setDepth(it) }

            cloneCommand.call().use { git ->
                // Verify clone was successful
                if (!Files.exists(tempDir.resolve(".git"))) {
                    throw IllegalStateException("Clone operation completed but .git directory not found")
                }
            }

            return ClonedRepository(tempDir)
        } catch (e: Exception) {
            // Clean up on failure
            try {
                tempDir.toFile().deleteRecursively()
            } catch (cleanupEx: Exception) {
                e.addSuppressed(cleanupEx)
            }
            throw when (e) {
                is GitAPIException -> e
                else -> e
            }
        }
    }

    /**
     * Clone a repository to a specific directory (not temporary).
     * The returned ClonedRepository will not auto-delete on close.
     */
    fun cloneRepositoryTo(
        url: String,
        targetDirectory: Path,
        branch: String? = null,
        depth: Int? = null,
    ): ClonedRepository {
        if (Files.exists(targetDirectory) && Files.list(targetDirectory).use { it.findFirst().isPresent }) {
            throw IllegalArgumentException("Target directory $targetDirectory already exists and is not empty")
        }

        Files.createDirectories(targetDirectory.parent)

        val cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setDirectory(targetDirectory.toFile())
            .setCloneAllBranches(branch == null)

        branch?.let { cloneCommand.setBranch(it) }
        depth?.let { cloneCommand.setDepth(it) }

        cloneCommand.call().use { git ->
            // Verify clone was successful
            if (!Files.exists(targetDirectory.resolve(".git"))) {
                throw IllegalStateException("Clone operation completed but .git directory not found")
            }
        }

        return ClonedRepository(targetDirectory, shouldDeleteOnClose = false)
    }

    private fun createTempDirectory(): Path {
        return Files.createTempDirectory("embabel-git-")
    }
}
