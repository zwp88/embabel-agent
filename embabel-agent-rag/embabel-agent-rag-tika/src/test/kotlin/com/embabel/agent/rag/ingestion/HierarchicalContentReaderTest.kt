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

import com.embabel.agent.rag.LeafSection
import com.embabel.agent.tools.file.FileReadTools
import io.mockk.every
import io.mockk.mockk
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class HierarchicalContentReaderTest {

    private val reader = HierarchicalContentReader()

    @Test
    fun `test parse simple markdown content`() {
        val markdown = """
            # Main Title
            This is the introduction.

            ## Section 1
            Content for section 1.

            ### Subsection 1.1
            Content for subsection 1.1.

            ## Section 2
            Content for section 2.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, "test.md")
            set(TikaCoreProperties.CONTENT_TYPE_HINT, "text/markdown")
        }

        val result = reader.parseContent(inputStream, metadata, "test://example.md")

        assertEquals(4, result.children.size) // Introduction + Section 1 + Subsection 1.1 + Section 2
        assertEquals("test://example.md", result.url)
        assertNotNull(result.id)

        // Check that all sections have proper titles and content
        val titles = result.children.map { it.title }
        assertTrue(titles.contains("Main Title"))
        assertTrue(titles.contains("Section 1"))
        assertTrue(titles.contains("Subsection 1.1"))
        assertTrue(titles.contains("Section 2"))

        // Check that all sections have the same URL
        result.children.forEach { section ->
            assertEquals("test://example.md", section.url)
            assertNotNull(section.id)
        }
    }

    @Test
    fun `test parse markdown with nested structure`() {
        val markdown = """
            # Document Title
            Introduction paragraph.

            ## Chapter 1
            Chapter introduction.

            ### Section 1.1
            Section content here.

            #### Subsection 1.1.1
            Detailed content.

            ### Section 1.2
            More content.

            ## Chapter 2
            Second chapter content.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, "document.md")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(6, result.children.size) // All leaf sections
        assertNotNull(result.id)

        val titles = result.children.map { it.title }
        assertTrue(titles.contains("Document Title"))
        assertTrue(titles.contains("Chapter 1"))
        assertTrue(titles.contains("Section 1.1"))
        assertTrue(titles.contains("Subsection 1.1.1"))
        assertTrue(titles.contains("Section 1.2"))
        assertTrue(titles.contains("Chapter 2"))

        // Verify all sections have content
        result.children.forEach { section ->
            assertTrue((section as LeafSection).content.isNotBlank())
            assertNotNull(section.id)
        }
    }

    @Test
    fun `test parse plain text content`() {
        val text = """
            This is a simple text document.
            It has multiple lines.
            But no special formatting.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(text.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.CONTENT_TYPE_HINT, "text/plain")
        }

        val result = reader.parseContent(inputStream, metadata, "test://plain.txt")

        assertEquals(1, result.children.size)
        assertEquals("test://plain.txt", result.url)
        assertNotNull(result.id)

        val section = result.children.first() as LeafSection
        assertEquals("This is a simple text document.", section.title)
        assertEquals("test://plain.txt", section.url)
        assertEquals(text, section.content)
        assertNotNull(section.id)
    }

    @Test
    fun `test parse file from disk`(@TempDir tempDir: Path) {
        val markdownFile = tempDir.resolve("test.md").toFile()
        val markdown = """
            # Test Document
            This is a test document.

            ## First Section
            Content of the first section.

            ## Second Section
            Content of the second section.
        """.trimIndent()

        markdownFile.writeText(markdown)

        val result = reader.parseFile(markdownFile)

        assertEquals(3, result.children.size)
        assertNotNull(result.id)
        val titles = result.children.map { it.title }
        assertTrue(titles.contains("Test Document"))
        assertTrue(titles.contains("First Section"))
        assertTrue(titles.contains("Second Section"))

        result.children.forEach { section ->
            assertTrue(section.url!!.contains("test.md"))
            assertNotNull(section.id)
        }
    }

    @Test
    fun `test title extraction from metadata`() {
        val content = "Some content without markdown headers"

        val inputStream = ByteArrayInputStream(content.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.TITLE, "Custom Title from Metadata")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(1, result.children.size)
        assertEquals("Custom Title from Metadata", (result.children.first() as LeafSection).title)
    }

    @Test
    fun `test title extraction from first line when no headers`() {
        val content = """
            This should become the title
            And this is the rest of the content.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(content.toByteArray())

        val result = reader.parseContent(inputStream)

        assertEquals(1, result.children.size)
        assertEquals("This should become the title", (result.children.first() as LeafSection).title)
    }

    @Test
    fun `test metadata preservation`() {
        val content = "Simple content"

        val inputStream = ByteArrayInputStream(content.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.TITLE, "Test Title")
            set(TikaCoreProperties.CREATOR, "Test Author")
            set("custom-field", "custom-value")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(1, result.children.size)
        val section = result.children.first() as LeafSection
        val resultMetadata = section.metadata
        assertEquals("Test Author", resultMetadata[TikaCoreProperties.CREATOR.name])
        assertEquals("custom-value", resultMetadata["custom-field"])
    }

    @Test
    fun `test error handling for empty content`() {
        // Create an input stream with empty content
        val inputStream = ByteArrayInputStream(ByteArray(0))

        val result = reader.parseContent(inputStream)

        // Should return empty content root for empty content
        assertTrue(result.children.isEmpty())
        assertNotNull(result.id)
    }

    @Test
    fun `test HTML content parsing`() {
        val html = """
            <html>
            <head><title>HTML Document</title></head>
            <body>
                <h1>Main Heading</h1>
                <p>This is a paragraph with <strong>bold</strong> text.</p>
                <h2>Second Heading</h2>
                <p>More content here.</p>
            </body>
            </html>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(html.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.CONTENT_TYPE_HINT, "text/html")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(1, result.children.size) // HTML content treated as single section
        val section = result.children.first() as LeafSection
        assertNotNull(section.title)
        assertTrue(section.content.isNotBlank())
        // Content should be cleaned HTML
        assertFalse(section.content.contains("<"))
        assertFalse(section.content.contains(">"))
    }

    @Test
    fun `test markdown with code blocks`() {
        val markdown = """
            # Code Examples

            Here's some code:

            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```

            ## Another Section
            More content after code.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, "code.md")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(2, result.children.size)
        val titles = result.children.map { it.title }
        assertTrue(titles.contains("Code Examples"))
        assertTrue(titles.contains("Another Section"))

        // Find the Code Examples section and verify it contains the code block
        val codeSection = result.children.find { it.title == "Code Examples" } as LeafSection?
        assertNotNull(codeSection)
        assertTrue(codeSection!!.content.contains("```kotlin"))
        assertTrue(codeSection.content.contains("fun main()"))
    }

    @Test
    fun `test empty markdown file`() {
        val markdown = ""

        val inputStream = ByteArrayInputStream(markdown.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, "empty.md")
        }

        val result = reader.parseContent(inputStream, metadata)

        // Empty content should return empty content root
        assertTrue(result.children.isEmpty())
        assertNotNull(result.id)
    }

    @Test
    fun `test markdown with only content no headers`() {
        val markdown = """
            This is just content.
            No headers at all.
            Just plain text.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, "plain.md")
        }

        val result = reader.parseContent(inputStream, metadata)

        assertEquals(1, result.children.size)
        val section = result.children.first() as LeafSection
        assertEquals("This is just content.", section.title)
        assertEquals(markdown, section.content)
        assertNotNull(section.id)
    }

    @Test
    fun `test multiple markdown sections with different levels`() {
        val markdown = """
            # Level 1 Title
            Content under level 1.

            ## Level 2 Title
            Content under level 2.

            ### Level 3 Title
            Content under level 3.

            #### Level 4 Title
            Content under level 4.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())

        val result = reader.parseContent(inputStream)

        assertEquals(4, result.children.size)
        val titles = result.children.map { it.title }
        assertEquals(listOf("Level 1 Title", "Level 2 Title", "Level 3 Title", "Level 4 Title"), titles)

        // Verify each section has appropriate content
        result.children.forEach { section ->
            val leafSection = section as LeafSection
            assertTrue(leafSection.content.startsWith("Content under"))
            assertNotNull(leafSection.id)
            assertNotNull(leafSection.parentId) // All sections should have parent references
        }
    }

    @Test
    fun `test section parent relationships are maintained`() {
        val markdown = """
            # Main Section
            Main content.

            ## Sub Section
            Sub content.

            ### Deep Section
            Deep content.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray())

        val result = reader.parseContent(inputStream)

        assertEquals(3, result.children.size)

        // All sections should have parent IDs set
        result.children.forEach { section ->
            assertNotNull(section.parentId)
        }

        // Verify content is correctly assigned
        val mainSection = result.children.find { it.title == "Main Section" } as LeafSection?
        assertEquals("Main content.", mainSection?.content?.trim())

        val subSection = result.children.find { it.title == "Sub Section" } as LeafSection?
        assertEquals("Sub content.", subSection?.content?.trim())

        val deepSection = result.children.find { it.title == "Deep Section" } as LeafSection?
        assertEquals("Deep content.", deepSection?.content?.trim())
    }

    @Test
    fun `test parseFromDirectory with mixed file types`(@TempDir tempDir: Path) {
        // Create test files
        val mdFile = tempDir.resolve("document.md")
        Files.writeString(mdFile, """
            # Test Document
            This is a test document.

            ## Section 1
            Content of section 1.
        """.trimIndent(), StandardOpenOption.CREATE)

        val txtFile = tempDir.resolve("readme.txt")
        Files.writeString(txtFile, "This is a simple text file.", StandardOpenOption.CREATE)

        val subdirPath = tempDir.resolve("subdir")
        Files.createDirectory(subdirPath)
        val subFile = subdirPath.resolve("sub.md")
        Files.writeString(subFile, """
            # Sub Document
            Content in subdirectory.
        """.trimIndent(), StandardOpenOption.CREATE)

        // Mock FileReadTools
        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:document.md", "f:readme.txt", "d:subdir")
        every { fileTools.listFiles("subdir") } returns listOf("f:sub.md")
        every { fileTools.resolvePath("document.md") } returns mdFile
        every { fileTools.resolvePath("readme.txt") } returns txtFile
        every { fileTools.resolvePath("subdir/sub.md") } returns subFile
        every { fileTools.safeReadFile("document.md") } returns Files.readString(mdFile)
        every { fileTools.safeReadFile("readme.txt") } returns Files.readString(txtFile)
        every { fileTools.safeReadFile("subdir/sub.md") } returns Files.readString(subFile)

        val config = DirectoryParsingConfig(
            includedExtensions = setOf("md", "txt"),
            maxFileSize = 1024 * 1024
        )

        val result = reader.parseFromDirectory(fileTools, "", config)

        assertTrue(result.success)
        assertEquals(3, result.totalFilesFound)
        assertEquals(3, result.filesProcessed)
        assertEquals(0, result.filesSkipped)
        assertEquals(0, result.filesErrored)
        assertEquals(3, result.contentRoots.size)
        assertTrue(result.errors.isEmpty())

        // Verify parsed content
        val documentRoot = result.contentRoots.find { it.title == "Test Document" }
        assertNotNull(documentRoot)
        assertEquals(2, documentRoot!!.leaves().size) // 2 sections in document.md

        val readmeRoot = result.contentRoots.find { it.title == "This is a simple text file." }
        assertNotNull(readmeRoot)
        assertEquals(1, readmeRoot!!.leaves().size) // 1 section in readme.txt

        val subRoot = result.contentRoots.find { it.title == "Sub Document" }
        assertNotNull(subRoot)
        assertEquals(1, subRoot!!.leaves().size) // 1 section in sub.md
    }

    @Test
    fun `test parseFromDirectory with file size limits`(@TempDir tempDir: Path) {
        val smallFile = tempDir.resolve("small.md")
        Files.writeString(smallFile, "# Small\nSmall content", StandardOpenOption.CREATE)

        val largeFile = tempDir.resolve("large.md")
        Files.writeString(largeFile, "# Large\n" + "X".repeat(2000), StandardOpenOption.CREATE)

        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:small.md", "f:large.md")
        every { fileTools.resolvePath("small.md") } returns smallFile
        every { fileTools.resolvePath("large.md") } returns largeFile
        every { fileTools.safeReadFile("small.md") } returns Files.readString(smallFile)

        val config = DirectoryParsingConfig(
            includedExtensions = setOf("md"),
            maxFileSize = 100 // Very small limit to exclude large file
        )

        val result = reader.parseFromDirectory(fileTools, "", config)

        assertTrue(result.success)
        assertEquals(1, result.totalFilesFound) // Only small file should be discovered
        assertEquals(1, result.filesProcessed)
        assertEquals(0, result.filesSkipped)
        assertEquals(0, result.filesErrored)
        assertEquals(1, result.contentRoots.size)
    }

    @Test
    fun `test parseFromDirectory with excluded directories`(@TempDir tempDir: Path) {
        val normalFile = tempDir.resolve("normal.md")
        Files.writeString(normalFile, "# Normal\nNormal content", StandardOpenOption.CREATE)

        val gitDir = tempDir.resolve(".git")
        Files.createDirectory(gitDir)
        val gitFile = gitDir.resolve("config")
        Files.writeString(gitFile, "git config content", StandardOpenOption.CREATE)

        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:normal.md", "d:.git")
        every { fileTools.resolvePath("normal.md") } returns normalFile
        every { fileTools.safeReadFile("normal.md") } returns Files.readString(normalFile)

        val config = DirectoryParsingConfig(
            includedExtensions = setOf("md"),
            excludedDirectories = setOf(".git")
        )

        val result = reader.parseFromDirectory(fileTools, "", config)

        assertTrue(result.success)
        assertEquals(1, result.totalFilesFound) // Only normal.md should be found
        assertEquals(1, result.filesProcessed)
        assertEquals(1, result.contentRoots.size)

        val contentRoot = result.contentRoots.first()
        assertEquals("Normal", contentRoot.title)
    }

    @Test
    fun `test parseFromDirectory handles file read errors gracefully`(@TempDir tempDir: Path) {
        // Create a real file first so it passes the file size validation
        val errorFile = tempDir.resolve("error.md")
        Files.writeString(errorFile, "# Error\nSome content", StandardOpenOption.CREATE)

        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:error.md")
        every { fileTools.resolvePath("error.md") } returns errorFile
        every { fileTools.safeReadFile("error.md") } returns null // Simulate read error

        val result = reader.parseFromDirectory(fileTools, "")

        assertTrue(result.success) // Should still be successful overall
        assertEquals(1, result.totalFilesFound)
        assertEquals(0, result.filesProcessed)
        assertEquals(1, result.filesSkipped) // File should be skipped due to read error
        assertEquals(0, result.filesErrored)
        assertEquals(0, result.contentRoots.size)
    }

    @Test
    fun `test parseFromDirectory with custom extensions`(@TempDir tempDir: Path) {
        val kotlinFile = tempDir.resolve("Example.kt")
        Files.writeString(kotlinFile, """
            /**
             * Example Kotlin class
             */
            class Example {
                fun doSomething() = "Hello"
            }
        """.trimIndent(), StandardOpenOption.CREATE)

        val javaFile = tempDir.resolve("Main.java")
        Files.writeString(javaFile, """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
        """.trimIndent(), StandardOpenOption.CREATE)

        val ignoredFile = tempDir.resolve("data.csv")
        Files.writeString(ignoredFile, "name,value\ntest,123", StandardOpenOption.CREATE)

        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:Example.kt", "f:Main.java", "f:data.csv")
        every { fileTools.resolvePath("Example.kt") } returns kotlinFile
        every { fileTools.resolvePath("Main.java") } returns javaFile
        every { fileTools.resolvePath("data.csv") } returns Path.of("data.csv")
        every { fileTools.safeReadFile("Example.kt") } returns Files.readString(kotlinFile)
        every { fileTools.safeReadFile("Main.java") } returns Files.readString(javaFile)

        val config = DirectoryParsingConfig(
            includedExtensions = setOf("kt", "java"), // Only include code files
            maxFileSize = 1024 * 1024
        )

        val result = reader.parseFromDirectory(fileTools, "", config)

        assertTrue(result.success)
        assertEquals(2, result.totalFilesFound) // Only kt and java files
        assertEquals(2, result.filesProcessed)
        assertEquals(2, result.contentRoots.size)

        val titles = result.contentRoots.map { it.title }
        // Debug: Print actual titles to understand the issue
        println("Actual titles: $titles")

        // Since both are plain text files without markdown headers,
        // they will use the first line as title (truncated to 50 chars)
        assertTrue(titles.any { it.contains("/**") || it.contains("Example") }) // Kotlin file
        assertTrue(titles.any { it.contains("public class") || it.contains("Main") }) // Java file
    }

    @Test
    fun `test parseFromDirectory with max depth limit`(@TempDir tempDir: Path) {
        // Create nested directory structure
        val level1 = tempDir.resolve("level1")
        Files.createDirectory(level1)
        val level2 = level1.resolve("level2")
        Files.createDirectory(level2)

        val rootFile = tempDir.resolve("root.md")
        Files.writeString(rootFile, "# Root\nRoot content", StandardOpenOption.CREATE)

        val level1File = level1.resolve("level1.md")
        Files.writeString(level1File, "# Level1\nLevel1 content", StandardOpenOption.CREATE)

        val level2File = level2.resolve("level2.md")
        Files.writeString(level2File, "# Level2\nLevel2 content", StandardOpenOption.CREATE)

        val fileTools = mockk<FileReadTools>()
        every { fileTools.listFiles("") } returns listOf("f:root.md", "d:level1")
        every { fileTools.listFiles("level1") } returns listOf("f:level1.md", "d:level2")
        every { fileTools.resolvePath("root.md") } returns rootFile
        every { fileTools.resolvePath("level1/level1.md") } returns level1File
        every { fileTools.safeReadFile("root.md") } returns Files.readString(rootFile)
        every { fileTools.safeReadFile("level1/level1.md") } returns Files.readString(level1File)

        val config = DirectoryParsingConfig(
            includedExtensions = setOf("md"),
            maxDepth = 1 // Should stop at level1, not go to level2
        )

        val result = reader.parseFromDirectory(fileTools, "", config)

        assertTrue(result.success)
        assertEquals(2, result.totalFilesFound) // Only root.md and level1.md
        assertEquals(2, result.filesProcessed)
        assertEquals(2, result.contentRoots.size)

        val titles = result.contentRoots.map { it.title }
        assertTrue(titles.contains("Root"))
        assertTrue(titles.contains("Level1"))
        assertFalse(titles.contains("Level2")) // Should not be included due to depth limit
    }
}
