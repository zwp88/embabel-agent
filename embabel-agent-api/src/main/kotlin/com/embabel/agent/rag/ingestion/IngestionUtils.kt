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

import com.embabel.agent.tools.file.FileReadTools
import com.embabel.common.util.loggerFor
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

data class DirectoryIngestionConfig(
    val includedExtensions: Set<String> = IngestionUtils.Companion.DEFAULT_EXTENSIONS,
    val excludedDirectories: Set<String> = IngestionUtils.Companion.DEFAULT_EXCLUDED_DIRS,
    val maxFileSize: Long = 1024 * 1024, // 1MB default
    val followSymlinks: Boolean = false,
    val maxDepth: Int = Int.MAX_VALUE,
    val dryRun: Boolean = false,
)

/**
 * Result of directory ingestion operation
 */
data class DirectoryIngestionResult(
    val totalFilesFound: Int,
    val filesProcessed: Int,
    val filesSkipped: Int,
    val filesErrored: Int,
    val ingestionResults: List<IngestionResult>,
    val processingTime: Duration,
    val errors: List<String>,
) {
    val success: Boolean
        get() = filesErrored == 0 && errors.isEmpty() && ingestionResults.all { it.success() }

    val totalDocumentsIngested: Int
        get() = ingestionResults.sumOf { it.documentsWritten }
}

/**
 * Utilities for ingesting content from directory structures into RAG systems.
 * Uses FileTools for safe file system operations and provides comprehensive logging.
 */
