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
package com.embabel.examples.dogfood.research

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.InternetResource
import com.embabel.agent.domain.library.InternetResources
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.experimental.prompt.Persona
import com.embabel.agent.experimental.prompt.PromptUtils
import com.embabel.agent.experimental.prompt.ResponseFormat
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelProvider.Companion.CHEAPEST_ROLE
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byRole
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.ai.prompt.PromptContributorConsumer
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.Timestamped
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Instant

@JsonClassDescription("Research report, containing a text field and links")
data class ResearchReport(
    @get:JsonPropertyDescription(
        "The text of the research report",
    )
    override val text: String,
    override val links: List<InternetResource>,
) : HasContent, InternetResources, HasInfoString {

    override fun infoString(verbose: Boolean?): String {
        return """
            Report:
            $text
            Links: ${links.joinToString("\n") { it.url }}
        """.trimIndent()
    }
}

data class SingleLlmReport(
    val report: ResearchReport,
    val model: String,
) : Timestamped {
    override val timestamp: Instant = Instant.now()
}

data class Critique(
    val accepted: Boolean,
    val reasoning: String,
)


@ConfigurationProperties(prefix = "embabel.examples.researcher")
data class ResearcherProperties(
    val responseFormat: ResponseFormat = ResponseFormat.MARKDOWN,
    val maxWordCount: Int = 250,
    override val name: String = "Sherlock",
    override val persona: String = "A resourceful researcher agent that can perform deep web research on a topic. Nothing escapes Sherlock",
    override val voice: String = "Your voice is dry and in the style of Sherlock Holmes. Occasionally you address the user as Watson",
    override val objective: String = "To clarify all points the user has brought up",
) : Persona, PromptContributorConsumer {
    override val promptContributors: List<PromptContributor>
        get() = listOf(
            responseFormat,
            this,
        )

}

enum class Category {
    QUESTION,
    DISCUSSION,
}

data class Categorization(
    val category: Category,
)

/**
 * Researcher agent that can be used independently or as a subflow
 */
