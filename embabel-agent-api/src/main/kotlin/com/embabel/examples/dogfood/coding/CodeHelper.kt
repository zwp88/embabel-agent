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

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.create
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.lastOrNull
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.toolgroups.code.BuildResult
import com.embabel.agent.toolgroups.code.toMavenBuildResult
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository

@JsonClassDescription("Analysis of a technology project")
data class SoftwareProject(
    val location: String,
    @get:JsonPropertyDescription("The technologies used in the project. List, comma separated. Include 10")
    val tech: String,
    @get: JsonPropertyDescription("Notes on the coding style used in this project. 20 words.")
    val codingStyle: String,
) : PromptContributor {

    override fun contribution() =
        """
            |Project:
            |$tech
            |
            |Coding style:
            |$codingStyle
        """.trimMargin()


}

data class Explanation(
    override val text: String,
) : HasContent

data class CodeModificationReport(
    override val text: String,
) : HasContent

interface ProjectRepository : CrudRepository<SoftwareProject, String>

object Conditions {
    const val BuildSucceeded = "buildSucceeded"
    const val SpringProjectCreated = "springProjectCreated"
}

data class SpringRecipe(
    val projectName: String = "demo",
    val groupId: String = "com.example",
    val artifactId: String = "demo",
    val version: String = "0.0.1-SNAPSHOT",
    val bootVersion: String = "3.2.0",
    val language: String = "kotlin",
    val packaging: String = "jar",
    val javaVersion: String = "17",
    val dependencies: String = "web,actuator,devtools",
)

@Agent(
    description = "Explain code or perform changes to a software project or directory structure",
    toolGroups = [
        ToolGroup.FILE,
    ],
)
class CodeHelper(
    val projectRepository: ProjectRepository,
    val defaultLocation: String = System.getProperty("user.dir") + "/embabel-agent-api",
) {

    private val logger = LoggerFactory.getLogger(CodeHelper::class.java)

    private val claudeSonnet = LlmOptions(
        AnthropicModels.CLAUDE_37_SONNET
    )

    @Action
    fun loadExistingProject(): SoftwareProject? {
        val found = projectRepository.findById(defaultLocation)
        if (found.isPresent) {
            logger.info("Found existing project at $defaultLocation")
        }
        return found.orElse(null)
    }

    /**
     * Use an LLM to analyze the project.
     * This is expensive so we set cost high
     */
    @Action(cost = 10000.0)
    fun analyzeProject(): SoftwareProject =
        using(claudeSonnet).create<SoftwareProject>(
            """
                Analyze the project at $defaultLocation
                Use the file tools to read code and directories before analyzing it
            """.trimIndent(),
        ).also { project ->
            // So we don't need to do this again
            projectRepository.save(project)
        }

    @Action(
//        post = [Conditions.SpringProjectCreated]
    )
    fun createSpringInitialzrProject(payload: OperationPayload): SoftwareProject {
        logger.info("Creating Spring Initialzr project")

        // Create a temporary directory to store the project
        val tempDir = java.nio.file.Files.createTempDirectory("spring-initialzr-").toFile()
        val tempDirPath = tempDir.absolutePath
        logger.info("Created temporary directory at {}", tempDirPath)

        // Create RestClient to call Spring Initialzr
        val restClient = org.springframework.web.client.RestClient.builder()
            .baseUrl("https://start.spring.io")
            .build()

        val springRecipe = SpringRecipe()
        // Make the request to Spring Initialzr and save the response to a zip file
        val zipFile = java.io.File("$tempDirPath/${springRecipe.artifactId}.zip")
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/starter.zip")
                    .queryParam("name", springRecipe.projectName)
                    .queryParam("groupId", springRecipe.groupId)
                    .queryParam("artifactId", springRecipe.artifactId)
                    .queryParam("version", springRecipe.version)
                    .queryParam("bootVersion", springRecipe.bootVersion)
                    .queryParam("language", springRecipe.language)
                    .queryParam("packaging", springRecipe.packaging)
                    .queryParam("javaVersion", springRecipe.javaVersion)
                    .queryParam("dependencies", springRecipe.dependencies)
                    .build()
            }
            .retrieve()
            .toEntity(ByteArray::class.java)
            .body ?: throw RuntimeException("Failed to download Spring Initialzr project")

        // Save the response to a zip file
        zipFile.writeBytes(response)
        logger.info("Downloaded Spring Initialzr project to {}", zipFile.absolutePath)

        // Extract the zip file
        val projectDir = java.io.File(tempDirPath, springRecipe.artifactId)
        projectDir.mkdir()

        // Use Java's ZipInputStream to extract the zip file
        java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = java.io.File(projectDir, zipEntry.name)

                // Create directories if needed
                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // Create parent directories if needed
                    newFile.parentFile.mkdirs()

                    // Extract file
                    java.io.FileOutputStream(newFile).use { fileOutputStream ->
                        zipInputStream.copyTo(fileOutputStream)
                    }
                }

                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
        }

        logger.info("Extracted Spring Initialzr project to {}", projectDir.absolutePath)

        // Delete the zip file
        zipFile.delete()

        // Return the project coordinates
