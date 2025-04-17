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

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.core.Agent
import com.embabel.agent.core.Goal
import com.embabel.agent.core.InteractionId
import com.embabel.agent.core.LlmOperations
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.Ranker
import com.embabel.agent.spi.Ranking
import com.embabel.agent.spi.Rankings
import com.embabel.common.core.types.Described
import com.embabel.common.core.types.Named
import org.springframework.stereotype.Service

/**
 * Use an LLM to rank things
 */
@Service
internal class LlmRanker(
    private val llmOperations: LlmOperations,
) : Ranker {

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

        val prompt = """
            Given the user input, choose the $wordForThing that best reflects the user's intent.

            User input: ${userInput.content}

            Available ${wordForThing}s:
            ${things.joinToString("\n") { "- ${it.name}: ${it.description}" }}

            Return the name of the chosen $wordForThing and the confidence score (0-1).
        """.trimIndent()
        val grr = llmOperations.doTransform<UserInput, RankingsResponse>(
            input = userInput,
            literalPrompt = prompt,
            interactionId = InteractionId("rank-${wordForThing}s"),
            llmOptions = LlmOptions(model = "gpt-4o-mini"),
            outputClass = RankingsResponse::class.java,
        )
        return Rankings(
            rankings = grr.rankings.map {
                Ranking(
                    ranked = things.single { thing -> thing.name == it.name },
                    confidence = it.confidence,
                )
            }.sortedBy { it.confidence }.reversed()
        )
    }
}

internal data class RankingsResponse(
    val rankings: List<RankedChoiceResponse>,
)

internal data class RankedChoiceResponse(
    val name: String,
    val confidence: Double,
)
