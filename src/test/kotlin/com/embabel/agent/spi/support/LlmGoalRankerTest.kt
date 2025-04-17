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
import com.embabel.agent.core.LlmOperations
import com.embabel.agent.domain.special.UserInput
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LlmGoalRankerTest {

    @Nested
    inner class Errors {

        @Test
        @Disabled("not yet implemented")
        fun `on llm error`() {
        }

        @Test
        fun `no goals`() {
            val llmt = mockk<LlmOperations>()
            val ranker = LlmRanker(llmt)
            val result = ranker.rankGoals(
                UserInput("whatever"), emptySet(),
            )
            assertTrue(result.rankings.isEmpty())
        }
    }

    @Nested
    inner class HappyPath {
        @Test
        fun `successful choice`() {
            val llmt = mockk<LlmOperations>()
            val llmr = RankingsResponse(
                rankings = listOf(
                    RankedChoiceResponse("horoscope", .2),
                    RankedChoiceResponse("weather", .8),
                )
            )
            every {
                llmt.doTransform<UserInput, RankingsResponse>(
                    input = any(),
                    literalPrompt = any(),
                    interactionId = any(),
                    llmOptions = any(),
                    outputClass = RankingsResponse::class.java,
                )
            } returns llmr
            val ranker = LlmRanker(llmt)
            val rankings = ranker.rankGoals(
                goals = setOf(
                    Goal(
                        name = "horoscope",
                        description = "Get a horoscope",
                    ),
                    Goal(
                        name = "weather",
                        description = "Get the weather",
                    ),
                ), userInput = UserInput("What is my horoscope for today?")
            )
            assertEquals("weather", rankings.rankings[0].ranked.name)
        }
    }

}