class IngestionUtils(
    val ingester: Ingester,
) {

    private val logger = loggerFor<IngestionUtils>()

    companion object {
        /**
         * Default file extensions to include in ingestion
         */
        val DEFAULT_EXTENSIONS = setOf(
            "txt", "md", "rst", "adoc", "asciidoc",
            "java", "kt", "scala", "py", "js", "ts",
            "go", "rs", "c", "cpp", "h", "hpp",
            "xml", "json", "yaml", "yml", "toml",
            "sql", "html", "css", "scss", "less",
            "sh", "bat", "ps1", "dockerfile"
        )

        /**
         * Directories to exclude from ingestion
         */
        val DEFAULT_EXCLUDED_DIRS = setOf(
            ".git", ".svn", ".hg",
            "node_modules", ".npm",
            "target", "build", "dist", "out",
            ".gradle", ".m2",
            "__pycache__", ".pytest_cache",
            "venv", "env", ".venv",
            ".idea", ".vscode", ".vs",
            "bin", "obj",
            ".next", ".nuxt"
        )
    }

    /**
     * Ingest all files from a directory structure using FileTools for safe access.
     *
     * @param fileTools The FileTools instance to use for file system operations
     * @param directoryPath The relative path to the directory to ingest (relative to FileTools root)
     * @param config Configuration for the ingestion process
     * @return Result of the ingestion operation
     */
    @JvmOverloads
    fun ingestFromDirectory(
        fileTools: FileReadTools,
        directoryPath: String = "",
        config: DirectoryIngestionConfig = DirectoryIngestionConfig(),
    ): DirectoryIngestionResult {
        val startTime = Instant.now()

        logger.info("Starting directory ingestion from '{}' with config: {}", directoryPath, config)

        if (!ingester.active()) {
            logger.warn("Ingester is not active - skipping directory ingestion")
            return DirectoryIngestionResult(
                totalFilesFound = 0,
                filesProcessed = 0,
                filesSkipped = 0,
                filesErrored = 0,
                ingestionResults = emptyList(),
                processingTime = Duration.between(startTime, Instant.now()),
                errors = listOf("Ingester is not active")
            )
        }

        return try {
            val files = discoverFiles(fileTools, directoryPath, config)
            logger.info("Discovered {} files for ingestion", files.size)

            if (config.dryRun) {
                logger.info("DRY RUN - would process {} files", files.size)
                files.forEach { logger.info("Would ingest: {}", it) }
                return DirectoryIngestionResult(
                    totalFilesFound = files.size,
                    filesProcessed = 0,
                    filesSkipped = files.size,
                    filesErrored = 0,
                    ingestionResults = emptyList(),
                    processingTime = Duration.between(startTime, Instant.now()),
                    errors = emptyList()
                )
            }

            processFiles(fileTools, files, config, startTime)

        } catch (e: Exception) {
            logger.error("Failed to ingest directory '{}': {}", directoryPath, e.message, e)
            DirectoryIngestionResult(
                totalFilesFound = 0,
                filesProcessed = 0,
                filesSkipped = 0,
                filesErrored = 1,
                ingestionResults = emptyList(),
                processingTime = Duration.between(startTime, Instant.now()),
                errors = listOf("Directory ingestion failed: ${e.message}")
            )
        }
    }

    /**
     * Ingest a single file using the configured ingester.
     *
     * @param fileTools The FileTools instance to use for file access
     * @param filePath The relative path to the file to ingest
     * @return Result of the ingestion operation, or null if the file couldn't be processed
     */
    fun ingestFile(
        fileTools: FileReadTools,
        filePath: String,
    ): IngestionResult? {
        return try {
            logger.debug("Ingesting single file: {}", filePath)

            if (!ingester.active()) {
                logger.warn("Ingester is not active - skipping file ingestion for '{}'", filePath)
                return null
            }

            // Validate file exists and is readable through FileTools
            val content = fileTools.safeReadFile(filePath)
            if (content == null) {
                logger.warn("Could not read file: {}", filePath)
                return null
            }

            // Use file:// protocol for local files as expected by Spring Resource
            val resourcePath = "file://" + fileTools.resolvePath(filePath).toAbsolutePath()
            val result = ingester.ingest(resourcePath)

            logger.info(
                "Successfully ingested file '{}' - {} documents written to {} stores",
                filePath, result.documentsWritten, result.storesWrittenTo.size
            )

            result

        } catch (e: Exception) {
            logger.error("Failed to ingest file '{}': {}", filePath, e.message, e)
            null
        }
    }

    /**
     * Discover all files in the directory structure that match the ingestion criteria.
     */
    private fun discoverFiles(
        fileTools: FileReadTools,
        directoryPath: String,
        config: DirectoryIngestionConfig,
    ): List<String> {
        val files = mutableListOf<String>()
        val startPath = if (directoryPath.isEmpty()) "" else directoryPath

        logger.debug("Discovering files in directory: {}", startPath)

        try {
            discoverFilesRecursive(fileTools, startPath, files, config, 0)
        } catch (e: Exception) {
            logger.error("Error discovering files in '{}': {}", startPath, e.message, e)
        }

        logger.debug("Discovered {} files in directory '{}'", files.size, startPath)
        return files
    }

    /**
     * Recursively discover files in a directory structure.
     */
    private fun discoverFilesRecursive(
        fileTools: FileReadTools,
        currentPath: String,
        files: MutableList<String>,
        config: DirectoryIngestionConfig,
        depth: Int,
    ) {
        if (depth > config.maxDepth) {
            logger.debug("Reached max depth {} at path '{}'", config.maxDepth, currentPath)
            return
        }

        try {
            val entries = fileTools.listFiles(currentPath)

            for (entry in entries) {
                val isDirectory = entry.startsWith("d:")
                val name = entry.substring(2) // Remove "d:" or "f:" prefix
                val fullPath = if (currentPath.isEmpty()) name else "$currentPath/$name"

                if (isDirectory) {
                    // Check if directory should be excluded
                    if (name in config.excludedDirectories) {
                        logger.debug("Skipping excluded directory: {}", fullPath)
                        continue
                    }

                    // Recurse into subdirectory
                    discoverFilesRecursive(fileTools, fullPath, files, config, depth + 1)
                } else {
                    // Check file extension
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension in config.includedExtensions) {
                        // Check file size
                        val resolvedPath = fileTools.resolvePath(fullPath)
                        if (Files.exists(resolvedPath)) {
                            val size = Files.size(resolvedPath)
                            if (size <= config.maxFileSize) {
                                files.add(fullPath)
                                logger.trace("Added file for ingestion: {} (size: {} bytes)", fullPath, size)
                            } else {
                                logger.debug(
                                    "Skipping large file: {} (size: {} bytes, limit: {} bytes)",
                                    fullPath, size, config.maxFileSize
                                )
                            }
                        }
                    } else {
                        logger.trace("Skipping file with excluded extension: {} (extension: {})", fullPath, extension)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not list files in directory '{}': {}", currentPath, e.message)
        }
    }

    /**
     * Process the discovered files for ingestion.
     */
    private fun processFiles(
        fileTools: FileReadTools,
        files: List<String>,
        config: DirectoryIngestionConfig,
        startTime: Instant,
    ): DirectoryIngestionResult {
        var filesProcessed = 0
        var filesSkipped = 0
        var filesErrored = 0
        val ingestionResults = mutableListOf<IngestionResult>()
        val errors = mutableListOf<String>()

        logger.info("Processing {} files for ingestion", files.size)

        for ((index, filePath) in files.withIndex()) {
            if ((index + 1) % 100 == 0) {
                logger.info("Progress: {}/{} files processed", index + 1, files.size)
            }

            try {
                val result = ingestFile(fileTools, filePath)
                if (result != null) {
                    if (result.success()) {
                        ingestionResults.add(result)
                        filesProcessed++
                        logger.debug(
                            "Successfully processed file {} ({}/{}): {} documents",
                            filePath, index + 1, files.size, result.documentsWritten
                        )
                    } else {
                        filesErrored++
                        val error = "Failed to ingest file '$filePath': no stores written to"
                        errors.add(error)
                        logger.warn(error)
                    }
                } else {
                    filesSkipped++
                    logger.debug("Skipped file {} ({}/{})", filePath, index + 1, files.size)
                }
            } catch (e: Exception) {
                filesErrored++
                val error = "Error processing file '$filePath': ${e.message}"
                errors.add(error)
                logger.error(error, e)
            }
        }

        val processingTime = Duration.between(startTime, Instant.now())

        logger.info("Directory ingestion completed in {} ms", processingTime.toMillis())
        logger.info("Files processed: {}, skipped: {}, errors: {}", filesProcessed, filesSkipped, filesErrored)
        logger.info("Total documents ingested: {}", ingestionResults.sumOf { it.documentsWritten })

        return DirectoryIngestionResult(
            totalFilesFound = files.size,
            filesProcessed = filesProcessed,
            filesSkipped = filesSkipped,
            filesErrored = filesErrored,
            ingestionResults = ingestionResults,
            processingTime = processingTime,
            errors = errors
        )
    }
}
