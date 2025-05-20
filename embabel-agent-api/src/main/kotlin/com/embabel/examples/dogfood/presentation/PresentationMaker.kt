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
import com.embabel.agent.api.annotation.usingModel
import com.embabel.agent.api.common.DynamicExecutionResult
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.dsl.parallelMap
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.agent.domain.library.CompletedResearch
import com.embabel.agent.domain.library.ResearchReport
import com.embabel.agent.domain.library.ResearchResult
import com.embabel.agent.domain.library.ResearchTopics
import com.embabel.agent.event.logging.personality.severance.LumonColorPalette
import com.embabel.agent.experimental.prompt.CoStar
import com.embabel.agent.shell.formatProcessOutput
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byName
import com.embabel.common.ai.prompt.PromptContributor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ResourceLoader
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.nio.charset.Charset

data class MarkdownFile(
    val fileName: String,
)

/**
 * @param brief the content of the presentation. Can be short
 * or detailed
 *
 */
data class PresentationRequest(
    val slideCount: Int,
    val brief: String,
    val softwareProject: String? = "/Users/rjohnson/dev/embabel.com/embabel-agent/embabel-agent-api",
    val outputDirectory: String = "/Users/rjohnson/Documents",
    val outputFile: String = "presentation.md",
    val copyright: String = "Embabel 2025",
    val header: String = """
        ---
        marp: true
        theme: default
        paginate: false
        class: invert
        size: 16:9
        style: |
          img {background-color: transparent!important;}
          a:hover, a:active, a:focus {text-decoration: none;}
          header a {color: #ffffff !important; font-size: 30px;}
          footer {color: #148ec8;}
        footer: "(c) $copyright"
        ---
    """.trimIndent(),
    //val slidesToInclude: String,
    val coStar: CoStar,
) : PromptContributor by coStar {

    val project: Project? =
        softwareProject?.let {
            Project(it)
        }
}

@ConfigurationProperties(prefix = "embabel.presentation-maker")
data class PresentationMakerProperties(
    val researchLlm: String = OpenAiModels.GPT_41_MINI,
    val creationLlm: String = AnthropicModels.CLAUDE_37_SONNET,
)


/**
 * Naming agent that generates names for a company or project.
 */
