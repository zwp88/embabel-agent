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
package com.embabel.agent.e2e

import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentPlatformTypedOps
import com.embabel.agent.core.GoalResult
import com.embabel.agent.core.NoSuchAgentException
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.TypedOps
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.GoalRanking
import com.embabel.agent.spi.GoalRankings
import com.embabel.agent.testing.FakeGoalRanker
import com.embabel.common.ai.model.*
import com.embabel.examples.simple.horoscope.FunnyWriteup
import com.embabel.examples.simple.horoscope.HoroscopeService
import com.embabel.examples.simple.horoscope.StarNewsFinder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import kotlin.test.assertNotNull

@TestConfiguration
class FakeConfig {

    @Bean
    @Primary
    fun fakeHoroscopeService() = HoroscopeService {
        """"
            |On Monday, try to avoid being eaten by wolves            .
        """.trimMargin()
    }

    @Bean
    @Primary
    fun fakeGoalRanker() = FakeGoalRanker { ui, goals ->
        val g = goals.find { it.description.contains("horoscope") }!!
        GoalRankings(
            rankings = listOf(GoalRanking(g, .9))
        )
    }

}

/**
 * Integration tests
 */
@SpringBootTest
@Import(FakeConfig::class)
class AgentPlatformIntegrationTest(
    @Autowired
    private val agentPlatform: AgentPlatform,
) {

    private val typedOps: TypedOps = AgentPlatformTypedOps(agentPlatform)

    @Test
    fun `agent starts up`() {
        // Nothing to test
    }

    @Test
    fun `run star finder as transform by name`() {
        val funnyWriteup = typedOps.asFunction<UserInput, FunnyWriteup>(
            processOptions = ProcessOptions(test = true),
            outputClass = FunnyWriteup::class.java,
            agentName = StarNewsFinder::class.qualifiedName!!,
        ).apply(
            UserInput("Lynda is a Scorpio, find some news for her"),
        )
        assertNotNull(funnyWriteup)
        assertNotNull(funnyWriteup.text)
    }

    @Test
    fun `reject unknown agent in transform by name`() {
        assertThrows<NoSuchAgentException> {
            typedOps.asFunction<UserInput, FunnyWriteup>(
                processOptions = ProcessOptions(test = true),
                outputClass = FunnyWriteup::class.java,
                agentName = "stuff and nonsense",
            )
        }
    }

    @Test
    fun `run star finder as AgentPlatform transform`() {
        val funnyWriteup = typedOps.asFunction<UserInput, FunnyWriteup>(
            processOptions = ProcessOptions(test = true),
            outputClass = FunnyWriteup::class.java,
        ).apply(
            UserInput("Lynda is a Scorpio, find some news for her"),
        )
        assertNotNull(funnyWriteup)
        assertNotNull(funnyWriteup.text)
    }

    @Test
    fun `run star finder agent`() {
        val goalResult = agentPlatform.chooseAndAccomplishGoal(
            "Lynda is a Scorpio, find some news for her",
            ProcessOptions(test = true),
        )
        when (goalResult) {
            is GoalResult.Success -> {
                assertNotNull(goalResult.output)
                assertTrue(
                    goalResult.output is FunnyWriteup,
                    "Expected FunnyWriteup, got ${goalResult.output?.javaClass?.name}"
                )
            }

            is GoalResult.NoGoalFound -> {
                fail("Goal not found: $goalResult")
            }
        }
    }
}
