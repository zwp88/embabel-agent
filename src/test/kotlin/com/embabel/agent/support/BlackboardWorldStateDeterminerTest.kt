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
package com.embabel.agent.support

import com.embabel.agent.*
import com.embabel.agent.annotation.support.Person
import com.embabel.agent.dsl.agent
import com.embabel.agent.dsl.transformer
import com.embabel.plan.goap.ConditionDetermination
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

data class AllOfTheAbove(
    val userInput: UserInput,
    val person: Person,
) : Aggregation

val SimpleTestAgent = agent("SimpleTest", description = "Simple test agent") {
    action {
        transformer<UserInput, Person>(name = "thing") {
            Person(name = "Rod")
        }
    }

    action {
        transformer<AllOfTheAbove, Person>(name = "reverse-name") {
            Person(it.input.person.name.reversed())
        }
    }

    goal(name = "done", description = "done", satisfiedBy = Person::class)
}

class BlackboardWorldStateDeterminerTest {

    val mockPlatformServices = mockk<PlatformServices>()

    init {
        every { mockPlatformServices.llmTransformer } returns mockk()
    }

    @Nested
    inner class Worlds {

        @Test
        fun `negative world`() {
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns SimpleTestAgent
            assertEquals(2, SimpleTestAgent.actions.size)
            val bsb = BlackboardWorldStateDeterminer(
                processContext = ProcessContext(
                    blackboard = InMemoryBlackboard(),
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                    processOptions = ProcessOptions()
                )
            )
            val worldState = bsb.determineWorldState().state
            assertEquals(
                mapOf(
                    "it:UserInput" to ConditionDetermination.FALSE,
                    "it:Person" to ConditionDetermination.FALSE,
                ),
                worldState
            )

        }

        @Test
        fun `one element world`() {
            val blackboard = InMemoryBlackboard()

            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns SimpleTestAgent
            val bsb = BlackboardWorldStateDeterminer(
                processContext = ProcessContext(
                    blackboard = blackboard,
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                    processOptions = ProcessOptions()

                )
            )
            blackboard["it"] = UserInput("xyz")
            val worldState = bsb.determineWorldState().state
            assertEquals(
                mapOf(
                    "it:UserInput" to ConditionDetermination.TRUE,
                    "it:Person" to ConditionDetermination.FALSE,
                ),
                worldState
            )
        }

        @Test
        fun `activated megazord`() {
            val blackboard = InMemoryBlackboard()

            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns SimpleTestAgent
            val bsb = BlackboardWorldStateDeterminer(
                processContext = ProcessContext(
                    blackboard = blackboard,
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                    processOptions = ProcessOptions(),
                )
            )
            blackboard["input"] = UserInput("xyz")
            blackboard["person"] = Person("Rod")
            val worldState = bsb.determineWorldState()
            assertEquals(
                worldState.state,
                mapOf(
                    "it:UserInput" to ConditionDetermination.TRUE,
                    "it:Person" to ConditionDetermination.TRUE,
                ),
            )
            val action = SimpleTestAgent.actions.single { it.name == "reverse-name" }
            assertTrue(
                action.isAchievable(worldState)
            )
        }
    }

}
