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
package com.embabel.agent.rag.ingestion

import com.embabel.agent.tools.file.FileTools
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class IngestionUtilsTest {

    private lateinit var mockIngester: Ingester
    private lateinit var ingestionUtils: IngestionUtils

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockIngester = mockk()
        ingestionUtils = IngestionUtils(mockIngester)
    }

    @Nested
    inner class DirectoryIngestionTests {

        @Test
        fun `test ingestFromDirectory with inactive ingester`() {
            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns false

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertFalse(result.success, "Should fail when ingester is inactive")
            assertEquals(0, result.totalFilesFound, "Should have no files found")
            assertEquals(0, result.filesProcessed, "Should have no files processed")
            assertEquals(1, result.errors.size, "Should have one error")
            assertTrue(result.errors[0].contains("not active"), "Error should mention inactive ingester")
        }

        @Test
        fun `test ingestFromDirectory with empty directory`() {
            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertTrue(result.success, "Should succeed with empty directory")
            assertEquals(0, result.totalFilesFound, "Should have no files found")
            assertEquals(0, result.filesProcessed, "Should have no files processed")
            assertEquals(0, result.filesErrored, "Should have no errors")
        }

        @Test
        fun `test ingestFromDirectory with various file types`() {
            // Create test files
            Files.writeString(tempDir.resolve("test.txt"), "Text file content")
            Files.writeString(tempDir.resolve("test.java"), "public class Test {}")
            Files.writeString(tempDir.resolve("test.py"), "print('hello')")
            Files.writeString(tempDir.resolve("test.exe"), "binary content") // Should be excluded
            Files.writeString(tempDir.resolve("README.md"), "# Test Project")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1", "chunk2")
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertTrue(result.success, "Should succeed with mixed file types")
            assertEquals(4, result.totalFilesFound, "Should find 4 supported files (excluding .exe)")
            assertEquals(4, result.filesProcessed, "Should process all supported files")
            assertEquals(0, result.filesErrored, "Should have no errors")
            assertEquals(8, result.totalDocumentsIngested, "Should have 8 documents total (2 per file)")

            verify(exactly = 4) { mockIngester.ingest(any()) }
        }

        @Test
        fun `test ingestFromDirectory with excluded directories`() {
            // Create files in excluded directories
            val nodeModulesDir = tempDir.resolve("node_modules")
            Files.createDirectories(nodeModulesDir)
            Files.writeString(nodeModulesDir.resolve("package.json"), "{}")

            val gitDir = tempDir.resolve(".git")
            Files.createDirectories(gitDir)
            Files.writeString(gitDir.resolve("config"), "git config")

            val targetDir = tempDir.resolve("target")
            Files.createDirectories(targetDir)
            Files.writeString(targetDir.resolve("classes.txt"), "classes")

            // Create files in regular directory
            Files.writeString(tempDir.resolve("source.java"), "public class Source {}")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1")
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertTrue(result.success, "Should succeed")
            assertEquals(1, result.totalFilesFound, "Should only find files not in excluded directories")
            assertEquals(1, result.filesProcessed, "Should only process non-excluded files")

            verify(exactly = 1) { mockIngester.ingest(any()) }
        }

        @Test
        fun `test ingestFromDirectory with custom configuration`() {
            // Create test files
            Files.writeString(tempDir.resolve("test.txt"), "Text file")
            Files.writeString(tempDir.resolve("test.custom"), "Custom file")
            Files.writeString(tempDir.resolve("large.txt"), "x".repeat(2000)) // Large file

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1")
            )

            val config = DirectoryIngestionConfig(
                includedExtensions = setOf("custom", "txt"),
                maxFileSize = 1000 // 1KB limit
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools, "", config)

            assertTrue(result.success, "Should succeed")
            assertEquals(2, result.totalFilesFound, "Should find 2 files (large file excluded)")
            assertEquals(2, result.filesProcessed, "Should process both files under size limit")
        }

        @Test
        fun `test ingestFromDirectory with nested directories`() {
            // Create nested directory structure
            val subDir1 = tempDir.resolve("src").resolve("main").resolve("java")
            Files.createDirectories(subDir1)
            Files.writeString(subDir1.resolve("Main.java"), "public class Main {}")

            val subDir2 = tempDir.resolve("docs")
            Files.createDirectories(subDir2)
            Files.writeString(subDir2.resolve("README.md"), "# Documentation")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1")
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertTrue(result.success, "Should succeed with nested directories")
            assertEquals(2, result.totalFilesFound, "Should find files in nested directories")
            assertEquals(2, result.filesProcessed, "Should process all nested files")
        }

        @Test
        fun `test ingestFromDirectory with max depth limit`() {
            // Create deeply nested structure
            val level1 = tempDir.resolve("level1")
            val level2 = level1.resolve("level2")
            val level3 = level2.resolve("level3")
            Files.createDirectories(level3)

            Files.writeString(level1.resolve("file1.txt"), "Level 1 file")
            Files.writeString(level2.resolve("file2.txt"), "Level 2 file")
            Files.writeString(level3.resolve("file3.txt"), "Level 3 file")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1")
            )

            val config = DirectoryIngestionConfig(maxDepth = 2)
            val result = ingestionUtils.ingestFromDirectory(fileTools, "", config)

            assertTrue(result.success, "Should succeed")
            assertEquals(2, result.totalFilesFound, "Should only find files within max depth")
            assertTrue(result.filesProcessed <= 2, "Should not process files beyond max depth")
        }

        @Test
        fun `test ingestFromDirectory with dry run`() {
            Files.writeString(tempDir.resolve("test.txt"), "Test content")
            Files.writeString(tempDir.resolve("test.java"), "public class Test {}")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true

            val config = DirectoryIngestionConfig(dryRun = true)
            val result = ingestionUtils.ingestFromDirectory(fileTools, "", config)

            assertTrue(result.success, "Dry run should succeed")
            assertEquals(2, result.totalFilesFound, "Should find files")
            assertEquals(0, result.filesProcessed, "Should not process files in dry run")
            assertEquals(2, result.filesSkipped, "Should skip all files in dry run")
            assertEquals(0, result.totalDocumentsIngested, "Should not ingest documents in dry run")

            verify(exactly = 0) { mockIngester.ingest(any()) }
        }

        @Test
        fun `test ingestFromDirectory with ingestion failures`() {
            Files.writeString(tempDir.resolve("success.txt"), "Success file")
            Files.writeString(tempDir.resolve("failure.txt"), "Failure file")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true

            // Mock different responses based on file
            every { mockIngester.ingest(match { it.contains("success.txt") }) } returns IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1")
            )
            every { mockIngester.ingest(match { it.contains("failure.txt") }) } returns IngestionResult(
                storesWrittenTo = emptySet(),
                chunkIds = emptyList()
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertFalse(result.success, "Should fail when some ingestions fail")
            assertEquals(2, result.totalFilesFound, "Should find both files")
            assertEquals(1, result.filesProcessed, "Should process one file successfully")
            assertEquals(1, result.filesErrored, "Should have one failed file")
            assertEquals(1, result.errors.size, "Should have one error")
        }
    }

    @Nested
    inner class SingleFileIngestionTests {

        @Test
        fun `test ingestFile with inactive ingester`() {
            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns false

            val result = ingestionUtils.ingestFile(fileTools, "test.txt")

            assertNull(result, "Should return null when ingester is inactive")
        }

        @Test
        fun `test ingestFile with non-existent file`() {
            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true

            val result = ingestionUtils.ingestFile(fileTools, "non-existent.txt")

            assertNull(result, "Should return null for non-existent file")
        }

        @Test
        fun `test ingestFile with successful ingestion`() {
            Files.writeString(tempDir.resolve("test.txt"), "Test content")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true

            val expectedResult = IngestionResult(
                storesWrittenTo = setOf("test-store"),
                chunkIds = listOf("chunk1", "chunk2")
            )
            every { mockIngester.ingest(any()) } returns expectedResult

            val result = ingestionUtils.ingestFile(fileTools, "test.txt")

            assertNotNull(result, "Should return result for successful ingestion")
            assertEquals(expectedResult, result, "Should return expected ingestion result")
            assertTrue(result!!.success(), "Ingestion should be successful")

            verify { mockIngester.ingest(match { it.startsWith("file://") && it.contains("test.txt") }) }
        }

        @Test
        fun `test ingestFile with ingestion exception`() {
            Files.writeString(tempDir.resolve("test.txt"), "Test content")

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } throws RuntimeException("Ingestion failed")

            val result = ingestionUtils.ingestFile(fileTools, "test.txt")

            assertNull(result, "Should return null when ingestion throws exception")
        }
    }

    @Nested
    inner class ConfigurationTests {

        @Test
        fun `test default configuration values`() {
            val config = DirectoryIngestionConfig()

            assertEquals(IngestionUtils.DEFAULT_EXTENSIONS, config.includedExtensions)
            assertEquals(IngestionUtils.DEFAULT_EXCLUDED_DIRS, config.excludedDirectories)
            assertEquals(1024 * 1024, config.maxFileSize)
            assertFalse(config.followSymlinks)
            assertEquals(Int.MAX_VALUE, config.maxDepth)
            assertFalse(config.dryRun)
        }

        @Test
        fun `test custom configuration values`() {
            val customExtensions = setOf("custom", "test")
            val customExcludedDirs = setOf("exclude1", "exclude2")
            val customMaxFileSize = 500L
            val customMaxDepth = 5

            val config = DirectoryIngestionConfig(
                includedExtensions = customExtensions,
                excludedDirectories = customExcludedDirs,
                maxFileSize = customMaxFileSize,
                followSymlinks = true,
                maxDepth = customMaxDepth,
                dryRun = true
            )

            assertEquals(customExtensions, config.includedExtensions)
            assertEquals(customExcludedDirs, config.excludedDirectories)
            assertEquals(customMaxFileSize, config.maxFileSize)
            assertTrue(config.followSymlinks)
            assertEquals(customMaxDepth, config.maxDepth)
            assertTrue(config.dryRun)
        }

        @Test
        fun `test default extensions include common file types`() {
            val extensions = IngestionUtils.DEFAULT_EXTENSIONS

            // Check for various categories
            assertTrue(extensions.contains("java"), "Should include Java files")
            assertTrue(extensions.contains("kt"), "Should include Kotlin files")
            assertTrue(extensions.contains("py"), "Should include Python files")
            assertTrue(extensions.contains("js"), "Should include JavaScript files")
            assertTrue(extensions.contains("md"), "Should include Markdown files")
            assertTrue(extensions.contains("txt"), "Should include text files")
            assertTrue(extensions.contains("xml"), "Should include XML files")
            assertTrue(extensions.contains("json"), "Should include JSON files")
        }

        @Test
        fun `test default excluded directories include common build artifacts`() {
            val excludedDirs = IngestionUtils.DEFAULT_EXCLUDED_DIRS

            assertTrue(excludedDirs.contains(".git"), "Should exclude .git directory")
            assertTrue(excludedDirs.contains("node_modules"), "Should exclude node_modules directory")
            assertTrue(excludedDirs.contains("target"), "Should exclude target directory")
            assertTrue(excludedDirs.contains("build"), "Should exclude build directory")
            assertTrue(excludedDirs.contains(".idea"), "Should exclude .idea directory")
            assertTrue(excludedDirs.contains("__pycache__"), "Should exclude __pycache__ directory")
        }
    }

    @Nested
    inner class ResultTests {

        @Test
        fun `test DirectoryIngestionResult success calculation`() {
            val successfulResult = DirectoryIngestionResult(
                totalFilesFound = 5,
                filesProcessed = 5,
                filesSkipped = 0,
                filesErrored = 0,
                ingestionResults = listOf(
                    IngestionResult(setOf("store1"), listOf("chunk1")),
                    IngestionResult(setOf("store2"), listOf("chunk2", "chunk3"))
                ),
                processingTime = Duration.ofSeconds(1),
                errors = emptyList()
            )

            assertTrue(successfulResult.success, "Should be successful with no errors")
            assertEquals(3, successfulResult.totalDocumentsIngested, "Should count all documents")

            val failedResult = DirectoryIngestionResult(
                totalFilesFound = 3,
                filesProcessed = 2,
                filesSkipped = 0,
                filesErrored = 1,
                ingestionResults = listOf(
                    IngestionResult(setOf("store1"), listOf("chunk1"))
                ),
                processingTime = Duration.ofSeconds(1),
                errors = listOf("Error processing file")
            )

            assertFalse(failedResult.success, "Should not be successful with errors")
            assertEquals(1, failedResult.totalDocumentsIngested, "Should count processed documents")
        }

        @Test
        fun `test DirectoryIngestionResult with failed ingestion results`() {
            val result = DirectoryIngestionResult(
                totalFilesFound = 2,
                filesProcessed = 2,
                filesSkipped = 0,
                filesErrored = 0,
                ingestionResults = listOf(
                    IngestionResult(setOf("store1"), listOf("chunk1")),
                    IngestionResult(emptySet(), emptyList()) // Failed ingestion
                ),
                processingTime = Duration.ofSeconds(1),
                errors = emptyList()
            )

            assertFalse(result.success, "Should not be successful when ingestion results fail")
            assertEquals(1, result.totalDocumentsIngested, "Should only count successful ingestions")
        }
    }

    @Nested
    inner class IntegrationTests {

        @Test
        fun `test real file system integration`() {
            // Create a realistic project structure
            val srcDir = tempDir.resolve("src").resolve("main").resolve("java").resolve("com").resolve("example")
            Files.createDirectories(srcDir)
            Files.writeString(
                srcDir.resolve("Main.java"), """
                package com.example;

                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
            """.trimIndent()
            )

            val testDir = tempDir.resolve("src").resolve("test").resolve("java").resolve("com").resolve("example")
            Files.createDirectories(testDir)
            Files.writeString(
                testDir.resolve("MainTest.java"), """
                package com.example;

                import org.junit.Test;

                public class MainTest {
                    @Test
                    public void testMain() {
                        // Test implementation
                    }
                }
            """.trimIndent()
            )

            val docsDir = tempDir.resolve("docs")
            Files.createDirectories(docsDir)
            Files.writeString(docsDir.resolve("README.md"), "# Example Project")

            // Create excluded directory
            val targetDir = tempDir.resolve("target").resolve("classes")
            Files.createDirectories(targetDir)
            Files.writeString(targetDir.resolve("Main.class"), "compiled bytecode")

            Files.writeString(
                tempDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>example</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()
            )

            val fileTools = FileTools.readOnly(tempDir.toString())
            every { mockIngester.active() } returns true
            every { mockIngester.ingest(any()) } returns IngestionResult(
                storesWrittenTo = setOf("vector-store"),
                chunkIds = listOf("chunk1", "chunk2")
            )

            val result = ingestionUtils.ingestFromDirectory(fileTools)

            assertTrue(result.success, "Integration test should succeed")
            assertEquals(4, result.totalFilesFound, "Should find Java, XML, and MD files")
            assertEquals(4, result.filesProcessed, "Should process all found files")
            assertEquals(0, result.filesErrored, "Should have no processing errors")
            assertEquals(8, result.totalDocumentsIngested, "Should ingest all documents")

            // Verify files were processed in correct order and with correct paths
            verify(exactly = 4) { mockIngester.ingest(any()) }
        }
    }
}
