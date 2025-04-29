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

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.spi.support.SelfToolGroup
import com.embabel.agent.toolgroups.DirectoryBased
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.util.kotlin.loggerFor
import org.springframework.ai.tool.annotation.Tool
import java.nio.file.Paths

interface CiTools : SelfToolGroup, DirectoryBased {

    override val description
        get() = ToolGroup.CI_DESCRIPTION

    override val permissions get() = setOf(ToolGroupPermission.HOST_ACCESS)

    @Tool(description = "build the project using the given command in the root")
    fun buildProject(command: String): String {
        loggerFor<CiTools>().info("Running build command in root directory: $command")

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

    companion object {
        operator fun invoke(root: String): CiTools = object : CiTools {
            override val root: String = root
        }
    }

}

fun toMavenBuildResult(
    rawOutput: String,
): BuildResult {
    val success = rawOutput.contains("BUILD SUCCESS")
    val relevantOutput = rawOutput.lines().filter { it.contains("[ERROR]") }.joinToString("\n")
    return BuildResult(
        success = success,
        rawOutput = rawOutput,
        relevantOutput = relevantOutput,
    )
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
