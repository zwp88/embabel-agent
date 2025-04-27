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
package com.embabel.agent.api.dsl

import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.Condition
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.Transition
import com.embabel.agent.core.support.InMemoryBlackboard
import com.embabel.common.ai.model.LlmOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallback


data class Person(val name: String, val age: Int)
data class Summary(val text: String)

class PromptTransformerKtTest {

    @Nested
    inner class PromptTransformer {

        @Test
        fun `test prompt`() {
            val transformer = promptTransformer<MagicVictim, Frog>(name = "frogger") {
                "Turn the person named ${it.input.name} into a frog"
            }
            val magicVictim = MagicVictim(name = "Marmaduke")
            val blackboard = InMemoryBlackboard()
            val frog = Frog(name = "Marmaduke")
            val mockAgentProcess = mockk<AgentProcess>()
            val processContext = mockk<ProcessContext>()
            every { mockAgentProcess.processContext } returns processContext
            every { processContext.blackboard } returns blackboard
            every { processContext.agentProcess } returns mockAgentProcess
            every { processContext.getValue("it", MagicVictim::class.java.name) } returns magicVictim
            val promptSlot = slot<String>()
            every {
                processContext.createObject(
                    capture(promptSlot),
                    any(),
                    Frog::class.java,
                    mockAgentProcess,
                    transformer
                )
            } returns frog
            transformer.execute(processContext, outputTypes = emptyMap(), action = transformer)
        }

        @Test
        fun `transformer should use custom input and output variable names`() {
            val transformer = promptTransformer<Person, Summary>(
                name = "summarizer",
                inputVarName = "person",
                outputVarName = "summary"
            ) {
                "Summarize information about ${it.input.name} who is ${it.input.age} years old"
            }

//            assertEquals("person", transformer.inputVarName)
//            assertEquals("summary", transformer.outputVarName)

            val person = Person(name = "John", age = 30)
            val summary = Summary(text = "John is 30 years old")
            val mockAgentProcess = mockk<AgentProcess>()
            val processContext = mockk<ProcessContext>()

            every { mockAgentProcess.processContext } returns processContext
            every { processContext.blackboard } returns InMemoryBlackboard()
            every { processContext.agentProcess } returns mockAgentProcess
            every { processContext.getValue("person", Person::class.java.name) } returns person
            every {
                processContext.createObject(
                    any(),
                    any(),
                    Summary::class.java,
                    mockAgentProcess,
                    transformer
                )
            } returns summary

            transformer.execute(processContext = processContext, outputTypes = emptyMap(), action = transformer)

            verify { processContext.getValue("person", Person::class.java.name) }
        }

        @Test
        fun `transformer should handle pre and post conditions`() {
            val preCondition = mockk<Condition>()
            val postCondition = mockk<Condition>()

            every { preCondition.name } returns "preCheck"
            every { postCondition.name } returns "postCheck"

            val transformer = promptTransformer<MagicVictim, Frog>(
                name = "conditionalTransformer",
                pre = listOf(preCondition),
                post = listOf(postCondition)
            ) {
                "Transform ${it.input.name}"
            }

            assertEquals(listOf("preCheck"), transformer.pre)
            assertEquals(listOf("postCheck"), transformer.post)
        }

        @Test
        fun `transformer should handle custom LLM options`() {
            val customLlmOptions = LlmOptions(temperature = 0.7)

            val transformer = promptTransformer<MagicVictim, Frog>(
                name = "customLlmTransformer",
                llm = customLlmOptions
            ) {
                "Transform ${it.input.name}"
            }

            val magicVictim = MagicVictim(name = "Alice")
            val mockAgentProcess = mockk<AgentProcess>()
            val processContext = mockk<ProcessContext>()

            every { mockAgentProcess.processContext } returns processContext
            every { processContext.blackboard } returns InMemoryBlackboard()
            every { processContext.agentProcess } returns mockAgentProcess
            every { processContext.getValue("it", MagicVictim::class.java.name) } returns magicVictim
            every {
                processContext.createObject(
                    any(),
                    any(),
                    Frog::class.java,
                    mockAgentProcess,
                    transformer
                )
            } returns Frog(name = "Alice")

            transformer.execute(processContext = processContext, outputTypes = emptyMap(), action = transformer)
        }

        @Test
        @Disabled("behavior not yet defined")
        fun `transformer should handle transitions and expectations`() {
            val transition = Transition(to = "nextStep", condition = "someCondition")
            val expectationCondition = mockk<Condition>()

            every { expectationCondition.name } returns "expectationCheck"

            val transformer = promptTransformer<MagicVictim, Frog>(
                name = "transitionTransformer",
                transitions = listOf(transition),
                expectation = expectationCondition
            ) {
                "Transform ${it.input.name}"
            }

            // Should include both the explicit transition and the expectation transition
            assertEquals(2, transformer.transitions.size)

            // Verify the explicit transition
            val explicitTransition = transformer.transitions.find { it.to == "nextStep" }
            assertNotNull(explicitTransition)
            assertEquals("someCondition", explicitTransition?.condition)

            // Verify the expectation transition
            val expectationTransition = transformer.transitions.find { it.to == "transitionTransformer" }
            assertNotNull(expectationTransition)
            assertEquals("expectationCheck", expectationTransition?.condition)
        }

        @Test
        fun `transformer should handle tool groups and callbacks`() {
            val toolCallback = mockk<ToolCallback>()
            val toolGroups = listOf("math", "web")

            val transformer = promptTransformer<MagicVictim, Frog>(
                name = "toolTransformer",
                toolGroups = toolGroups,
                toolCallbacks = listOf(toolCallback)
            ) {
                "Transform ${it.input.name}"
            }

            assertEquals(toolGroups, transformer.toolGroups)

            val magicVictim = MagicVictim(name = "Bob")
            val mockAgentProcess = mockk<AgentProcess>()
            val processContext = mockk<ProcessContext>()

            every { mockAgentProcess.processContext } returns processContext
            every { processContext.blackboard } returns InMemoryBlackboard()
            every { processContext.agentProcess } returns mockAgentProcess
            every { processContext.getValue("it", MagicVictim::class.java.name) } returns magicVictim
            every {
                processContext.createObject(
                    any(),
                    any(),
                    Frog::class.java,
                    mockAgentProcess,
                    transformer
                )
            } returns Frog(name = "Bob")

            transformer.execute(processContext = processContext, outputTypes = emptyMap(), action = transformer)
        }

        @Test
        fun `transformer should handle rerunnable actions`() {
            val rerunnable = promptTransformer<MagicVictim, Frog>(
                name = "rerunnableTransformer",
                canRerun = true
            ) {
                "Transform ${it.input.name}"
            }

            val nonRerunnable = promptTransformer<MagicVictim, Frog>(
                name = "nonRerunnableTransformer",
                canRerun = false
            ) {
                "Transform ${it.input.name}"
            }

            assertTrue(rerunnable.canRerun)
            assertFalse(nonRerunnable.canRerun)
        }
    }
}
