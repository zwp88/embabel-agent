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
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.support.using
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.create
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.Location
import com.embabel.common.ai.prompt.PromptContribution
import com.embabel.common.ai.prompt.PromptContributor
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository

@JsonClassDescription("Analysis of a technology project")
data class Project(
    val location: String,
    @get:JsonPropertyDescription("The technologies used in the project. List, comma separated. Include 10")
    val tech: String,
    @get: JsonPropertyDescription("Notes on the coding style used in this project. 20 words.")
    val codingStyle: String,
) : PromptContributor {

    override fun promptContribution() = PromptContribution(
        """
            |Project:
            |$tech
            |
            |Coding style:
            |$codingStyle
        """.trimMargin(),
        location = Location.BEGINNING,
        role = "project",
    )
}

data class Explanation(
    override val text: String,
) : HasContent

interface ProjectRepository : CrudRepository<Project, String>


@Agent(
    description = "Explain code",
    toolGroups = [
        "file",
//            ToolGroup.WEB,
    ],
)
class CodeExplainer(
    val projectRepository: ProjectRepository,
    val defaultLocation: String = System.getProperty("user.dir"),
) {

    private val logger = LoggerFactory.getLogger(CodeExplainer::class.java)

    private val claudeSonnet = LlmOptions(
        AnthropicModels.CLAUDE_37_SONNET
    )

    @Action
    fun loadExistingProject(): Project? {
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
    fun analyzeProject(): Project =
        using(claudeSonnet).create<Project>(
            """
                Analyze the project at $defaultLocation
                Use the file tools to read code and directories before analyzing it
            """.trimIndent(),
        ).also { project ->
            // So we don't need to do this again
            projectRepository.save(project)
        }

    @Action
    @AchievesGoal(description = "Code has been explained to the user")
    fun explainCode(
        userInput: UserInput,
        project: Project,
        payload: OperationPayload
    ): Explanation {
        val explanation: String = payload.promptRunner(
            llm = LlmOptions(
                AnthropicModels.CLAUDE_37_SONNET
            ),
            promptContributors = listOf(project)
        ).create(
            """
                Execute the following user request around explaining or modifying code in the given project.
                Use the file tools to read code and directories before explaining it

                User request:
                "${userInput.content}"
            """.trimIndent(),
        )
        return Explanation(explanation)
    }
}
