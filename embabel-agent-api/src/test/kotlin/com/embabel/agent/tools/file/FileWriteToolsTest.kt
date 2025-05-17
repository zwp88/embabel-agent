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
package com.embabel.agent.tools.file

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileWriteToolsTest {

    private lateinit var tempDir: File
    private lateinit var fileWriteTools: FileWriteTools
    private lateinit var rootPath: String

    @BeforeEach
    fun setUp() {
        tempDir = FileWriteTools.createTempDir("file-write-tools-test")
        rootPath = tempDir.absolutePath
        fileWriteTools = object : FileWriteTools {
            override val root: String = rootPath
        }
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Nested
    inner class CreateFile {

        @Test
        fun `should create file with content`() {
            val path = "test.txt"
            val content = "test content"

            val result = fileWriteTools.createFile(path, content)

            assertEquals("file created", result)
            val file = File(tempDir, path)
            assertTrue(file.exists())
            assertEquals(content, Files.readString(file.toPath()))
        }

        @Test
        fun `should create file in subdirectory`() {
            val path = "subdir/test.txt"
            val content = "test content"

            val result = fileWriteTools.createFile(path, content)

            assertEquals("file created", result)
            val file = File(tempDir, path)
            assertTrue(file.exists())
            assertEquals(content, Files.readString(file.toPath()))
        }

        @Test
        fun `should not overwrite file in subdirectory unless asked`() {
            val path = "subdir/test.txt"
            val content = "test content"

            fileWriteTools.createFile(path, content)
            assertThrows<Exception> { fileWriteTools.createFile(path, content) }
        }

        @Test
        fun `should overwrite file in subdirectory if asked`() {
            val path = "subdir/test.txt"
            val content = "test content"

            val result1 = fileWriteTools.createFile(path, content)
            val result2 = fileWriteTools.createFile(path, content, overwrite = true)
            val file = File(tempDir, path)
            assertTrue(file.exists())
            assertEquals(content, Files.readString(file.toPath()))
        }

//        @Test
//        fun `should throw exception when file already exists`() {
//            val path = "existing.txt"
//            Files.writeString(tempDir.resolve(path), "existing content")
//
//            assertThrows<IllegalArgumentException> {
//                fileWriteTools.createFile(path, "new content")
//            }
//        }
    }

    @Nested
    inner class EditFile {

        private val originalContent = "This is the original content."
        private val path = "edit-test.txt"
        private lateinit var file: File

        @BeforeEach
        fun setupFile() {
            file = File(tempDir, path)
            Files.writeString(file.toPath(), originalContent)
        }

        @Test
        fun `should replace content in file`() {
            val oldContent = "original content"
            val newContent = "modified content"
            val fullContent = "This is the $oldContent"
            Files.writeString(file.toPath(), fullContent)

            val result = fileWriteTools.editFile(path, oldContent, newContent)

            assertEquals("file edited", result)
            val loadedContent = Files.readString(file.toPath())
            assertEquals(
                "This is the $newContent",
                loadedContent,
                "File content is [$loadedContent]"
            )
        }

        @Test
        fun `should throw exception when file does not exist`() {
            assertThrows<IllegalArgumentException> {
                fileWriteTools.editFile("nonexistent.txt", "old", "new")
            }
        }
    }

    @Nested
    inner class CreateDirectory {

        @Test
        fun `should create directory`() {
            val path = "newdir"

            val result = fileWriteTools.createDirectory(path)

            assertEquals("directory created", result)
            val dir = File(tempDir, path)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

        @Test
        fun `should create nested directories`() {
            val path = "parent/child/grandchild"

            val result = fileWriteTools.createDirectory(path)

            assertEquals("directory created", result)
            val dir = File(tempDir, path)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

        @Test
        fun `should return message when directory already exists`() {
            val path = "existingdir"
            File(tempDir, path).mkdir()

            val result = fileWriteTools.createDirectory(path)

            assertEquals("directory already exists", result)
        }

//        @Test
//        fun `should throw exception when path exists as file`() {
//            val path = "existing.txt"
//            Files.writeString(tempDir.resolve(path), "content")
//
//            assertThrows<IllegalArgumentException> {
//                fileWriteTools.createDirectory(path)
//            }
//        }
    }

    @Nested
    inner class AppendFile {

        private val originalContent = "Original content\n"
        private val path = "append-test.txt"
        private lateinit var file: File

        @BeforeEach
        fun setupFile() {
            file = File(tempDir, path)
            Files.writeString(file.toPath(), originalContent)
        }

        @Test
        fun `should append content to file`() {
            val appendedContent = "Appended content"

            val result = fileWriteTools.appendFile(path, appendedContent)

            assertEquals("content appended to file", result)
            assertEquals(originalContent + appendedContent, Files.readString(file.toPath()))
        }

        @Test
        fun `should throw exception when file does not exist`() {
            assertThrows<IllegalArgumentException> {
                fileWriteTools.appendFile("nonexistent.txt", "content")
            }
        }
    }

    @Nested
    inner class Delete {

        private val path = "to-delete.txt"
        private lateinit var file: File

        @BeforeEach
        fun setupFile() {
            file = File(tempDir, path)
            Files.writeString(file.toPath(), "content to delete")
        }

        @Test
        fun `should delete file`() {
            val result = fileWriteTools.delete(path)

            assertEquals("file deleted", result)
            assertFalse(file.exists())
        }

        @Test
        fun `should throw exception when file does not exist`() {
            assertThrows<IllegalArgumentException> {
                fileWriteTools.delete("nonexistent.txt")
            }
        }
    }

    @Nested
    inner class CreateTempDir {

        private lateinit var createdTempDir: File

        @AfterEach
        fun cleanupTempDir() {
            if (::createdTempDir.isInitialized) {
                createdTempDir.deleteRecursively()
            }
        }

        @Test
        fun `should create temporary directory with seed name`() {
            val seed = "test-seed"

            createdTempDir = FileWriteTools.createTempDir(seed)

            assertTrue(createdTempDir.exists())
            assertTrue(createdTempDir.isDirectory)
            assertTrue(createdTempDir.absolutePath.contains(seed))
        }
    }

    @Nested
    inner class ExtractZipFile {

        private lateinit var zipFile: File
        private lateinit var extractDir: File

        @BeforeEach
        fun createZipFile() {
            // Create a zip file with test content
            zipFile = File(tempDir, "test.zip")
            ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                // Add a file
                val entry1 = ZipEntry("file1.txt")
                zipOut.putNextEntry(entry1)
                zipOut.write("content1".toByteArray())
                zipOut.closeEntry()

                // Add a file in a subdirectory
                val entry2 = ZipEntry("subdir/file2.txt")
                zipOut.putNextEntry(entry2)
                zipOut.write("content2".toByteArray())
                zipOut.closeEntry()
            }

            extractDir = FileWriteTools.createTempDir("extract-test")
        }

        @AfterEach
        fun cleanup() {
            extractDir.deleteRecursively()
        }

        @Test
        @Disabled("not yet working")
        fun `should extract zip file contents`() {
            val result = FileWriteTools.extractZipFile(
                zipFile,
                extractDir,
                delete = false
            )

            assertTrue(
                result.exists(),
                "Zip file content should exist at ${result.absolutePath}",
            )
            assertEquals("test", result.name)

            val file1 = File(extractDir, "file1.txt")
            val file2 = File(extractDir, "subdir/file2.txt")

            assertTrue(file1.exists())
            assertTrue(file2.exists())
            assertEquals("content1", Files.readString(file1.toPath()))
            assertEquals("content2", Files.readString(file2.toPath()))
        }

        @Test
        fun `should delete zip file when delete is true`() {
            FileWriteTools.extractZipFile(zipFile, extractDir, true)
            assertFalse(zipFile.exists())
        }
    }
}
