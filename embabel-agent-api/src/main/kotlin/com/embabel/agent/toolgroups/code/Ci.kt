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
package com.embabel.agent.toolgroups.code

import com.embabel.agent.toolgroups.DirectoryBased
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.util.kotlin.loggerFor
import org.slf4j.LoggerFactory
import java.nio.file.Paths

/**
 * CI support with pluggable build systems
 */
class Ci(
    override val root: String,
    val buildSystems: List<BuildSystem> = listOf(MavenBuildSystem),
) : DirectoryBased {

    private val logger = LoggerFactory.getLogger(Ci::class.java)

    fun buildAndParse(command: String): BuildResult {
        val rawOutput = build(command)
        return parseBuildOutput(rawOutput)
    }

    fun parseBuildOutput(rawOutput: String): BuildResult {
        for (b in buildSystems) {
            val br = b.parseBuildOutput(root, rawOutput)
            if (br != null) {
                return br
            }
        }
        error("No build system understands this output")
    }

    fun build(command: String): String {
        logger.info("Running build command in root directory: $command")

        val processBuilder = ProcessBuilder()

        // Set the working directory to the root
        processBuilder.directory(Paths.get(root).toFile())

        // Configure the command
        val commandParts = command.split("\\s+".toRegex())
        processBuilder.command(commandParts)

        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true)

        try {
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            return if (exitCode == 0) {
                "Command executed successfully:\n$output"
            } else {
                "Command failed with exit code $exitCode:\n$output"
            }
        } catch (e: Exception) {
            loggerFor<CiTools>().error("Error executing command: $command", e)
            return "Error executing command: ${e.message}"
        }
    }

}

interface BuildSystem {

    /**
     * If possible, parse the build result
     */
    fun parseBuildOutput(
        root: String,
        rawOutput: String,
    ): BuildResult?
}

object MavenBuildSystem : BuildSystem {
    override fun parseBuildOutput(root: String, rawOutput: String): BuildResult? {
        // TODO messy test
        if (!rawOutput.contains("[INFO]")) {
            // Not a Maven build
            return null
        }
        val success = rawOutput.contains("BUILD SUCCESS")
        val relevantOutput = rawOutput.lines().filter { it.contains("[ERROR]") }.joinToString("\n")
        return BuildResult(
            success = success,
            rawOutput = rawOutput,
            relevantOutput = relevantOutput,
        )
    }
}

/**
 * Result of a build command
 * @param relevantOutput only relevant error messages
 *
 */
data class BuildResult(
    val success: Boolean,
    val rawOutput: String,
    val relevantOutput: String,
) : PromptContributor {

    override fun contribution(): String =
        """
            |Build result: success=$success
            |Relevant output:
            |$relevantOutput
        """.trimIndent()
}
