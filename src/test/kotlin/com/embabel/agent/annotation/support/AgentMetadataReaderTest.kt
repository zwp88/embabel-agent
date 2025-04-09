/*
 * Copyright 2025 Embabel Software, Inc.
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

import com.embabel.agent.*
import com.embabel.agent.annotation.*
import com.embabel.agent.annotation.Action
import com.embabel.agent.annotation.Condition
import com.embabel.agent.event.AgenticEventListener.Companion.DevNull
import com.embabel.agent.primitive.LlmOptions
import com.embabel.agent.support.InMemoryBlackboard
import com.embabel.plan.goap.ConditionDetermination
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.annotation.Tool

data class Person(val name: String) {

    @Tool
    fun reverse() = name.reversed()

}

@Agentic
class NoMethods

@Agentic
class OneGoalOnly {

    val thing1 = Goal.createInstance(
        name = "thing1",
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    ).withValue(30.0)
}

@Agentic
class TwoGoalsOnly {

    val thing1 = Goal.createInstance(
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    )
    val thing2 = Goal.createInstance(
        description = "Thanks again to Dr Seuss",
        type = Person::class.java,
    )
}

@Agentic
class ActionGoal {

    @Action
    @AchievesGoal(description = "Creating a user")
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class NoConditions {

    // A goal makes it legal
    val g = Goal.createInstance(
        name = "thing1",
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    ).withValue(30.0)

}

@Agentic
class OneConditionOnly {

    @Condition(cost = .5)
    fun condition1(processContext: ProcessContext): Boolean {
        return true
    }

}

@Agentic
class OneTransformerActionOnly {

    @Action(cost = 500.0)
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionReferencingConditionByName {

    @Action(pre = ["condition1"])
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWithCustomToolGroupOnly {

    @Action(cost = 500.0, toolGroups = ["magic"])
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

data class Task(
    val what: String,
)


@Agentic
class OneTransformerActionWith2ArgsOnly {

    @Action(cost = 500.0)
    fun toPerson(userInput: UserInput, task: Task): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWith2ArgsAndCustomInputBindings {

    @Action
    fun toPerson(
        @RequireNameMatch userInput: UserInput,
        @RequireNameMatch task: Task,
    ): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWith2ArgsAndCustomOutputBinding {

    @Action(outputBinding = "person")
    fun toPerson(userInput: UserInput, task: Task): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OnePromptActionOnly(
) {

    val promptRunner = PromptRunner(
        // Java style usage
        llm = LlmOptions.DEFAULT.withTemperature(1.7).withModel("magical"),
    )

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return promptRunner.run("Generated prompt for ${userInput.content}")
    }

}

@Agentic
class Combined {

    val planner = Goal.createInstance(
        description = "Create a person",
        type = Person::class.java,
    ).withValue(30.0)

    // Can reuse this or inject
    val magicalLlm = PromptRunner(
        // Java style usage
        llm = LlmOptions.DEFAULT.withTemperature(1.7).withModel("magical"),
    )

    @Condition(cost = .5)
    fun condition1(processContext: ProcessContext): Boolean {
        return true
    }

    @Action
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return magicalLlm.run("Generated prompt for ${userInput.content}")
    }

    @Tool
    fun weatherService(location: String) =
        "The weather in $location is ${listOf("sunny", "raining", "foggy").random()}"


}

@Agentic
class OnePromptActionWithToolOnly(
) {

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return Prompt.run("Generated prompt for ${userInput.content}")
    }

    @Tool
    fun thing(): String {
        return "foobar"
    }

}

@Agentic
class FromPersonUsesDomainObjectTools {

    @Action
    fun fromPerson(
        person: Person
    ): UserInput {
        return Prompt.run("Create a UserInput")
    }
}

@Agentic
class OneTransformerActionWith2Tools {

    @Action
    fun toPerson(
        @RequireNameMatch userInput: UserInput,
        @RequireNameMatch task: Task,
    ): Person {
        return Person(userInput.content)
    }

    @Tool
    fun toolWithoutArg(): String = "foo"

    @Tool
    fun toolWithArg(location: String) = "bar"

}


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
            assertThrows<IllegalArgumentException> {
                reader.createAgentMetadata(NoMethods())
            }
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
            val t1 = metadata.goals.single { it.name == "thing1" }
            val t2 = metadata.goals.single { it.name == "thing2" }
            assertEquals("Thanks to Dr Seuss", t1.description)
            assertEquals("Thanks again to Dr Seuss", t2.description)
        }

        @Test
        fun `action goal`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(ActionGoal())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.goals.size)
            val g = metadata.goals.single()
            assertEquals("Creating a user", g.description)
            assertEquals(mapOf("it:Person" to ConditionDetermination.TRUE), g.preconditions)
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
            assertEquals(UserInput::class.java.simpleName, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(Person::class.java.simpleName, action.outputs.single().type)
        }

        @Test
        fun `one action referencing condition by name`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionReferencingConditionByName())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(1, action.inputs.size, "Should have 1 input")
            assertEquals(UserInput::class.java.simpleName, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(Person::class.java.simpleName, action.outputs.single().type)
            assertEquals(
                ConditionDetermination.TRUE,
                action.preconditions["it:UserInput"],
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
            assertEquals(UserInput::class.java.simpleName, action.inputs.single().type)
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertEquals(Person::class.java.simpleName, action.outputs.single().type)
            assertEquals(1, action.toolGroups.size)
            assertEquals("magic", action.toolGroups.single())
        }

        @Test
        fun `one action with 2 args only`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionWith2ArgsOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.single()
            assertEquals(2, action.inputs.size, "Should have 2 inputs")
            assertEquals(1, action.outputs.size, "Should have 1 output")
            assertTrue(action.inputs.any { it.type == UserInput::class.java.simpleName })
            assertTrue(action.inputs.any { it.type == Task::class.java.simpleName })
            assertEquals(Person::class.java.simpleName, action.outputs.single().type)
            assertEquals(IoBinding.DEFAULT_BINDING, action.outputs.single().name)
        }

        @Test
        fun `transformer action invocation`() {
            val reader = AgentMetadataReader()
            val metadata = reader.createAgentMetadata(OneTransformerActionOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            val agent = mockk<Agent>()
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
            val metadata = reader.createAgentMetadata(OneTransformerActionWith2ArgsOnly())
            assertNotNull(metadata)
            assertEquals(1, metadata!!.actions.size)
            val action = metadata.actions.first()
            val agent = mockk<Agent>()
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
                val uib = action.inputs.single { it.type == UserInput::class.java.simpleName }
                val tb = action.inputs.single { it.type == Task::class.java.simpleName }
                assertEquals("userInput", uib.name)
                assertEquals("task", tb.name)
                assertEquals(Person::class.java.simpleName, action.outputs.single().type)
                assertEquals(IoBinding.DEFAULT_BINDING, action.outputs.single().name)
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
                assertTrue(action.inputs.any { it.type == UserInput::class.java.simpleName })
                assertTrue(action.inputs.any { it.type == Task::class.java.simpleName })
                assertEquals(Person::class.java.simpleName, action.outputs.single().type)
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
                val agent = mockk<Agent>()
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
                val agent = mockk<Agent>()
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
                val agent = mockk<Agent>()
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
}
