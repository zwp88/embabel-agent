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

import com.embabel.agent.api.annotation.support.Person
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.transformer
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.PlatformServices
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.support.BlackboardWorldStateDeterminer
import com.embabel.agent.core.support.InMemoryBlackboard
import com.embabel.agent.domain.special.Aggregation
import com.embabel.agent.domain.special.UserInput
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

interface Fancy

data class FancyPerson(
    val name: String,
) : Fancy

val InterfaceTestAgent = agent("SimpleTest", description = "Simple test agent") {
    action {
        transformer<UserInput, FancyPerson>(name = "thing") {
            FancyPerson(name = "Rod")
        }
    }

    action {
        transformer<AllOfTheAbove, FancyPerson>(name = "reverse-name") {
            FancyPerson(it.input.person.name.reversed())
        }
    }

    goal(name = "done", description = "done", satisfiedBy = FancyPerson::class)
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
                    "it:${UserInput::class.qualifiedName}" to ConditionDetermination.FALSE,
                    "it:${Person::class.qualifiedName}" to ConditionDetermination.FALSE,
                ),
                worldState,
                "World state must use qualified names",
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
                    "it:${UserInput::class.qualifiedName}" to ConditionDetermination.TRUE,
                    "it:${Person::class.qualifiedName}" to ConditionDetermination.FALSE,
                ),
                worldState,
                "World state must use qualified names"
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
                    "it:${UserInput::class.qualifiedName}" to ConditionDetermination.TRUE,
                    "it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE,
                ),
                "World state must use qualified names",
            )
            val action = SimpleTestAgent.actions.single { it.name == "reverse-name" }
            assertTrue(
                action.isAchievable(worldState)
            )
        }
    }

    @Nested
    inner class TypeChecks {

        @Test
        fun `exact type match with simple name`() {
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
            val pc = bsb.determineCondition("it:Person")
            assertEquals(ConditionDetermination.TRUE, pc)
        }

        @Test
        fun `subclass match`() {
            val blackboard = InMemoryBlackboard()
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns InterfaceTestAgent
            val bsb = BlackboardWorldStateDeterminer(
                processContext = ProcessContext(
                    blackboard = blackboard,
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                    processOptions = ProcessOptions(),
                )
            )

            blackboard["input"] = UserInput("xyz")
            blackboard["person"] = FancyPerson("Rod")
            val pc = bsb.determineCondition("it:FancyPerson")
            assertEquals(ConditionDetermination.FALSE, bsb.determineCondition("it:Person"))
            assertEquals(ConditionDetermination.TRUE, pc)
            assertEquals(
                ConditionDetermination.TRUE, bsb.determineCondition("it:Fancy"),
                "Should match against interface",
            )
        }

    }

    @Test
    fun `exact type match with fqn`() {
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
        assertEquals(
            ConditionDetermination.TRUE,
            bsb.determineCondition("it:${Person::class.qualifiedName}"),
            "Should match against fully qualified name",
        )
    }

}
