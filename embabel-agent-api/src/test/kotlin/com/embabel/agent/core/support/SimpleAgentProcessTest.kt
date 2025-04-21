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
package com.embabel.agent.core.support

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.confirm
import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.api.annotation.waitFor
import com.embabel.agent.api.dsl.Frog
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.transformer
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.hitl.ConfirmationRequest
import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.event.ObjectAddedEvent
import com.embabel.agent.event.ObjectBoundEvent
import com.embabel.agent.event.logging.personality.severance.SeveranceLoggingAgenticEventListener
import com.embabel.agent.spi.PlatformServices
import com.embabel.agent.spi.support.EventSavingAgenticEventListener
import com.embabel.agent.support.SimpleTestAgent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

data class LocalPerson(
    override val name: String,
) : Person

@com.embabel.agent.api.annotation.Agent(description = "waiting agent")
class AnnotationWaitingAgent {

    @Action
    fun fromInput(input: UserInput): LocalPerson {
        return confirm(
            LocalPerson(name = input.content),
            "Is this the right dude?"
        )
    }

    @Action
    @AchievesGoal(description = "the big goal in the sky")
    fun toFrog(person: LocalPerson): Frog {
        return Frog(person.name)
    }
}

val DslWaitingAgent = agent("Waiter", description = "Simple test agent that waits") {
    action {
        transformer<UserInput, LocalPerson>(name = "thing") {
            val person = LocalPerson(name = "Rod")
            waitFor(
                ConfirmationRequest(
                    person,
                    "Is this the dude?"
                )
            )
        }
    }

    action {
        transformer<LocalPerson, Frog>(name = "thing2") {
            Frog(name = it.input.name)
        }
    }

    goal(name = "done", description = "done", satisfiedBy = Frog::class)
}

class SimpleAgentProcessTest {

    @Nested
    inner class Waiting {

        @Test
        fun `wait on tick for DSL agent`() {
            waitOnTick(DslWaitingAgent)
        }

        @Test
        fun `wait on run for DSL agent`() {
            waitOnRun(DslWaitingAgent)
        }

        @Test
        fun `wait on tick for annotation agent`() {
            waitOnTick(AgentMetadataReader().createAgentMetadata(AnnotationWaitingAgent()) as Agent)
        }

        @Test
        fun `wait on run for annotation agent`() {
            waitOnRun(AgentMetadataReader().createAgentMetadata(AnnotationWaitingAgent()) as Agent)
        }

        private fun waitOnTick(agent: Agent) {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            blackboard += ("it" to UserInput("Rod"))
            val agentProcess = SimpleAgentProcess(
                id = "test",
                agent = agent,
                processOptions = ProcessOptions(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val agentStatus = agentProcess.tick()
            assertEquals(AgentProcessStatusCode.WAITING, agentStatus.status)
            val confirmation = blackboard.finalResult()
            assertTrue(confirmation is ConfirmationRequest<*>)
        }

        private fun waitOnRun(agent: Agent) {
            val ese: AgenticEventListener = SeveranceLoggingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            blackboard += ("it" to UserInput("Rod"))
            val agentProcess = SimpleAgentProcess(
                id = "test",
                agent = agent,
                processOptions = ProcessOptions(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val agentStatus = agentProcess.run()
            assertEquals(AgentProcessStatusCode.WAITING, agentStatus.status)
        }
    }

    @Nested
    inner class Binding {

        @Test
        fun adds() {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                id = "test",
                agent = SimpleTestAgent,
                processOptions = mockk(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val person = LocalPerson("John")
            agentProcess += person
            assertTrue(blackboard.objects.contains(person))
        }

        @Test
        fun `emits add event`() {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                id = "test",
                agent = SimpleTestAgent,
                processOptions = mockk(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val person = LocalPerson("John")
            agentProcess += person
            val e = ese.processEvents.filterIsInstance<ObjectAddedEvent>().single()
            assertEquals(person, e.value)
        }

        @Test
        fun binds() {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                "test", agent = SimpleTestAgent,
                processOptions = mockk(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val person = LocalPerson("John")
            agentProcess += ("john" to person)
            assertTrue(blackboard.objects.contains(person))
            assertEquals(person, blackboard["john"])
        }

        @Test
        fun `emits binding event`() {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmOperations } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                "test", agent = SimpleTestAgent,
                processOptions = mockk(),
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                parentId = null,
            )
            val person = LocalPerson("John")
            agentProcess += ("john" to person)
            assertTrue(blackboard.objects.contains(person))
            assertEquals(person, blackboard["john"])
            assertEquals(1, ese.processEvents.size, "Should have 1 event")
            val e = ese.processEvents.filterIsInstance<ObjectBoundEvent>().single()
            assertEquals(person, e.value)
            assertEquals("john", e.name)
        }
    }

}