@Agent(
    description = "Perform deep web research on a topic",
    toolGroups = [ToolGroup.WEB, ToolGroup.BROWSER_AUTOMATION]
)
class Researcher(
    val properties: ResearcherProperties,
) {

    @Action
    fun categorize(
        userInput: UserInput,
    ): Categorization = using(
        llm = LlmOptions(byRole(CHEAPEST_ROLE)),
    ).create(
        """
        Categorize the following user input:

        Topic:
        <${userInput.content}>
    """.trimIndent()
    )

    // These need a different output binding or only one will run
    @Action(
        post = [REPORT_SATISFACTORY],
        canRerun = true,
        outputBinding = "gpt4Report"
    )
    fun researchWithGpt4(
        userInput: UserInput,
        categorization: Categorization,
        context: OperationContext,
    ): SingleLlmReport = researchWith(
        userInput = userInput,
        categorization = categorization,
        critique = null,
        llm = LlmOptions(OpenAiModels.GPT_4o),
        context = context,
    )

    @Action(
        pre = [REPORT_UNSATISFACTORY],
        post = [REPORT_SATISFACTORY],
        canRerun = true,
        outputBinding = "gpt4Report"
    )
    fun redoResearchWithGpt4(
        userInput: UserInput,
        categorization: Categorization,
        critique: Critique,
        context: OperationContext,
    ): SingleLlmReport = researchWith(
        userInput = userInput,
        categorization = categorization,
        critique = critique,
        llm = LlmOptions(OpenAiModels.GPT_4o),
        context = context,
    )

    @Action(
        post = [REPORT_SATISFACTORY],
        outputBinding = "claudeReport",
        canRerun = true,
    )
    fun researchWithClaude(
        userInput: UserInput,
        categorization: Categorization,
        context: OperationContext,
    ): SingleLlmReport = researchWith(
        userInput = userInput,
        categorization = categorization,
        critique = null,
        llm = LlmOptions(AnthropicModels.CLAUDE_37_SONNET),
        context = context,
    )

    @Action(
        pre = [REPORT_UNSATISFACTORY],
        post = [REPORT_SATISFACTORY],
        outputBinding = "claudeReport",
        canRerun = true,
    )
    fun redoResearchWithClaude(
        userInput: UserInput,
        categorization: Categorization,
        critique: Critique,
        context: OperationContext,
    ): SingleLlmReport = researchWith(
        userInput = userInput,
        categorization = categorization,
        critique = critique,
        llm = LlmOptions(AnthropicModels.CLAUDE_37_SONNET),
        context = context,
    )

    private fun researchWith(
        userInput: UserInput,
        categorization: Categorization,
        critique: Critique?,
        llm: LlmOptions,
        context: OperationContext,
    ): SingleLlmReport {
        val researchReport = when (
            categorization.category
        ) {
            Category.QUESTION -> answerQuestion(userInput, llm, critique, context)
            Category.DISCUSSION -> research(userInput, llm, critique, context)
        }
        return SingleLlmReport(
            report = researchReport,
            model = llm.criteria.toString(),
        )
    }

    private fun answerQuestion(
        userInput: UserInput,
        llm: LlmOptions,
        critique: Critique?,
        context: OperationContext,
    ): ResearchReport = context.promptRunner(
        llm = llm,
        promptContributors = properties.promptContributors,
    ).create(
        """
        Use the web and browser tools to answer the given question.

        You must try to find the answer on the web, and be definite, not vague.

        Write a detailed report in at most ${properties.maxWordCount} words.
        If you can answer the question more briefly, do so.
        Including a number of links that are relevant to the topic.

        Example:
        ${PromptUtils.jsonExampleOf<ResearchReport>()}

        Question:
        <${userInput.content}>
        
        ${
            critique?.reasoning?.let {
                "Critique of previous answer:\n<$it>"
            }
        }
    """.trimIndent()
    )

    private fun research(
        userInput: UserInput,
        llm: LlmOptions,
        critique: Critique?,
        context: OperationContext,
    ): ResearchReport = context.promptRunner(
        llm = llm,
        promptContributors = properties.promptContributors,
    ).create(
        """
        Use the web and browser tools to perform deep research on the given topic.

        Write a detailed report in ${properties.maxWordCount} words,
        including a number of links that are relevant to the topic.

        Topic:
        <${userInput.content}>
        
         ${
            critique?.reasoning?.let {
                "Critique of previous answer:\n<$it>"
            }
        }
    """.trimIndent()
    )

    @Action(post = [REPORT_SATISFACTORY], canRerun = true)
    fun critiqueMergedReport(
        userInput: UserInput,
        @RequireNameMatch mergedReport: ResearchReport,
    ): Critique = using(LlmOptions(OpenAiModels.GPT_4o)).create(
        """
            Is this research report satisfactory? Consider the following question:
            <${userInput.content}>
            The report is satisfactory if it answers the question with adequate references.
            It is possible that the question does not have a clear answer, in which
            case the report is satisfactory if it provides a reasonable discussion of the topic.
            
            ${mergedReport.infoString(verbose = true)}
        """.trimIndent(),
    )

    @Action(
        post = [REPORT_SATISFACTORY],
        outputBinding = "mergedReport",
        canRerun = true,
    )
    fun mergeReports(
        userInput: UserInput,
        @RequireNameMatch gpt4Report: SingleLlmReport,
        @RequireNameMatch claudeReport: SingleLlmReport,
    ): ResearchReport {
        val reports = listOf(
            gpt4Report,
            claudeReport,
        )
        return using(
            llm = LlmOptions(OpenAiModels.GPT_4o),
            promptContributors = properties.promptContributors,
        ).create(
            """
        Merge the following research reports into a single report taking the best of each.
        Consider the user direction: <${userInput.content}>

        ${reports.joinToString("\n\n") { "Report from ${it.model}\n${it.report.infoString(verbose = true)}" }}
    """.trimIndent()
        )
    }

    @Condition(name = REPORT_SATISFACTORY)
    fun makesTheGrade(
        critique: Critique,
    ): Boolean = critique.accepted

    // TODO should be able to use !
    @Condition(name = REPORT_UNSATISFACTORY)
    fun rejected(
        critique: Critique,
    ): Boolean = !critique.accepted

    @AchievesGoal(
        description = "Accepts a research report",
    )
    // TODO this won't complete without the output binding to a new thing.
    // This makes some sense but seems a bit surprising
    @Action(pre = [REPORT_SATISFACTORY], outputBinding = "finalResearchReport")
    fun acceptReport(
        @RequireNameMatch mergedReport: ResearchReport,
        critique: Critique,
    ) = mergedReport

    companion object {

        const val REPORT_SATISFACTORY = "reportSatisfactory"
        const val REPORT_UNSATISFACTORY = "reportUnsatisfactory"
    }
}