//        payload.setCondition(Conditions.SpringProjectCreated, true)
        payload += springRecipe
        return SoftwareProject(
            location = projectDir.absolutePath,
            tech = "Kotlin, Spring Boot, Maven, Spring Web, Spring Actuator, Spring DevTools",
            codingStyle = "Modern Kotlin with Spring Boot conventions. Clean architecture with separation of concerns."
        )
    }

    @Action(
//        pre = [Conditions.SpringProjectCreated]
    )
    @AchievesGoal("Create a new Spring project")
    fun describeShinyNewSpringProject(softwareProject: SoftwareProject, springRecipe: SpringRecipe): Explanation =
        Explanation(
            text = """
                Project location: ${softwareProject.location}
                Technologies used: ${softwareProject.tech}
                Coding style: ${softwareProject.codingStyle}
            """.trimIndent()
        )


    @Action(
        cost = 1000.0,
        canRerun = true,
        toolGroups = [
            ToolGroup.FILE,
            ToolGroup.CI,
        ],
    )
    fun build(project: SoftwareProject, payload: OperationPayload): BuildResult {
        val buildOutput = payload.promptRunner(
            llm = claudeSonnet,
            promptContributors = listOf(project)
        ).generateText(
            """
                Build the project at $defaultLocation
                Using the appropriate tool
            """.trimIndent(),
        )
        return toMavenBuildResult(buildOutput)
    }

    @Action(
        toolGroups = [
            ToolGroup.FILE,
        ],
    )
    @AchievesGoal(description = "Code has been explained to the user")
    fun explainCode(
        userInput: UserInput,
        project: SoftwareProject,
        payload: OperationPayload,
    ): Explanation {
        val explanation: String = payload.promptRunner(
            llm = claudeSonnet,
            promptContributors = listOf(project)
        ).create(
            """
                Execute the following user request to explain something about the given project.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.

                User request:
                "${userInput.content}"
            }
            """.trimIndent(),
        )
        return Explanation(explanation)
    }

    @Condition(name = Conditions.BuildSucceeded)
    fun buildSucceeded(buildResult: BuildResult): Boolean = buildResult.success

    @Action(pre = [Conditions.BuildSucceeded])
    @AchievesGoal(description = "Modify project code as per user request")
    fun done(codeModificationReport: CodeModificationReport): CodeModificationReport {
        return codeModificationReport
    }

    @Action(canRerun = true, post = [Conditions.BuildSucceeded])
    fun modifyCode(
        userInput: UserInput,
        project: SoftwareProject,
        payload: OperationPayload,
    ): CodeModificationReport {
        val buildFailure = payload.lastOrNull<BuildResult> { !it.success }
        val report: String = payload.promptRunner(
            llm = claudeSonnet,
            promptContributors = listOf(project, buildFailure),
        ).create(
            """
                Execute the following user request to modify code in the given project.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.
                The project will be in git so you can safely modify content without worrying about backups.
                Return an explanation of what you did and why.
                Consider any build failure report.

                User request:
                "${userInput.content}"

                ${
                buildFailure?.let {
                    "Previous build failed:\n" + it.contribution()
                }
            }
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }
}
