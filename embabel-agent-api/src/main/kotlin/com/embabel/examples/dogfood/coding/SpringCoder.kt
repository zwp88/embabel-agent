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

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agentic
import com.embabel.agent.api.common.ActionContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

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

object SpringCoderConditions {
    const val SpringProjectCreated = "springProjectCreated"
}

@Agentic(scan = false)
@Profile("!test")
class SpringCoder(
    private val codingProperties: CodingProperties,
) {

    private val logger = LoggerFactory.getLogger(SpringCoder::class.java)

    @Action(
        post = [SpringCoderConditions.SpringProjectCreated]
    )
    fun createSpringInitialzrProject(context: ActionContext): SoftwareProject {
        logger.info("Creating Spring Initialzr project")

        val tempDirPath = CodingProperties.createTempDir("spring-initializr")

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

        val projectDir = CodingProperties.extractZipFile(
            zipFile,
            tempDirPath,
            springRecipe.artifactId,
        )
        logger.info("Extracted Spring Initialzr project to {}", projectDir.absolutePath)

        // Return the project coordinates
//        payload.setCondition(Conditions.SpringProjectCreated, true)
        context += springRecipe
        return SoftwareProject(
            root = projectDir.absolutePath,
            tech = "Kotlin, Spring Boot, Maven, Spring Web, Spring Actuator, Spring DevTools",
            codingStyle = "Modern Kotlin with Spring Boot conventions. Clean architecture with separation of concerns.",
            buildCommand = "mvn test",
        )
    }

    @Action(
        pre = [SpringCoderConditions.SpringProjectCreated,
            CodeWriterConditions.BuildSucceeded],
    )
    @AchievesGoal("Create a new Spring project")
    fun describeShinyNewSpringProject(softwareProject: SoftwareProject, springRecipe: SpringRecipe): CodeExplanation =
        CodeExplanation(
            text = """
                Project root: ${softwareProject.root}
                Technologies used: ${softwareProject.tech}
                Coding style: ${softwareProject.codingStyle}
            """.trimIndent(),
            links = emptyList(),
        )

}
