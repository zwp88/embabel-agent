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
import com.embabel.agent.domain.library.ResearchTopic
import com.embabel.agent.domain.library.ResearchTopics
import com.embabel.agent.experimental.prompt.CoStar
import com.embabel.agent.tools.file.FileTools
import com.embabel.common.ai.prompt.PromptContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service


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

data class MarkdownFile(
    val fileName: String,
)

@ConfigurationProperties("embabel.presentation-maker")
data class PresentationMakerProperties(
    val slideCount: Int = 30,
    // TODO change this
    val outputDirectory: String = "/Users/rjohnson/Documents",
    val outputFile: String = "presentation.md",
    val copyright: String = "Embabel 2025",
    val header: String = """
        ---
        marp: true
        theme: default
        class: invert
        size: 16:9
        style: |
          img {background-color: transparent!important;}
          a:hover, a:active, a:focus {text-decoration: none;}
          header a {color: #ffffff !important; font-size: 30px;}
          footer {color: #148ec8;}
        header: '[&#9671;](#1 " ")'
        footer: "(c) $copyright"
        ---
    """.trimIndent(),
    val coStar: CoStar = CoStar(
        context = "grade 4",
        objective = "teach kids in an engaging way",
        style = "funny and accessible",
        tone = "funny and accessible",
        audience = "grade 4 school kids",
    )
) : PromptContributor by coStar


/**
 * Naming agent that generates names for a company or project.
 */
@Agent(description = "Presentation maker. Build a presentation on a topic")
class PresentationMaker(
    private val properties: PresentationMakerProperties,
    private val slideFormatter: SlideFormatter,
    private val filePersister: FilePersister,
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
        context: OperationContext,
    ): ResearchComplete {
        val researchReports = researchTopics.topics.parallelMap(context) {
            context.promptRunner(toolGroups = setOf(CoreToolGroups.WEB))
                .withPromptContributor(properties)
                .create<ResearchReport>(
                    """
            Given the following topic and the goal to create a presentation
            for this audience, create a research report.
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

    @Action
    fun createDeck(
        userInput: UserInput,
        researchComplete: ResearchComplete,
        context: OperationContext,
    ): Deck {
        val reports = researchComplete.topicResearches.map { it.researchReport }
        return context.promptRunner()
            .withPromptContributor(properties)
            .withToolGroup(CoreToolGroups.WEB)
            .create<Deck>(
                """
                Create content for a slide deck based on a research report.
                Use the following input to guide the presentation:
                ${userInput.content}

                Support your points using the following research:
                Reports:
                $reports

                The presentation should be ${properties.slideCount} slides long.
                It should have a compelling narrative and call to action.
                It should end with a list of reference links.
                Do

                Use Marp format, creating Markdown that can be rendered as slides.
                If you need to look it up, see https://github.com/marp-team/marp/blob/main/website/docs/guide/directives.md

                Use the following header elements to start the deck.
                Add further header elements if you wish.

                ```
                ${properties.header}
                ```
            """.trimIndent()
            )
    }

    @Action
    fun saveDeck(deck: Deck): MarkdownFile {
        filePersister.saveFile(
            directory = properties.outputDirectory,
            fileName = properties.outputFile,
            content = deck.deck,
        )
        return MarkdownFile(
            properties.outputFile
        )
    }

    @AchievesGoal(
        description = "Create a presentation based on research reports",
    )
    @Action
    fun convertToSlides(
        deck: Deck,
        markdownFile: MarkdownFile
    ): Deck {
        slideFormatter.createHtmlSlides(
            directory = properties.outputDirectory,
            markdownFilename = markdownFile.fileName,
        )
        return deck
    }

}


fun interface FilePersister {

    fun saveFile(
        directory: String,
        fileName: String,
        content: String,
    )
}

@Service
class FileToolsFilePersister : FilePersister {

    override fun saveFile(
        directory: String,
        fileName: String,
        content: String
    ) {
        FileTools.readWrite(directory).createFile(path = fileName, content = content, overwrite = true)
    }
}
