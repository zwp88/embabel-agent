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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class GitReferenceTest {

    private val gitReference = GitReference()

    @Test
    fun `clone public repository to temporary directory`() {
        val url = "https://github.com/octocat/Hello-World.git"

        gitReference.cloneRepository(url).use { clonedRepo ->
            // Verify the repository was cloned
            assertTrue(Files.exists(clonedRepo.localPath))
            assertTrue(Files.isDirectory(clonedRepo.localPath))
            assertTrue(Files.exists(clonedRepo.localPath.resolve(".git")))

            // Verify absolute path is returned
            assertTrue(clonedRepo.absolutePath.startsWith("/"))
            assertEquals(clonedRepo.localPath.toAbsolutePath().toString(), clonedRepo.absolutePath)

            // Verify some expected files exist (Hello-World repo has README)
            assertTrue(Files.exists(clonedRepo.localPath.resolve("README")))
        }

        // After closing, temp directory should be cleaned up
        // Note: We can't easily test this without keeping a reference to the path
    }

    @Test
    fun `clone repository with specific branch`() {
        val url = "https://github.com/octocat/Hello-World.git"

        gitReference.cloneRepository(url, branch = "master").use { clonedRepo ->
            assertTrue(Files.exists(clonedRepo.localPath))
            assertTrue(Files.exists(clonedRepo.localPath.resolve(".git")))
            assertTrue(Files.exists(clonedRepo.localPath.resolve("README")))
        }
    }

    @Test
    fun `clone repository with shallow depth`() {
        val url = "https://github.com/octocat/Hello-World.git"

        gitReference.cloneRepository(url, depth = 1).use { clonedRepo ->
            assertTrue(Files.exists(clonedRepo.localPath))
            assertTrue(Files.exists(clonedRepo.localPath.resolve(".git")))
            assertTrue(Files.exists(clonedRepo.localPath.resolve("README")))
        }
    }

    @Test
    fun `clone repository to specific directory`(@TempDir tempDir: Path) {
        val url = "https://github.com/octocat/Hello-World.git"
        val targetDir = tempDir.resolve("hello-world")

        gitReference.cloneRepositoryTo(url, targetDir).use { clonedRepo ->
            assertEquals(targetDir, clonedRepo.localPath)
            assertTrue(Files.exists(targetDir))
            assertTrue(Files.exists(targetDir.resolve(".git")))
            assertTrue(Files.exists(targetDir.resolve("README")))

            // This should not auto-delete on close
            assertFalse(clonedRepo.shouldDeleteOnClose)
        }
        println("Cloned repository at: ${targetDir.toAbsolutePath()}")

        // Directory should still exist after close since shouldDeleteOnClose is false
        assertTrue(Files.exists(targetDir))
    }

    @Test
    fun `clone to existing non-empty directory throws exception`(@TempDir tempDir: Path) {
        val url = "https://github.com/octocat/Hello-World.git"
        val targetDir = tempDir.resolve("existing")
        Files.createDirectories(targetDir)
        Files.write(targetDir.resolve("existing-file.txt"), "content".toByteArray())

        assertThrows(IllegalArgumentException::class.java) {
            gitReference.cloneRepositoryTo(url, targetDir)
        }
    }

    @Test
    fun `invalid repository URL throws GitAPIException`() {
        val invalidUrl = "https://github.com/nonexistent/nonexistent.git"

        assertThrows(Exception::class.java) {
            gitReference.cloneRepository(invalidUrl).use { }
        }
    }

    @Test
    fun `cloned repository implements AutoCloseable properly`() {
        val url = "https://github.com/octocat/Hello-World.git"
        var localPath: Path? = null

        gitReference.cloneRepository(url).use { clonedRepo ->
            localPath = clonedRepo.localPath
            assertTrue(Files.exists(localPath!!))
        }

        // Note: We can't reliably test cleanup in unit tests since it happens in close()
        // and temp directories might be cleaned up by the OS
    }
}
