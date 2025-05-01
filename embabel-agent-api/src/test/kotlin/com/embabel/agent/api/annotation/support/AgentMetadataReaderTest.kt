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
package com.embabel.agent.api.annotation.support

import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.dsl.Frog
import com.embabel.agent.core.ActionStatusCode
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.hitl.ConfirmationRequest
import com.embabel.agent.core.support.HAS_RUN_CONDITION_PREFIX
import com.embabel.agent.core.support.InMemoryBlackboard
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.AgenticEventListener.Companion.DevNull
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.agent.spi.PlatformServices
import com.embabel.agent.support.containsAll
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byName
import com.embabel.plan.goap.ConditionDetermination
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
        fun `action goal requires output of action method`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ActionGoal())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.goals.size)
            val g = metadata.goals.single()
            assertEquals("Creating a person", g.description)
            assertTrue(
                g.preconditions.containsAll(mapOf("it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE)),
                "Should have precondition for Person",
            )
        }

        @Test
        fun `action goal requires action method to have run`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ActionGoal())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.goals.size)
            val action = metadata.actions.single()
            val g = metadata.goals.single()
            assertEquals("Creating a person", g.description)
            val expected = mapOf(
                "it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE,
                HAS_RUN_CONDITION_PREFIX + action.name to ConditionDetermination.TRUE
            )
            assertTrue(
                g.preconditions.containsAll(
                    expected,
                ),
                "Should have precondition for input to the action method: have\n${g.preconditions}, expected\n$expected",
            )
        }

        @Test
        fun `two distinct action goals`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(TwoActionGoals())
            assertNotNull(metadata)
            assertEquals(2, metadata!!.goals.size)
            val personGoal = metadata.goals.find { it.name == TwoActionGoals::class.java.name + ".toPerson" }
                ?: fail("Should have toPerson goal: " + metadata.goals.map { it.name })
            val frogGoal = metadata.goals.find { it.name == TwoActionGoals::class.java.name + ".toFrog" }
                ?: fail("Should have toFrog goal: " + metadata.goals.map { it.name })

            assertEquals("Creating a person", personGoal.description)
            assertTrue(
                personGoal.preconditions.containsAll(
                    mapOf(
                        "it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE,
//                        "it:${UserInput::class.qualifiedName}" to ConditionDetermination.TRUE
                    )
                ),
                "Should have precondition for Person",
            )
            assertEquals("Creating a frog", frogGoal.description)
            assertTrue(
                frogGoal.preconditions.containsAll(
                    mapOf("it:${Frog::class.qualifiedName}" to ConditionDetermination.TRUE),
                ),
                "Should have precondition for Frog",
            )
        }

        @Test
        fun `two actually non conflicting action goals with different inputs but same output`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(TwoActuallyNonConflictingActionGoalsWithSameOutput())
            assertNotNull(metadata)
            assertEquals(2, metadata!!.goals.size)
            val expectedPersonGoalName =
                TwoActuallyNonConflictingActionGoalsWithSameOutput::class.java.name + ".toPerson"
            val personGoal =
                metadata.goals.find { it.name == expectedPersonGoalName }
                    ?: fail("Should have $expectedPersonGoalName goal: " + metadata.goals.map { it.name })

            val alsoGoal =
                metadata.goals.find { it.name == TwoActuallyNonConflictingActionGoalsWithSameOutput::class.java.name + ".alsoToPerson" }
                    ?: fail("Should have alsoToPerson goal: " + metadata.goals.map { it.name })

            assertEquals("Creating a person", personGoal.description)
            assertTrue(
                personGoal.preconditions.containsAll(
                    mapOf(
                        "it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE,
//                        "it:${UserInput::class.qualifiedName}" to ConditionDetermination.TRUE,
                    )
                ),
                "Should have precondition for Person",
            )
            assertEquals("Also to person", alsoGoal.description)
            assertTrue(
                alsoGoal.preconditions.containsAll(
                    mapOf("it:${Person::class.qualifiedName}" to ConditionDetermination.TRUE)
                ),
                "Should have precondition for alsoPerson",
            )
        }

        @Test
        @Disabled("must decide what behavior should be")
        fun `two conflicting action goals`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(TwoConflictingActionGoals())
            TODO("decide what to do here: this invalid")
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
        fun `one condition taking ProcessContext`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneProcessContextConditionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            assertEquals(
                "${OneProcessContextConditionOnly::class.java.name}.condition1",
                metadata.conditions.first().name
            )
        }

        @Test
        fun `processContext condition invocation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneProcessContextConditionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            every { mockProcessContext.agentProcess } returns mockk()
            assertEquals(ConditionDetermination.TRUE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `blackboard condition invocation not found`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ConditionFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            every { mockProcessContext.blackboard } returns InMemoryBlackboard()
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java,
            )
            assertEquals(ConditionDetermination.FALSE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `blackboard condition invocation found and true`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ConditionFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            val bb = InMemoryBlackboard()
            bb += Person("Rod")
            every { mockProcessContext.blackboard } returns bb
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java,
            )
            assertEquals(ConditionDetermination.TRUE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `custom named blackboard condition invocation found and true`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(CustomNameConditionFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            assertEquals("condition1", condition.name)
            val mockProcessContext = mockk<ProcessContext>()
            val bb = InMemoryBlackboard()
            bb += Person("Rod")
            every { mockProcessContext.blackboard } returns bb
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java,
            )
            assertEquals(ConditionDetermination.TRUE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `blackboard condition invocation found and false`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ConditionFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            val bb = InMemoryBlackboard()
            bb += Person("ted")
            every { mockProcessContext.blackboard } returns bb
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java,
            )
            assertEquals(ConditionDetermination.FALSE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `blackboard conditions invocation not all found and false`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ConditionsFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            val bb = InMemoryBlackboard()
            bb += Person("Rod")
            every { mockProcessContext.blackboard } returns bb
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java, Frog::class.java,
            )
            assertEquals(ConditionDetermination.FALSE, condition.evaluate(mockProcessContext))
        }

        @Test
        fun `blackboard conditions invocation all found and true`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ConditionsFromBlackboard())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.conditions.size)
            val condition = metadata.conditions.first()
            val mockProcessContext = mockk<ProcessContext>()
            val bb = InMemoryBlackboard()
            bb += Person("Rod")
            bb += Frog("Kermit")
            every { mockProcessContext.blackboard } returns bb
            every { mockProcessContext.agentProcess.agent.domainTypes } returns listOf(
                Person::class.java, Frog::class.java,
            )
            assertEquals(ConditionDetermination.TRUE, condition.evaluate(mockProcessContext))
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
            every { mockPlatformServices.llmOperations } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull
            val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
            every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                blackboard.getValue(
                    firstArg(),
                    secondArg(),
                    thirdArg(),
                )
            }
            every { mockAgentProcess.set(any(), any()) } answers {
                blackboard.set(
                    firstArg(),
                    secondArg(),
                )
            }
            every { mockAgentProcess.lastResult() } returns Person("John Doe")

            val pc = ProcessContext(
                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.SUCCEEDED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.lastResult())
        }

        @Test
        fun `transformer action invocation with payload`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionTakingPayloadOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            assertEquals(
                1,
                action.inputs.size,
                "Should not consider payload as input: ${action.inputs}",
            )
            assertEquals(
                UserInput::class.java.name,
                action.inputs.single().type,
                "Should not consider payload as input: ${action.inputs}",
            )
            val agent = mockk<IAgent>()
            every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns agent
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.llmOperations } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull
            val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
            every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                blackboard.getValue(
                    firstArg(),
                    secondArg(),
                    thirdArg(),
                )
            }
            every { mockAgentProcess.set(any(), any()) } answers {
                blackboard.set(
                    firstArg(),
                    secondArg(),
                )
            }
            every { mockAgentProcess.lastResult() } returns Person("John Doe")

            val pc = ProcessContext(

                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.SUCCEEDED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.lastResult())
        }

        @Test
        fun `action invocation with OperationPayload`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionTakingOperationPayload())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            assertEquals(
                1,
                action.inputs.size,
                "Should not consider payload as input: ${action.inputs}",
            )
            assertEquals(
                UserInput::class.java.name,
                action.inputs.single().type,
                "Should not consider payload as input",
            )
            val agent = mockk<IAgent>()
            every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.agent } returns agent
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.llmOperations } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull
            val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
            every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                blackboard.getValue(
                    firstArg(),
                    secondArg(),
                    thirdArg(),
                )
            }
            every { mockAgentProcess.set(any(), any()) } answers {
                blackboard.set(
                    firstArg(),
                    secondArg(),
                )
            }
            every { mockAgentProcess.lastResult() } returns Person("John Doe")

            val pc = ProcessContext(

                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.SUCCEEDED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.lastResult())
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
            every { mockPlatformServices.llmOperations } returns mockk()
            every { mockPlatformServices.eventListener } returns DevNull

            val blackboard = InMemoryBlackboard()
            blackboard += UserInput("John Doe")
            blackboard += ("task" to Task("task"))
            every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                blackboard.getValue(
                    firstArg(),
                    secondArg(),
                    thirdArg(),
                )
            }
            every { mockAgentProcess.set(any(), any()) } answers {
                blackboard.set(
                    firstArg(),
                    secondArg(),
                )
            }
            every { mockAgentProcess.lastResult() } returns Person("John Doe")

            val pc = ProcessContext(
                platformServices = mockPlatformServices,
                agentProcess = mockAgentProcess,
            )
            val result = action.execute(pc, mockk(), action)
            assertEquals(ActionStatusCode.SUCCEEDED, result.status)
            assertEquals(Person("John Doe"), pc.blackboard.lastResult())
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
        inner class TestToolMethodsOnDomainObject {

            @Test
            fun `tools are added from single domain object used in this action`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(ToolMethodsOnDomainObject())
                assertNotNull(metadata)
                assertEquals(2, metadata!!.actions.size)
                val action = metadata.actions.single { it.name.contains("toPerson") }
                assertEquals(
                    2,
                    action.toolCallbacks.size,
                    "Should have two tools from domain object: have ${action.toolCallbacks.map { it.toolDefinition.name() }}",
                )
                assertEquals(
                    setOf("toolWithoutArg", "toolWithArg"),
                    action.toolCallbacks.map { it.toolDefinition.name() }.toSet(),
                )
            }

            @Test
            fun `tools are added from multiple domain objects used in this action`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(ToolMethodsOnDomainObjects())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.single()
                assertEquals(
                    3,
                    action.toolCallbacks.size,
                    "Should have three tools from domain object: have ${action.toolCallbacks.map { it.toolDefinition.name() }}",
                )
                assertEquals(
                    setOf("toolWithoutArg", "toolWithArg", "reverse"),
                    action.toolCallbacks.map { it.toolDefinition.name() }.toSet(),
                )
            }

            @Test
            @Disabled("not yet implemented")
            fun `handles conflicting tool definitions in multiple domain objects`() {

            }

            @Test
            fun `tools are not added from tooled domain object not used in this action`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(ToolMethodsOnDomainObject())
                assertNotNull(metadata)
                assertEquals(2, metadata!!.actions.size)
                val action = metadata.actions.single { it.name.contains("toFrog") }
                assertEquals(
                    0,
                    action.toolCallbacks.size,
                    "Should have no tools from domain object: have ${action.toolCallbacks.map { it.toolDefinition.name() }}",
                )
            }

            @Test
            fun `invoke tool callback on domain object`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(ToolMethodsOnDomainObject())
                assertNotNull(metadata)
                assertEquals(2, metadata!!.actions.size)
                val action = metadata.actions.single { it.name.contains("toPerson") }
                assertEquals(
                    2,
                    action.toolCallbacks.size,
                    "Should have two tools from domain object: have ${action.toolCallbacks.map { it.toolDefinition.name() }}",
                )
                assertEquals(
                    setOf("toolWithoutArg", "toolWithArg"),
                    action.toolCallbacks.map { it.toolDefinition.name() }.toSet(),
                )
                val toolWithArg = action.toolCallbacks.single { it.toolDefinition.name() == "toolWithArg" }
                val result = toolWithArg.call(
                    """
                    {
                        "location": "foo"
                    }
                """.trimIndent()
                )
                assertEquals(result, "\"foo\"")
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
                val llmo = slot<LlmInteraction>()
                val llmt = mockk<LlmOperations>()
                every {
                    llmt.createObject<Any>(
                        any(),
                        capture(llmo),
                        any(),
                        any(),
                        any(),
                    )
                } returns Person("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmOperations } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull
                val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
                every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                    blackboard.getValue(
                        firstArg(),
                        secondArg(),
                        thirdArg(),
                    )
                }
                every { mockAgentProcess.set(any(), any()) } answers {
                    blackboard.set(
                        firstArg(),
                        secondArg(),
                    )
                }
                every { mockAgentProcess.lastResult() } returns Person("John Doe")

                val pc = ProcessContext(
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.SUCCEEDED, result.status)
                assertEquals(Person("John Doe"), pc.blackboard.lastResult())
                assertEquals(byName(LlmOptions.DEFAULT_MODEL), llmo.captured.llm.criteria)
                assertEquals(1.7, llmo.captured.llm.temperature)
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
                val llmi = slot<LlmInteraction>()
                val llmt = mockk<LlmOperations>()
                every {
                    llmt.createObject<Any>(
                        any(),
                        capture(llmi),
                        any(),
                        any(),
                        any(),
                    )
                } returns Person("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmOperations } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull
                val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
                every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                    blackboard.getValue(
                        firstArg(),
                        secondArg(),
                        thirdArg(),
                    )
                }
                every { mockAgentProcess.set(any(), any()) } answers {
                    blackboard.set(
                        firstArg(),
                        secondArg(),
                    )
                }
                every { mockAgentProcess.lastResult() } returns Person("John Doe")

                val pc = ProcessContext(
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.SUCCEEDED, result.status)
                assertEquals(Person("John Doe"), pc.blackboard.lastResult())
                assertEquals(1, llmi.captured.toolCallbacks.size)
                assertEquals("thing", llmi.captured.toolCallbacks.single().toolDefinition.name())
                assertEquals(byName(LlmOptions.DEFAULT_MODEL), llmi.captured.llm.criteria)
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
                val llmo = slot<LlmInteraction>()
                val llmt = mockk<LlmOperations>()
                every {
                    llmt.createObject<Any>(
                        any(),
                        capture(llmo),
                        any(),
                        any(),
                        any(),
                    )
                } returns UserInput("John Doe")
                val mockPlatformServices = mockk<PlatformServices>()
                every { mockPlatformServices.llmOperations } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull
                val blackboard = InMemoryBlackboard().bind("it", Person("John Doe"))
                every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                    blackboard.getValue(
                        firstArg(),
                        secondArg(),
                        thirdArg(),
                    )
                }
                every { mockAgentProcess.set(any(), any()) } answers {
                    blackboard.set(
                        firstArg(),
                        secondArg(),
                    )
                }
                every { mockAgentProcess.lastResult() } returns UserInput("John Doe")

                val pc = ProcessContext(
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.SUCCEEDED, result.status)
                assertTrue(pc.blackboard.lastResult() is UserInput)
                assertEquals("John Doe", (pc.blackboard.lastResult() as UserInput).content)
                assertEquals(
                    1,
                    llmo.captured.toolCallbacks.size,
                    "Should have one callback, had ${llmo.captured.toolCallbacks.map { it.toolDefinition.name() }}",
                )
                assertEquals("reverse", llmo.captured.toolCallbacks.single().toolDefinition.name())
                assertEquals(byName(LlmOptions.DEFAULT_MODEL), llmo.captured.llm.criteria)
            }

        }

        @Nested
        inner class AwaitableTest {

            @Test
            fun `awaitable action invocation`() {
                val reader = AgentMetadataReader()
                val metadata = reader.createAgentMetadata(AwaitableOne())
                assertNotNull(metadata)
                assertEquals(1, metadata!!.actions.size)
                val action = metadata.actions.first()
                val agent = mockk<IAgent>()
                every { agent.domainTypes } returns listOf(Person::class.java, UserInput::class.java)
                val mockAgentProcess = mockk<AgentProcess>()
                every { mockAgentProcess.agent } returns agent
                every { mockAgentProcess.id } returns "mythical_beast"

                val mockPlatformServices = mockk<PlatformServices>()
                val llmt = mockk<LlmOperations>()
                every { mockPlatformServices.llmOperations } returns llmt
                every { mockPlatformServices.eventListener } returns DevNull
                val blackboard = InMemoryBlackboard().bind("it", UserInput("John Doe"))
                every { mockAgentProcess.getValue(any(), any(), any()) } answers {
                    blackboard.getValue(
                        firstArg(),
                        secondArg(),
                        thirdArg(),
                    )
                }
                every { mockAgentProcess.addObject(any()) } answers {
                    blackboard.addObject(
                        firstArg(),
                    )
                }
                every {
                    mockAgentProcess.lastResult()
                } answers {
                    blackboard.lastResult()
                }

                val pc = ProcessContext(
                    platformServices = mockPlatformServices,
                    agentProcess = mockAgentProcess,
                )
                val result = action.execute(pc, mockk(), action)
                assertEquals(ActionStatusCode.WAITING, result.status)
                val fr = pc.blackboard.lastResult()
                assertTrue(
                    fr is ConfirmationRequest<*>,
                    "Last result should be an ConfirmationRequest: had ${blackboard.infoString(true)}",
                )
                val awaitable = fr as ConfirmationRequest<*>
                assertEquals(Person("John Doe"), awaitable.payload)
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
