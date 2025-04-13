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

import com.embabel.agent.core.Goal
import com.embabel.agent.core.LlmTransformer
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.core.primitive.LlmOptions
import com.embabel.agent.spi.GoalRanker
import com.embabel.agent.spi.GoalRanking
import com.embabel.agent.spi.GoalRankings
import org.springframework.stereotype.Service

@Service
class LlmGoalRanker(
    private val llmTransformer: LlmTransformer,
) : GoalRanker {

    override fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): GoalRankings {

        if (goals.isEmpty()) {
            return GoalRankings(emptyList())
        }

        val prompt = """
            Given the user input, choose the goal that best reflects the user's intent.

            User input: ${userInput.content}

            Available goals:
            ${goals.joinToString("\n") { "- ${it.name}: ${it.description}" }}

            Return the name of the chosen goal and the confidence score (0-1).
        """.trimIndent()
        val grr = llmTransformer.doTransform<UserInput, GoalRankingsResponse>(
            input = userInput,
            literalPrompt = prompt,
            llmOptions = LlmOptions.Companion(model = "gpt-4o-mini"),
            outputClass = GoalRankingsResponse::class.java,
        )
        return GoalRankings(
            rankings = grr.rankings.map {
                GoalRanking(
                    goal = goals.single { goal -> goal.name == it.name },
                    confidence = it.confidence,
                )
            }.sortedBy { it.confidence }.reversed()
        )
    }
}

internal data class GoalRankingsResponse(
    val rankings: List<GoalChoiceResponse>,
)

internal data class GoalChoiceResponse(
    val name: String,
    val confidence: Double,
)
