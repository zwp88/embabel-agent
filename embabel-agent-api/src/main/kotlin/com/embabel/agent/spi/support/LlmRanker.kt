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
package com.embabel.agent.spi.support

import com.embabel.agent.core.Agent
import com.embabel.agent.core.Goal
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.*
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.Described
import com.embabel.common.core.types.Named
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.retry.support.RetryTemplate
import org.springframework.retry.support.RetryTemplateBuilder

/**
 * Use an LLM to rank things
 */
internal class LlmRanker(
    private val llmOperations: LlmOperations,
    private val llm: LlmOptions = LlmOptions(),
    private val maxAttempts: Int = 3,
) : Ranker {

    private val retryTemplate: RetryTemplate =
        RetryTemplateBuilder().maxAttempts(maxAttempts).fixedBackoff(20)
            .build()

    override fun rankAgents(
        userInput: UserInput,
        agents: Set<Agent>
    ): Rankings<Agent> = rankThings(userInput, agents, "agent")

    override fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): Rankings<Goal> = rankThings(userInput, goals, "goal")

    private fun <T> rankThings(
        userInput: UserInput,
        things: Set<T>,
        wordForThing: String,
    ): Rankings<T> where T : Named, T : Described {
        if (things.isEmpty()) {
            return Rankings(emptyList())
        }
        return retryTemplate.execute<Rankings<T>, Exception> {
            rankThingsInternal(
                userInput = userInput,
                things = things,
                wordForThing = wordForThing,
            )
        }
    }

    private fun <T> rankThingsInternal(
        userInput: UserInput,
        things: Set<T>,
        wordForThing: String,
    ): Rankings<T> where T : Named, T : Described {

        val prompt = """
            Given the user input, choose the $wordForThing that best reflects the user's intent.

            User input: ${userInput.content}

            Available ${wordForThing}s:
            ${things.joinToString("\n") { "- ${it.name}: ${it.description}" }}

            Return the name of the chosen $wordForThing and the confidence score (0-1).
            IMPORTANT: The fully qualified name must be exactly the same as in the list.
        """.trimIndent()
        val grr = llmOperations.doTransform(
            prompt = prompt,
            interaction = LlmInteraction(
                id = InteractionId("rank-${wordForThing}s"),
                llm = llm,
            ),
            outputClass = RankingsResponse::class.java,
            llmRequestEvent = null,
        )
        val thingNames = things.map { it.name }
        val bogus = grr.rankings.find { rankingChoice -> thingNames.none { rankingChoice.name == it } }
        if (bogus != null) {
            throw IllegalStateException(
                "Ranker returned choice '$bogus' not in the list of available ${wordForThing}s: ${
                    thingNames
                }, raw=$grr"
            )
            return Rankings(emptyList())
        }

        return Rankings(
            rankings = grr.rankings.map {
                Ranking(
                    match = things.single { thing -> thing.name == it.name },
                    score = it.confidence,
                )
            }.sortedBy { it.score }.reversed()
        )
    }
}

@JsonClassDescription("List of ranked choices")
internal data class RankingsResponse(
    val rankings: List<RankedChoiceResponse>,
)

internal data class RankedChoiceResponse(
    @get:JsonPropertyDescription("name of what we're ranking")
    val name: String,
    @get:JsonPropertyDescription("confidence score from 0-1")
    val confidence: Double,
)
