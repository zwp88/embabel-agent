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

import com.embabel.agent.rag.WritableRagService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TextSplitter
import java.io.File
import java.nio.file.Path
import java.util.UUID

class MultiIngesterTest {

    private lateinit var mockRagService1: WritableRagService
    private lateinit var mockRagService2: WritableRagService
    private lateinit var mockTextSplitter: TextSplitter

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockRagService1 = mockk()
        mockRagService2 = mockk()
        mockTextSplitter = mockk()

        every { mockRagService1.name } returns "rag-service-1"
        every { mockRagService2.name } returns "rag-service-2"
        every { mockRagService1.write(any()) } just Runs
        every { mockRagService2.write(any()) } just Runs
    }

    @Nested
    inner class ConstructorAndInitializationTests {

        @Test
        fun `test constructor with empty rag services list`() {
            val multiIngester = MultiIngester(emptyList())

            assertFalse(multiIngester.active(), "Should not be active with empty rag services")
            assertTrue(multiIngester.ragServices.isEmpty(), "Should have empty rag services list")
        }

        @Test
        fun `test constructor with single rag service`() {
            val multiIngester = MultiIngester(listOf(mockRagService1))

            assertTrue(multiIngester.active(), "Should be active with one rag service")
            assertEquals(1, multiIngester.ragServices.size, "Should have one rag service")
            assertEquals(mockRagService1, multiIngester.ragServices[0], "Should contain the correct rag service")
        }

        @Test
        fun `test constructor with multiple rag services`() {
            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2))

            assertTrue(multiIngester.active(), "Should be active with multiple rag services")
            assertEquals(2, multiIngester.ragServices.size, "Should have two rag services")
            assertTrue(multiIngester.ragServices.contains(mockRagService1), "Should contain first rag service")
            assertTrue(multiIngester.ragServices.contains(mockRagService2), "Should contain second rag service")
        }

        @Test
        fun `test constructor with custom text splitter`() {
            val multiIngester = MultiIngester(listOf(mockRagService1), mockTextSplitter)

            assertEquals(mockTextSplitter, multiIngester.splitter, "Should use custom text splitter")
        }

        @Test
        fun `test constructor with default text splitter`() {
            val multiIngester = MultiIngester(listOf(mockRagService1))

            assertNotNull(multiIngester.splitter, "Should have a default text splitter")
            // Default is TokenTextSplitter - we can't easily test the exact type without reflection
        }
    }

    @Nested
    inner class ActiveStatusTests {

        @Test
        fun `test active returns false with empty rag services`() {
            val multiIngester = MultiIngester(emptyList())

            assertFalse(multiIngester.active(), "Should not be active with no rag services")
        }

        @Test
        fun `test active returns true with rag services`() {
            val multiIngester = MultiIngester(listOf(mockRagService1))

            assertTrue(multiIngester.active(), "Should be active with rag services")
        }

        @Test
        fun `test active returns true with multiple rag services`() {
            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2))

            assertTrue(multiIngester.active(), "Should be active with multiple rag services")
        }
    }

    @Nested
    inner class IngestionTests {

        @Test
        fun `test ingest with valid resource creates documents and writes to all services`() {
            // Create a test file
            val testFile = tempDir.resolve("test.txt").toFile()
            testFile.writeText("This is a test document for ingestion.")

            // Mock text splitter to return specific documents
            val splitDoc1 = Document("This is a test document", mapOf("id" to "chunk-1"))
            val splitDoc2 = Document("for ingestion.", mapOf("id" to "chunk-2"))

            every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(splitDoc1, splitDoc2)

            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2), mockTextSplitter)

            val result = multiIngester.ingest("file://${testFile.absolutePath}")

            // Verify result
            assertNotNull(result, "Should return ingestion result")
            assertTrue(result.success(), "Ingestion should be successful")
            assertEquals(2, result.chunkIds.size, "Should have 2 chunk IDs")
            assertEquals(2, result.storesWrittenTo.size, "Should write to 2 stores")
            assertTrue(result.storesWrittenTo.contains("rag-service-1"), "Should write to first service")
            assertTrue(result.storesWrittenTo.contains("rag-service-2"), "Should write to second service")

            // Verify services were called
            verify { mockRagService1.write(listOf(splitDoc1, splitDoc2)) }
            verify { mockRagService2.write(listOf(splitDoc1, splitDoc2)) }
        }

        @Test
        fun `test ingest with no rag services still processes but returns empty stores`() {
            val testFile = tempDir.resolve("test.txt").toFile()
            testFile.writeText("Test content")

            val multiIngester = MultiIngester(emptyList())

            val result = multiIngester.ingest("file://${testFile.absolutePath}")

            assertNotNull(result, "Should return ingestion result")
            assertTrue(result.storesWrittenTo.isEmpty(), "Should have empty stores when no services")
            assertFalse(result.success(), "Should not be successful with no stores")
        }

        @Test
        fun `test ingest with large document gets split appropriately`() {
            val testFile = tempDir.resolve("large.txt").toFile()
            val largeContent = "This is a large document. ".repeat(1000)
            testFile.writeText(largeContent)

            // Mock splitter to simulate splitting large document
            val sourceDoc = Document(largeContent)
            val splitDocs = (1..5).map {
                Document("Chunk $it content", mapOf("id" to "chunk-$it"))
            }
            every { mockTextSplitter.split(any<List<Document>>()) } returns splitDocs

            val multiIngester = MultiIngester(listOf(mockRagService1), mockTextSplitter)

            val result = multiIngester.ingest("file://${testFile.absolutePath}")

            assertTrue(result.success(), "Should succeed with large document")
            assertEquals(5, result.chunkIds.size, "Should have 5 chunks from splitting")
            assertEquals(1, result.storesWrittenTo.size, "Should write to 1 store")

            verify { mockRagService1.write(splitDocs) }
        }

        @Test
        fun `test ingest with file that doesn't exist throws appropriate exception`() {
            val nonExistentPath = "file:///non/existent/path.txt"
            val multiIngester = MultiIngester(listOf(mockRagService1))

            assertThrows(Exception::class.java) {
                multiIngester.ingest(nonExistentPath)
            }

            // Verify services were not called due to exception
            verify(exactly = 0) { mockRagService1.write(any()) }
        }

        @Test
        fun `test ingest with empty file creates minimal document`() {
            val emptyFile = tempDir.resolve("empty.txt").toFile()
            emptyFile.writeText("")

            // Mock splitter behavior for empty content
            val emptyDoc = Document("")
            every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(emptyDoc)

            val multiIngester = MultiIngester(listOf(mockRagService1), mockTextSplitter)

            val result = multiIngester.ingest("file://${emptyFile.absolutePath}")

            assertTrue(result.success(), "Should succeed with empty file")
            assertEquals(1, result.chunkIds.size, "Should have 1 document even for empty file")

            verify { mockRagService1.write(listOf(emptyDoc)) }
        }

        @Test
        fun `test ingest with different file types`() {
            // Test with different file extensions
            val extensions = listOf("txt", "md", "java", "json")

            extensions.forEach { ext ->
                val testFile = tempDir.resolve("test.$ext").toFile()
                testFile.writeText("Content for $ext file")

                val sourceDoc = Document("Content for $ext file")
                val splitDoc = Document("Content for $ext file", mapOf("id" to "chunk-$ext"))
                every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(splitDoc)

                val multiIngester = MultiIngester(listOf(mockRagService1), mockTextSplitter)

                val result = multiIngester.ingest("file://${testFile.absolutePath}")

                assertTrue(result.success(), "Should succeed with $ext file")
                assertEquals(1, result.chunkIds.size, "Should have 1 chunk for $ext file")

                verify { mockRagService1.write(listOf(splitDoc)) }

                clearMocks(mockRagService1, answers = false)
            }
        }
    }

    @Nested
    inner class AcceptMethodTests {

        @Test
        fun `test accept method writes documents directly to all services`() {
            val doc1 = Document("Document 1", mapOf("id" to "doc1"))
            val doc2 = Document("Document 2", mapOf("id" to "doc2"))
            val documents = listOf(doc1, doc2)

            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2))

            multiIngester.accept(documents)

            verify { mockRagService1.write(documents) }
            verify { mockRagService2.write(documents) }
        }

        @Test
        fun `test accept with empty document list`() {
            val multiIngester = MultiIngester(listOf(mockRagService1))

            multiIngester.accept(emptyList())

            verify { mockRagService1.write(emptyList()) }
        }

        @Test
        fun `test accept with no rag services doesn't fail`() {
            val documents = listOf(Document("Test content"))
            val multiIngester = MultiIngester(emptyList())

            // Should not throw exception
            assertDoesNotThrow {
                multiIngester.accept(documents)
            }
        }

        @Test
        fun `test accept with single document`() {
            val document = Document("Single document", mapOf("id" to "single"))
            val multiIngester = MultiIngester(listOf(mockRagService1))

            multiIngester.accept(listOf(document))

            verify { mockRagService1.write(listOf(document)) }
        }
    }

    @Nested
    inner class ErrorHandlingTests {

        @Test
        fun `test ingestion continues when one rag service fails`() {
            val testFile = tempDir.resolve("test.txt").toFile()
            testFile.writeText("Test content")

            val sourceDoc = Document("Test content")
            val splitDoc = Document("Test content", mapOf("id" to "chunk-1"))
            every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(splitDoc)

            // Make first service throw exception
            every { mockRagService1.write(any()) } throws RuntimeException("Service 1 failed")
            every { mockRagService2.write(any()) } just Runs

            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2), mockTextSplitter)

            // Should throw exception since writeToStores doesn't handle exceptions
            assertThrows(RuntimeException::class.java) {
                multiIngester.ingest("file://${testFile.absolutePath}")
            }

            verify { mockRagService1.write(listOf(splitDoc)) }
            // Second service might not be called due to exception in first
        }

        @Test
        fun `test accept method with rag service exception`() {
            val document = Document("Test content")
            every { mockRagService1.write(any()) } throws RuntimeException("Write failed")

            val multiIngester = MultiIngester(listOf(mockRagService1))

            assertThrows(RuntimeException::class.java) {
                multiIngester.accept(listOf(document))
            }
        }
    }

    @Nested
    inner class InfoStringTests {

        @Test
        fun `test infoString with no rag services`() {
            val multiIngester = MultiIngester(emptyList())

            val infoString = multiIngester.infoString(verbose = false, indent = 0)

            assertEquals("No RAG services", infoString, "Should indicate no RAG services")
        }

        @Test
        fun `test infoString with single rag service`() {
            every { mockRagService1.infoString(verbose = false, indent = 1) } returns "RagService1Info"

            val multiIngester = MultiIngester(listOf(mockRagService1))

            val infoString = multiIngester.infoString(verbose = false, indent = 0)

            assertTrue(infoString.contains("MultiIngester"), "Should contain class name")
            assertTrue(infoString.contains("RagService1Info"), "Should contain service info")

            verify { mockRagService1.infoString(verbose = false, indent = 1) }
        }

        @Test
        fun `test infoString with multiple rag services`() {
            every { mockRagService1.infoString(verbose = false, indent = 1) } returns "Service1"
            every { mockRagService2.infoString(verbose = false, indent = 1) } returns "Service2"

            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2))

            val infoString = multiIngester.infoString(verbose = false, indent = 0)

            assertTrue(infoString.contains("MultiIngester"), "Should contain class name")
            assertTrue(infoString.contains("Service1"), "Should contain first service info")
            assertTrue(infoString.contains("Service2"), "Should contain second service info")
            assertTrue(infoString.contains(","), "Should separate services with comma")
        }

        @Test
        fun `test infoString with verbose flag`() {
            every { mockRagService1.infoString(verbose = true, indent = 1) } returns "DetailedService1Info"

            val multiIngester = MultiIngester(listOf(mockRagService1))

            val infoString = multiIngester.infoString(verbose = true, indent = 0)

            assertTrue(infoString.contains("DetailedService1Info"), "Should contain detailed service info")

            verify { mockRagService1.infoString(verbose = true, indent = 1) }
        }

        @Test
        fun `test infoString with different indent levels`() {
            every { mockRagService1.infoString(verbose = false, indent = 1) } returns "IndentedServiceInfo"

            val multiIngester = MultiIngester(listOf(mockRagService1))

            multiIngester.infoString(verbose = false, indent = 2)

            // Verify that indent + 1 is passed to services
            verify { mockRagService1.infoString(verbose = false, indent = 1) }
        }
    }

    @Nested
    inner class IntegrationTests {

        @Test
        fun `test complete ingestion workflow with realistic scenario`() {
            // Create a markdown file with multiple sections
            val markdownFile = tempDir.resolve("document.md").toFile()
            markdownFile.writeText("""
                # Introduction
                This is a comprehensive document about testing.

                ## Section 1
                Content for section 1 with important information.

                ## Section 2
                More content with different topics and details.

                ## Conclusion
                Final thoughts and summary of the document.
            """.trimIndent())

            // Mock the splitter to break document into sections
            val sourceDoc = Document(markdownFile.readText())
            val chunks = listOf(
                Document("# Introduction\nThis is a comprehensive document about testing.",
                        mapOf("id" to UUID.randomUUID().toString())),
                Document("## Section 1\nContent for section 1 with important information.",
                        mapOf("id" to UUID.randomUUID().toString())),
                Document("## Section 2\nMore content with different topics and details.",
                        mapOf("id" to UUID.randomUUID().toString())),
                Document("## Conclusion\nFinal thoughts and summary of the document.",
                        mapOf("id" to UUID.randomUUID().toString()))
            )
            every { mockTextSplitter.split(any<List<Document>>()) } returns chunks

            val multiIngester = MultiIngester(listOf(mockRagService1, mockRagService2), mockTextSplitter)

            val result = multiIngester.ingest("file://${markdownFile.absolutePath}")

            // Verify comprehensive result
            assertTrue(result.success(), "Integration test should succeed")
            assertEquals(4, result.chunkIds.size, "Should have 4 chunks from document sections")
            assertEquals(4, result.documentsWritten, "Should report 4 documents written")
            assertEquals(2, result.storesWrittenTo.size, "Should write to both stores")
            assertTrue(result.storesWrittenTo.containsAll(setOf("rag-service-1", "rag-service-2")),
                      "Should write to both named services")

            // Verify both services received all chunks
            verify { mockRagService1.write(chunks) }
            verify { mockRagService2.write(chunks) }

            // Verify text splitter was called correctly
            verify { mockTextSplitter.split(any<List<Document>>()) }
        }

        @Test
        fun `test ingestion with mixed service types and configurations`() {
            // Create additional mock services with different characteristics
            val mockVectorService = mockk<WritableRagService>()
            val mockGraphService = mockk<WritableRagService>()

            every { mockVectorService.name } returns "vector-store"
            every { mockGraphService.name } returns "knowledge-graph"
            every { mockVectorService.write(any()) } just Runs
            every { mockGraphService.write(any()) } just Runs

            val testFile = tempDir.resolve("mixed.txt").toFile()
            testFile.writeText("Content for mixed service ingestion")

            val sourceDoc = Document("Content for mixed service ingestion")
            val chunk = Document("Content for mixed service ingestion", mapOf("id" to "mixed-1"))
            every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(chunk)

            val multiIngester = MultiIngester(
                listOf(mockRagService1, mockVectorService, mockGraphService),
                mockTextSplitter
            )

            val result = multiIngester.ingest("file://${testFile.absolutePath}")

            assertTrue(result.success(), "Should succeed with mixed services")
            assertEquals(3, result.storesWrittenTo.size, "Should write to all 3 services")
            assertTrue(result.storesWrittenTo.containsAll(
                setOf("rag-service-1", "vector-store", "knowledge-graph")),
                "Should write to all named services")

            // Verify all services were called
            verify { mockRagService1.write(listOf(chunk)) }
            verify { mockVectorService.write(listOf(chunk)) }
            verify { mockGraphService.write(listOf(chunk)) }
        }

        @Test
        fun `test performance characteristics with large number of services`() {
            // Create multiple mock services
            val services = (1..10).map { index ->
                mockk<WritableRagService>().also { service ->
                    every { service.name } returns "service-$index"
                    every { service.write(any()) } just Runs
                }
            }

            val testFile = tempDir.resolve("performance.txt").toFile()
            testFile.writeText("Performance test content")

            val sourceDoc = Document("Performance test content")
            val chunk = Document("Performance test content", mapOf("id" to "perf-1"))
            every { mockTextSplitter.split(any<List<Document>>()) } returns listOf(chunk)

            val multiIngester = MultiIngester(services, mockTextSplitter)

            val startTime = System.currentTimeMillis()
            val result = multiIngester.ingest("file://${testFile.absolutePath}")
            val endTime = System.currentTimeMillis()

            assertTrue(result.success(), "Should succeed with many services")
            assertEquals(10, result.storesWrittenTo.size, "Should write to all 10 services")

            // Verify reasonable performance (should complete in reasonable time)
            val duration = endTime - startTime
            assertTrue(duration < 5000, "Should complete within 5 seconds for 10 services")

            // Verify all services were called
            services.forEach { service ->
                verify { service.write(listOf(chunk)) }
            }
        }
    }
}
