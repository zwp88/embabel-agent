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
package com.embabel.agent.annotation.support

import com.embabel.agent.annotation.Agent
import com.embabel.agent.core.*
import com.embabel.agent.core.support.InMemoryBlackboard
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.AgenticEventListener.Companion.DevNull
import com.embabel.agent.core.primitive.LlmOptions
import com.embabel.plan.goap.ConditionDetermination
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallback
import com.embabel.agent.core.Agent as IAgent


class AgentMetadataReaderTest {

    @Nested
    inner class Errors {

        @Test
        fun `no annotation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(Person("John Doe"))
            assertNull(metadata)
        }

        @Test
        fun `no methods`() {
            val reader = AgentMetadataReader()
            assertNull(reader.createAgentMetadata(NoMethods()))
        }

        @Test
        @Disabled
        fun invalidConditionSignature() {
        }

        @Test
        @Disabled
        fun invalidActionSignature() {
        }

    }

    @Nested
    inner class Goals {

        @Test
        fun `one goal only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneGoalOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.goals.size)
        }

        @Test
        fun `two goals only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(TwoGoalsOnly())
            assertNotNull(metadata)
            assertEquals(2, metadata!!.goals.size)
            val expectedThing1GoalName = "${TwoGoalsOnly::class.java.name}.thing1"
            val expectedThing2GoalName = "${TwoGoalsOnly::class.java.name}.thing2"
            val t1 = metadata.goals.find { it.name == expectedThing1GoalName }
            val t2 = metadata.goals.find { it.name == expectedThing2GoalName }
            assertNotNull(t1, "Should have $expectedThing1GoalName goal: " + metadata.goals.map { it.name })
            assertNotNull(t2, "Should have $expectedThing2GoalName goal: " + metadata.goals.map { it.name })

            assertEquals("Thanks to Dr Seuss", t1!!.description)
            assertEquals("Thanks again to Dr Seuss", t2!!.description)
        }

        @Test
        fun `action goal`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ActionGoal())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.goals.size)
            val g = metadata.goals.single()
            assertEquals("Creating a user", g.description)
            assertEquals(
                mapOf("it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE),
                g.preconditions,
                "Should have precondition for Person",
            )
        }
    }

    @Nested
    inner class Conditions {

        @Test
        fun `no conditions`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(NoConditions())
            assertNotNull(metadata)
            assertEquals(0, metadata!!.conditions.size)
        }

        @Test
        fun `one condition only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneConditionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            assertEquals("${OneConditionOnly::class.java.name}.condition1", metadata.conditions.first().name)
        }

        @Test
        fun `condition invocation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneConditionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            assertEquals(ConditionDetermination.TRUE, condition.evaluate(mockk()))
        }

    }

    @Nested
    inner class Actions {

        @Test
        fun `no actions`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneGoalOnly())
            assertNotNull(metadata)
            assertEquals(0, metadata!!.actions.size)
        }

        @Test
        fun `one action only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(1, action.inputs.size, "Should have 1 input")
            assertEquals(UserInput::class.java.name, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(
                Person::class.java.name,
                action.outputs.single().type,
                "Output name must match",
            )
        }

        @Test
        fun `one action referencing condition by name`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionReferencingConditionByName())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(1, action.inputs.size, "Should have 1 input")
            assertEquals(UserInput::class.java.name, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(
                Person::class.java.name,
                action.outputs.single().type,
                "Output name must match",
            )
            assertEquals(
                ConditionDetermination.TRUE,
                action.preconditions["it:${UserInput::class.qualifiedName}"],
                "Should have input precondition",
            )
            assertEquals(
                ConditionDetermination.TRUE,
                action.preconditions["condition1"],
                "Should have custom precondition",
            )
        }

        @Test
        fun `one action with custom tool group`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionWithCustomToolGroupOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(1, action.inputs.size, "Should have 1 input")
            assertEquals(UserInput::class.java.name, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(
                Person::class.java.name,
                action.outputs.single().type,
                "Output name must match",
            )
            assertEquals(1, action.toolGroups.size)
            assertEquals("magic", action.toolGroups.single())
        }

        @Test
        fun `one action with 2 args only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(AgentWithOneTransformerActionWith2ArgsOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(2, action.inputs.size, "Should have 2 inputs")
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertTrue(action.inputs.any { it.type == UserInput::class.java.name })
            assertTrue(action.inputs.any { it.type == Task::class.java.name })
            assertEquals(
                Person::class.java.name,
                action.outputs.single().type,
                "Output name must match"
            )
            assertEquals(IoBinding.DEFAULT_BINDING, action.outputs.single().name)
        }

        @Test
        fun `transformer action invocation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            val agent = mockk<IAgent>()
            every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns agent
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.llmTransformer } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull

            val pc = ProcessContext(
                blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe")),
                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.COMPLETED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.finalResult())
        }

        @Test
        fun `transformer action with 2 args invocation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(AgentWithOneTransformerActionWith2ArgsOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            val agent = mockk<IAgent>()
            every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns agent
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.llmTransformer } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull

            val blackboard = InMemoryBlackboard()
            blackboard += UserInput("John Doe")
            blackboard += ("task" to Task("task"))

            val pc = ProcessContext(
                blackboard = blackboard,
                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.COMPLETED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.finalResult())
        }

        @Test
        @Disabled
        fun `consumer action with no parameters`() {
        }

        @Nested
        inner class ToolMethodsOnAgenticClass {
            @Test
            fun `no actions`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(OneTransformerActionWith2Tools())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.single()
                assertEquals(2, action.toolCallbacks.size)
                assertEquals(
                    setOf("toolWithoutArg", "toolWithArg"),
                    action.toolCallbacks.map { it.toolDefinition.name() }.toSet(),
                )
            }

        }

        @Nested
        inner class CustomBinding {

            @Test
            fun `custom input bindings`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(OneTransformerActionWith2ArgsAndCustomInputBindings())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.single()
                assertEquals(2, action.inputs.size, "Should have 2 inputs")
                assertEquals(1, action.outputs.size, "Should have 1 output")
                val uib = action.inputs.single { it.type == UserInput::class.java.name }
                val tb = action.inputs.single { it.type == Task::class.java.name }
                assertEquals("userInput", uib.name)
                assertEquals("task", tb.name)
                assertEquals(
                    Person::class.java.name,
                    action.outputs.single().type,
                    "Output name must match",
                )
                assertEquals(
                    IoBinding.DEFAULT_BINDING,
                    action.outputs.single().name,
                    "Output name must match",
                )
            }

            @Test
            fun `custom output binding`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(OneTransformerActionWith2ArgsAndCustomOutputBinding())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.single()
                assertEquals(2, action.inputs.size, "Should have 2 inputs")
                assertEquals(1, action.outputs.size, "Should have 1 output")
                assertTrue(action.inputs.any { it.type == UserInput::class.java.name })
                assertTrue(action.inputs.any { it.type == Task::class.java.name })
                assertEquals(
                    Person::class.java.name,
                    action.outputs.single().type,
                    "Output name must match",
                )
                assertEquals("person", action.outputs.single().name)
            }
        }

        @Nested
        inner class Prompts {
            @Test
            fun `prompt action invocation`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(OnePromptActionOnly())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.first()
                val agent = mockk<IAgent>()
                every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
                val mockAgentProcess = mockk<AgentProcess>()
                every { mockAgentProcess.agent } returns agent
                val llmo = slot<LlmOptions>()
                val llmt = mockk<LlmTransformer>()
                every {
                    llmt.transform<Any, Any>(
                        any(),
                        any(),
                        capture(llmo),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns Person("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmTransformer } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull

                val pc = ProcessContext(
                    blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe")),
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.COMPLETED, result.status)
                assertEquals(Person("John Doe"), pc.blackboard.finalResult())
                assertEquals("magical", llmo.captured.model)
                assertEquals(1.7, llmo.captured.temperature)
            }

            @Test
            fun `prompt action invocation with tools`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(OnePromptActionWithToolOnly())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.first()
                val agent = mockk<IAgent>()
                every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
                val mockAgentProcess = mockk<AgentProcess>()
                every { mockAgentProcess.agent } returns agent
                val llmo = slot<LlmOptions>()

                val llmt = mockk<LlmTransformer>()
                val toolCallbacks = slot<List<ToolCallback>>()
                every {
                    llmt.transform<Any, Any>(
                        any(),
                        any(),
                        capture(llmo),
                        capture(toolCallbacks),
                        any(),
                        any(),
                        any(),
                    )
                } returns Person("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmTransformer } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull

                val pc = ProcessContext(
                    blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe")),
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.COMPLETED, result.status)
                assertEquals(Person("John Doe"), pc.blackboard.finalResult())
                assertEquals(1, toolCallbacks.captured.size)
                assertEquals("thing", toolCallbacks.captured.single().toolDefinition.name())
                assertEquals(LlmOptions.DEFAULT_MODEL, llmo.captured.model)
            }

            @Test
            fun `prompt action invocation with tools on domain object`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(FromPersonUsesDomainObjectTools())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.first()
                val agent = mockk<IAgent>()
                every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
                val mockAgentProcess = mockk<AgentProcess>()
                every { mockAgentProcess.agent } returns agent
                val llmo = slot<LlmOptions>()

                val llmt = mockk<LlmTransformer>()
                val toolCallbacks = slot<List<ToolCallback>>()
                every {
                    llmt.transform<Any, Any>(
                        any(),
                        any(),
                        capture(llmo),
                        capture(toolCallbacks),
                        any(),
                        any(),
                        any(),
                    )
                } returns UserInput("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmTransformer } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull

                val pc = ProcessContext(
                    blackboard = InMemoryBlackboard().bind("it", Person("John Doe")),
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.COMPLETED, result.status)
                assertEquals(UserInput("John Doe"), pc.blackboard.finalResult())
                assertEquals(1, toolCallbacks.captured.size)
                assertEquals("reverse", toolCallbacks.captured.single().toolDefinition.name())
                assertEquals(LlmOptions.DEFAULT_MODEL, llmo.captured.model)
            }

        }

    }

    @Nested
    inner class Agents {

        @Test
        fun `not an agent`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionOnly())
            assertNotNull(metadata)
            assertFalse(metadata!! is Agent)
        }

        @Test
        fun `recognize an agent`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(AgentWithOneTransformerActionWith2ArgsOnly())
            assertNotNull(metadata)
            assertTrue(metadata is IAgent, "@Agent should create an agent")
            metadata as IAgent
            assertEquals(1, metadata.actions.size)
            assertEquals(
                AgentWithOneTransformerActionWith2ArgsOnly::class.java.name,
                metadata.name,
            )
        }
    }
}
