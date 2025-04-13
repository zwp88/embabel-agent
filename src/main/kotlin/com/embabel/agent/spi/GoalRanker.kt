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
package com.embabel.agent.spi

import com.embabel.agent.Goal
import com.embabel.agent.domain.special.UserInput
import com.embabel.common.core.types.HasInfoString

/**
 * Rank available goals based on user input and agent metadata.
 * It's possible that no ranking will be high enough to progress with,
 * but that's a matter for the AgentPlatform using this service.
 */
fun interface GoalRanker {

    fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): GoalRankings
}

data class GoalRankings(
    val rankings: List<GoalRanking>
) : HasInfoString {

    override fun infoString(verbose: Boolean?): String =
        rankings.joinToString("\n") { ranking ->
            "${ranking.goal.name}: ${ranking.confidence}"
        }
}

/**
 * Goal choice returned by the goal chooser
 * @param goal The goal chosen by the goal chooser
 * @param confidence The confidence score of the goal choice, between 0 and 1
 */
data class GoalRanking(
    val goal: Goal,
    val confidence: Double,
)
