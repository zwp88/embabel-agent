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

import com.embabel.agent.core.Agent
import com.embabel.agent.core.Goal
import com.embabel.agent.domain.special.UserInput
import com.embabel.common.core.types.Described
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.Named

/**
 * Rank available choices based on user input and agent metadata.
 * It's possible that no ranking will be high enough to progress with,
 * but that's a matter for the AgentPlatform using this service.
 */
interface Ranker {

    fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): Rankings<Goal>

    fun rankAgents(
        userInput: UserInput,
        agents: Set<Agent>,
    ): Rankings<Agent>
}

data class Rankings<T>(
    val rankings: List<Ranking<T>>
) : HasInfoString where T : Named, T : Described {

    override fun infoString(verbose: Boolean?): String =
        rankings.joinToString("\n") { ranking ->
            "${ranking.ranked.name}: ${ranking.confidence}"
        }
}

/**
 * Ranking choice returned by the ranker
 * @param ranked The ranked item
 * @param confidence The confidence score of the ranker in this choice,
 * between 0 and 1
 */
data class Ranking<T>(
    val ranked: T,
    val confidence: Double,
) where T : Named, T : Described
