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
package com.embabel.examples.dogfood.presentation

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.dsl.parallelMap
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.ResearchReport
import org.springframework.boot.context.properties.ConfigurationProperties

// TODO make common
data class ResearchTopic(
    val topic: String,
    val questions: List<String>,
)

data class ResearchTopics(
    val topics: List<ResearchTopic>,
)

data class TopicResearch(
    val topic: ResearchTopic,
    val researchReport: ResearchReport,
)

data class ResearchComplete(
    val topicResearches: List<TopicResearch>,
)

data class Deck(
    val deck: String,
)

@ConfigurationProperties("embabel.presentation-maker")
data class PresentationMakerProperties(
    val slideCount: Int = 30,
)


/**
 * Naming agent that generates names for a company or project.
 */
@Agent(description = "Presentation maker. Build a presentation on a topic")
class PresentationMaker(
    private val properties: PresentationMakerProperties,
) {

    @Action
    fun identifyResearchTopics(userInput: UserInput): ResearchTopics =
        using().create(
            """
                Create a list of research topics for a presentation,
                based on the given input:
                ${userInput.content}
                """.trimIndent()
        )

    @Action
    fun researchTopics(
        researchTopics: ResearchTopics,
        context: OperationContext
    ): ResearchComplete {
        val researchReports = researchTopics.topics.parallelMap(context) {
            context.promptRunner(toolGroups = setOf(CoreToolGroups.WEB)).create<ResearchReport>(
                """
            Given the following topic, create a research report.
            Use web tools to research.
            Topic: ${it.topic}
            Questions:
            ${it.questions.joinToString("\n")}
                """.trimIndent()
            )
        }
        return ResearchComplete(
            topicResearches = researchTopics.topics.mapIndexed { index, topic ->
                TopicResearch(
                    topic = topic,
                    researchReport = researchReports[index],
                )
            }
        )
    }

    @AchievesGoal(
        description = "Create a presentation based on research reports",
    )
    @Action
    fun createDeck(
        userInput: UserInput,
        researchComplete: ResearchComplete,
        context: OperationContext,
    ): Deck {
        val reports = researchComplete.topicResearches.map { it.researchReport }
        return context.promptRunner(toolGroups = setOf(CoreToolGroups.WEB)).create<Deck>(
            """
                Create content for a deck based on a research report.
                Use the following input to guide the presentation:
                ${userInput.content}

                Support your points using the following reports:
                Reports:
                $reports

                The presentation should be ${properties.slideCount} slides long.

                Use Marp format.
            """.trimIndent()
        )
    }

}
