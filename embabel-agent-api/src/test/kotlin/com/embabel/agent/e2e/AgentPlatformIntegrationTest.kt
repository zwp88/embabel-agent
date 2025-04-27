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

import com.embabel.agent.api.common.*
import com.embabel.agent.api.dsl.EvilWizardAgent
import com.embabel.agent.api.dsl.Frog
import com.embabel.agent.core.*
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.Ranking
import com.embabel.agent.spi.Rankings
import com.embabel.agent.testing.FakeRanker
import com.embabel.common.test.config.FakeAiConfiguration
import com.embabel.examples.simple.horoscope.HoroscopeService
import com.embabel.examples.simple.horoscope.kotlin.Writeup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
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
    fun fakeGoalRanker() = object : FakeRanker {
        override fun rankGoals(
            userInput: UserInput,
            goals: Set<Goal>
        ): Rankings<Goal> {
            val g = goals.find { it.description.contains("horoscope") }!!
            return Rankings(
                rankings = listOf(Ranking(g, .9))
            )
        }

        override fun rankAgents(
            userInput: UserInput,
            agents: Set<Agent>
        ): Rankings<Agent> {
            val a = agents.find { it.name.contains("Star") }!!
            return Rankings(
                rankings = listOf(Ranking(a, .9))
            )
        }
    }

}

/**
 * Integration tests
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(
    value = [
        FakeConfig::class,
        FakeAiConfiguration::class,
    ]
)
class AgentPlatformIntegrationTest(
    @Autowired
    private val agentPlatform: AgentPlatform,
) {

    private val typedOps: TypedOps = AgentPlatformTypedOps(agentPlatform)

    @Nested
    inner class SmokeTest {

        @Test
        fun `AgentPlatform starts up`() {
            // Nothing to test
        }
    }

    @Nested
    inner class Transforms {

        @Test
        fun `run star finder as transform by name`() {
            val writeup = typedOps.asFunction<UserInput, HasContent>(
                processOptions = ProcessOptions(test = true),
                outputClass = HasContent::class.java,
                agentName = com.embabel.examples.simple.horoscope.java.StarNewsFinder::class.qualifiedName!!,
            ).apply(
                UserInput("Lynda is a Scorpio, find some news for her"),
            )
            assertNotNull(writeup)
            assertNotNull(writeup.text)
        }

        @Test
        fun `reject unknown agent in transform by name`() {
            assertThrows<NoSuchAgentException> {
                typedOps.asFunction<UserInput, Writeup>(
                    processOptions = ProcessOptions(test = true),
                    outputClass = Writeup::class.java,
                    agentName = "stuff and nonsense",
                )
            }
        }

        @Test
        fun `run star finder as AgentPlatform transform`() {
            val writeup = typedOps.asFunction<UserInput, com.embabel.examples.simple.horoscope.java.Writeup>(
                processOptions = ProcessOptions(test = true),
                outputClass = com.embabel.examples.simple.horoscope.java.Writeup::class.java,
            ).apply(
                UserInput("Lynda is a Scorpio, find some news for her"),
            )
            assertNotNull(writeup)
            assertNotNull(writeup.text)
        }

        @Test
        fun `run dsl agent as transform`() {
            agentPlatform.deploy(EvilWizardAgent)
            val frog = typedOps.asFunction<UserInput, Frog>(
                processOptions = ProcessOptions(test = true),
            ).apply(
                UserInput("Hamish a poor boy"),
            )
            assertNotNull(frog)
        }
    }

    @Nested
    inner class ClosedRunAgent {

        @Test
        fun `choose and run star finder agent`() {
            val dynamicExecutionResult = agentPlatform.chooseAndRunAgent(
                "Lynda is a Scorpio, find some news for her",
                ProcessOptions(test = true),
            )
            assertNotNull(dynamicExecutionResult.output)
            assertTrue(
                dynamicExecutionResult.output is HasContent,
                "Expected HasContent, got ${dynamicExecutionResult.output.javaClass.name}"
            )

        }
    }

    @Nested
    inner class OpenAccomplishGoal {

        @Test
        fun `run star finder agent`() {
            val dynamicExecutionResult = agentPlatform.chooseAndAccomplishGoal(
                "Lynda is a Scorpio, find some news for her",
                ProcessOptions(test = true),
            )
            assertNotNull(dynamicExecutionResult.output)
            assertTrue(
                dynamicExecutionResult.output is HasContent,
                "Expected HasContent, got ${dynamicExecutionResult.output?.javaClass?.name}"
            )
        }

    }
}
