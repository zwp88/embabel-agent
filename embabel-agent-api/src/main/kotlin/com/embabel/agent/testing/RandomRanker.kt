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
package com.embabel.agent.testing

import com.embabel.agent.core.Agent
import com.embabel.agent.core.Goal
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.Ranker
import com.embabel.agent.spi.Ranking
import com.embabel.agent.spi.Rankings
import kotlin.random.Random

/**
 * Identifies goal rankers used for test
 */
interface FakeRanker : Ranker

class RandomRanker : FakeRanker {
    private val random = Random(System.currentTimeMillis())

    override fun rankAgents(
        userInput: UserInput,
        agents: Set<Agent>
    ): Rankings<Agent> {
        return Rankings(agents.map {
            Ranking(
                match = it,
                score = random.nextDouble(),
            )
        })
    }

    override fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): Rankings<Goal> {
        return Rankings(goals.map {
            Ranking(
                match = it,
                score = random.nextDouble(),
            )
        })
    }
}
