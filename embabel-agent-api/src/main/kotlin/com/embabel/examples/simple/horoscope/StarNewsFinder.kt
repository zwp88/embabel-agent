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
package com.embabel.examples.simple.horoscope

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.fromForm
import com.embabel.agent.api.annotation.support.using
import com.embabel.agent.api.annotation.support.usingDefaultLlm
import com.embabel.agent.api.common.createObject
import com.embabel.agent.api.common.createObjectIfPossible
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.library.PersonImpl
import com.embabel.agent.domain.library.RelevantNewsStories
import com.embabel.agent.domain.special.UserInput
import com.embabel.ux.form.Text
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import org.springframework.beans.factory.annotation.Value

data class Starry(
    @Text(label = "Star sign")
    val sign: String,
)

data class StarPerson(
    override val name: String,
    val sign: String,
) : Person

data class Horoscope(
    val summary: String,
)

data class Writeup(
    override val text: String,
) : HasContent

/**
 * Find news based on a person's star sign
 */
@Agent(description = "Find news based on a person's star sign")
class StarNewsFinder(
    // Services such as Horoscope are injected using Spring
    private val horoscopeService: HoroscopeService,
    @Value("\${star-news-finder.story.count:5}")
    private val storyCount: Int,
) {

    // TODO should be able to use the Person interface directly?
    @Action
    fun extractPerson(userInput: UserInput): PersonImpl? =
        // All prompts are typesafe
        using(LlmOptions(OpenAiModels.GPT_4o)).createObjectIfPossible(
            """
            Create a person from this user input, extracting their name:
            ${userInput.content}
            """.trimIndent()
        )

    @Action(cost = 100.0) // Make it costly so it won't be used in a plan unless there's no other path
    fun makeStarry(
        person: PersonImpl,
    ): Starry =
        fromForm("Let's get some astrological details for ${person.name}")

    // TODO should work with the Person interface rather than PersonImpl
    @Action
    fun assembleStarPerson(
        person: PersonImpl,
        starry: Starry,
    ): StarPerson {
        return StarPerson(
            name = person.name,
            sign = starry.sign,
        )
    }

    @Action
    fun extractStarPerson(userInput: UserInput): StarPerson? =
        // All prompts are typesafe
        using(LlmOptions(OpenAiModels.GPT_4o)).createObjectIfPossible(
            """
            Create a person from this user input, extracting their name and star sign:
            ${userInput.content}
            """.trimIndent()
        )

    @Action
    fun retrieveHoroscope(starPerson: StarPerson) =
        Horoscope(horoscopeService.dailyHoroscope(starPerson.sign))

    // toolGroups specifies tools that are required for this action to run
    @Action(toolGroups = [ToolGroup.WEB])
    fun findNewsStories(person: StarPerson, horoscope: Horoscope): RelevantNewsStories =
        usingDefaultLlm.createObject(
            """
            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is:
                <horoscope>${horoscope.summary}</horoscope>
            Given this, use web tools and generate search queries
            to find $storyCount relevant news stories summarize them in a few sentences.
            Include the URL for each story.
            Do not look for another horoscope reading or return results directly about astrology;
            find stories relevant to the reading above.

            For example:
            - If the horoscope says that they may
            want to work on relationships, you could find news stories about
            novel gifts
            - If the horoscope says that they may want to work on their career,
            find news stories about training courses.
        """.trimIndent()
        )

    // The @AchievesGoal annotation indicates that completing this action
    // achieves the given goal, so the agent can be complete
    @AchievesGoal(
        description = "Write an amusing writeup for the target person based on their horoscope and current news stories",
    )
    @Action
    fun writeup(
        person: StarPerson,
        relevantNewsStories: RelevantNewsStories,
        horoscope: Horoscope,
    ): Writeup =
        // Customize LLM call
        using(
            LlmOptions(
                ModelSelectionCriteria.firstOf(
                    OpenAiModels.GPT_4o_MINI,
                )
            ).withTemperature(.9)
        ).createObject(
            """
            Take the following news stories and write up something
            amusing for the target person.

            Begin by summarizing their horoscope in a concise, amusing way, then
            talk about the news. End with a surprising signoff.

            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is:
                <horoscope>${horoscope.summary}</horoscope>
            Relevant news stories are:
            ${relevantNewsStories.items.joinToString("\n") { "- ${it.url}: ${it.summary}" }}

            Format it as Markdown with links.
        """.trimIndent()
        )

}