@Agent(description = "Presentation maker. Build a presentation on a topic")
class PresentationMaker(
    private val slideFormatter: SlideFormatter,
    private val filePersister: FilePersister,
    private val properties: PresentationMakerProperties,
) {

    @Action
    fun identifyResearchTopics(presentationRequest: PresentationRequest): ResearchTopics =
        usingModel(
            properties.researchLlm,
            toolGroups = setOf(CoreToolGroups.WEB),
        ).create(
            """
                Create a list of research topics for a presentation,
                based on the given input:
                ${presentationRequest.brief}
                """.trimIndent()
        )

    @Action
    fun researchTopics(
        researchTopics: ResearchTopics,
        presentationRequest: PresentationRequest,
        context: OperationContext,
    ): ResearchResult {
        val researchReports = researchTopics.topics.parallelMap(context) {
            context.promptRunner(
                llm = LlmOptions.fromModel(properties.researchLlm),
                toolGroups = setOf(CoreToolGroups.WEB),
            )
                .withToolObject(presentationRequest.project)
                .withPromptContributor(presentationRequest)
                .create<ResearchReport>(
                    """
            Given the following topic and the goal to create a presentation
            for this audience, create a research report.
            Use web tools to research and the findPatternInProject tool to look
            within the given software project.
            Always look for code examples in the project before using the web.
            Topic: ${it.topic}
            Questions:
            ${it.questions.joinToString("\n")}
                """.trimIndent()
                )
        }
        return ResearchResult(
            topicResearches = researchTopics.topics.mapIndexed { index, topic ->
                CompletedResearch(
                    topic = topic,
                    researchReport = researchReports[index],
                )
            }
        )
    }

    @Action
    fun createDeck(
        presentationRequest: PresentationRequest,
        researchComplete: ResearchResult,
        context: OperationContext,
    ): SlideDeck {
        val reports = researchComplete.topicResearches.map { it.researchReport }
        val slideDeck = context.promptRunner(llm = LlmOptions(byName(properties.creationLlm)))
            .withPromptContributor(presentationRequest)
            .withToolGroup(CoreToolGroups.WEB)
            .withToolObject(presentationRequest.project)
            .create<SlideDeck>(
                """
                Create content for a slide deck based on the given research.
                Use the following input to guide the presentation:
                ${presentationRequest.brief}

                Support your points using the following research:
                Reports:
                $reports

                The presentation should be ${presentationRequest.slideCount} slides long.
                It should have a compelling narrative and call to action.
                It should end with a list of reference links.
                Use the findPatternInProject tool to find relevant content within the given software project
                if required and format code on slides.

                Use Marp format, creating Markdown that can be rendered as slides.
                If you need to look it up, see https://github.com/marp-team/marp/blob/main/website/docs/guide/directives.md

                Use the following header elements to start the deck.
                Add further header elements if you wish.

                ```
                ${presentationRequest.header}
                ```
            """.trimIndent()
            )
        filePersister.saveFile(
            directory = presentationRequest.outputDirectory,
            fileName = "01_" + presentationRequest.outputFile,
            content = slideDeck.deck,
        )
        return slideDeck
    }

    @Action
    fun expandDigraphs(
        slideDeck: SlideDeck,
        presentationRequest: PresentationRequest,
        context: OperationContext,
    ): MarkdownFile {
        val diagramExpander = DotCliDigraphExpander(
            directory = presentationRequest.outputDirectory,
        )
        val withDotDiagrams = slideDeck.expandDotDiagrams(diagramExpander)
        filePersister.saveFile(
            directory = presentationRequest.outputDirectory,
            fileName = "02_" + presentationRequest.outputFile,
            content = withDotDiagrams.deck,
        )
        return MarkdownFile(
            presentationRequest.outputFile
        )
    }

    @AchievesGoal(
        description = "Create a presentation based on research reports",
    )
    @Action
    fun convertToSlides(
        presentationRequest: PresentationRequest,
        slideDeck: SlideDeck,
        markdownFile: MarkdownFile
    ): SlideDeck {
        slideFormatter.createHtmlSlides(
            directory = presentationRequest.outputDirectory,
            markdownFilename = markdownFile.fileName,
        )
        return slideDeck
    }

}

class Project(override val root: String) : SymbolSearch

@ShellComponent("Presentation maker commands")
class PresentationMakerShell(
    private val agentPlatform: AgentPlatform,
    private val resourceLoader: ResourceLoader,
    private val objectMapper: ObjectMapper,
) {
    @ShellMethod
    fun makePresentation(
        @ShellOption(
            defaultValue = "file:/Users/rjohnson/dev/embabel.com/embabel-agent/embabel-agent-api/src/main/kotlin/com/embabel/examples/dogfood/presentation/kotlinconf.yml",
        )
        file: String,
    ): String {
        val yamlReader = ObjectMapper(YAMLFactory()).registerKotlinModule()


        val presentationRequest = yamlReader.readValue(
            resourceLoader.getResource(file).getContentAsString(Charset.defaultCharset()),
            PresentationRequest::class.java,
        )

        val agentProcess = agentPlatform.runAgentWithInput(
            agent = agentPlatform.agents().single { it.name == "PresentationMaker" },
            input = presentationRequest,
            processOptions = ProcessOptions(verbosity = Verbosity(showPrompts = true)),
        )

        return formatProcessOutput(
            result = DynamicExecutionResult.fromProcessStatus(basis = presentationRequest, agentProcess = agentProcess),
            colorPalette = LumonColorPalette,
            objectMapper = objectMapper,
        ) + "\ndeck is at ${presentationRequest.outputDirectory}/${presentationRequest.outputFile}"
    }
}
