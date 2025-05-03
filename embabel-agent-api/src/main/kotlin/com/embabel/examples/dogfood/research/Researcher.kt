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
import com.embabel.agent.api.common.create
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.InternetResource
import com.embabel.agent.domain.library.InternetResources
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.experimental.prompt.Persona
import com.embabel.agent.experimental.prompt.ResponseFormat
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelProvider.Companion.BEST_ROLE
import com.embabel.common.ai.model.ModelProvider.Companion.CHEAPEST_ROLE
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byRole
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.ai.prompt.PromptContributorConsumer
import org.springframework.boot.context.properties.ConfigurationProperties

data class ResearchReport(
    override val text: String,
    override val links: List<InternetResource>,
) : HasContent, InternetResources

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

    @Action(post = [MAKES_THE_GRADE])
    fun doResearch(
        userInput: UserInput,
        categorization: Categorization,
    ): ResearchReport = when (
        categorization.category
    ) {
        Category.QUESTION -> answerQuestion(userInput, categorization)
        Category.DISCUSSION -> research(userInput, categorization)
    }

    fun answerQuestion(
        userInput: UserInput,
        categorization: Categorization,
    ): ResearchReport = using(
        llm = LlmOptions(byRole(BEST_ROLE)),
        promptContributors = properties.promptContributors,
    ).create(
        """
        Use the web and browser tools to answer the given question.
        
        You must try to find the answer on the web, and be definite, not vague.
        
        Write a detailed report in at most ${properties.maxWordCount} words.
        If you can answer the question more briefly, do so.
        Including a number of links that are relevant to the topic.
        
        Question:
        <${userInput.content}>
    """.trimIndent()
    )

    fun research(
        userInput: UserInput,
        categorization: Categorization,
    ): ResearchReport = using(
        llm = LlmOptions(byRole(BEST_ROLE)),
        promptContributors = properties.promptContributors,
    ).create(
        """
        Use the web and browser tools to perform deep research on the given topic.
        
        Write a detailed report in ${properties.maxWordCount} words,
        including a number of links that are relevant to the topic.
        
        Topic:
        <${userInput.content}>
    """.trimIndent()
    )

    @Condition(name = MAKES_THE_GRADE)
    fun makesTheGrade(
        userInput: UserInput,
        report: ResearchReport,
    ): Boolean = report.text.isNotBlank() && report.links.isNotEmpty()

    @AchievesGoal(
        description = "Accepts a research report",
    )
    @Action(pre = [MAKES_THE_GRADE])
    fun acceptReport(
        userInput: UserInput,
        report: ResearchReport,
    ) = report

    companion object {

        const val MAKES_THE_GRADE = "makesTheGrade"
    }
}
