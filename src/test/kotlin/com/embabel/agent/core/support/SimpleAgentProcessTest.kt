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

import com.embabel.agent.core.PlatformServices
import com.embabel.agent.domain.library.Person
import com.embabel.agent.event.ObjectAddedEvent
import com.embabel.agent.event.ObjectBoundEvent
import com.embabel.agent.spi.support.EventSavingAgenticEventListener
import com.embabel.agent.support.SimpleTestAgent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private data class LocalPerson(
    override val name: String,
) : Person

class SimpleAgentProcessTest {

    @Nested
    inner class Binding {

        @Test
        fun adds() {
            val ese = EventSavingAgenticEventListener()
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns ese
            every { mockPlatformServices.llmTransformer } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                "test", agent = SimpleTestAgent,
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
            every { mockPlatformServices.llmTransformer } returns mockk()
            val blackboard = InMemoryBlackboard()
            val agentProcess = SimpleAgentProcess(
                "test", agent = SimpleTestAgent,
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
            every { mockPlatformServices.llmTransformer } returns mockk()
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
            every { mockPlatformServices.llmTransformer } returns mockk()
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
