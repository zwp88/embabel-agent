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
package com.embabel.examples.dogfood.coding

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byName
import com.embabel.common.util.kotlin.loggerFor
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

/**
 * Common configuration and utilities
 */
@ConfigurationProperties(prefix = "embabel.coding")
class CodingProperties(
    val primaryCodingModel: String = AnthropicModels.CLAUDE_37_SONNET,
) {

    /**
     * Primary coding Llm
     */
    val primaryCodingLlm = LlmOptions(
        criteria = byName(primaryCodingModel),
    )

    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)

        fun createTempDir(seed: String): String {
            val tempDir = Files.createTempDirectory(seed).toFile()
            val tempDirPath = tempDir.absolutePath
            loggerFor<CodingProperties>().info("Created temporary directory at {}", tempDirPath)
            return tempDirPath
        }

        /**
         * Extract zip file to a temporary directory
         */
        fun extractZipFile(
            zipFile: File,
            tempDirPath: String,
            projectName: String,
        ): File {
            val projectDir = File(tempDirPath, projectName)
            projectDir.mkdir()

            // Use Java's ZipInputStream to extract the zip file
            ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val newFile = File(projectDir, zipEntry.name)

                    // Create directories if needed
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // Create parent directories if needed
                        newFile.parentFile.mkdirs()

                        // Extract file
                        FileOutputStream(newFile).use { fileOutputStream ->
                            zipInputStream.copyTo(fileOutputStream)
                        }
                    }

                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
            }

            logger.info("Extracted zip file project to {}", projectDir.absolutePath)

            // Delete the zip file
            zipFile.delete()
            return projectDir
        }
    }
}
