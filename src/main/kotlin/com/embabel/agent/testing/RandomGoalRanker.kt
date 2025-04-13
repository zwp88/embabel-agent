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

import com.embabel.agent.core.Goal
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.GoalRanker
import com.embabel.agent.spi.GoalRanking
import com.embabel.agent.spi.GoalRankings
import kotlin.random.Random

/**
 * Identifies goal rankers used for test
 */
fun interface FakeGoalRanker : GoalRanker

class RandomGoalRanker : FakeGoalRanker {
    private val random = Random(System.currentTimeMillis())

    override fun rankGoals(
        userInput: UserInput,
        goals: Set<Goal>,
    ): GoalRankings {
        return GoalRankings(goals.map {
            GoalRanking(
                goal = it,
                confidence = random.nextDouble(),
            )
        })
    }
}
