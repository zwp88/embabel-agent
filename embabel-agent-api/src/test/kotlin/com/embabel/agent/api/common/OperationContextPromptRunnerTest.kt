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
package com.embabel.agent.api.common

import com.embabel.agent.api.annotation.support.Wumpus
import com.embabel.agent.api.common.support.OperationContextPromptRunner
import com.embabel.agent.core.Operation
import com.embabel.agent.experimental.primitive.Determination
import com.embabel.agent.support.Dog
import com.embabel.agent.testing.unit.FakeOperationContext
import com.embabel.chat.Message
import com.embabel.chat.UserMessage
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.util.StringTransformer
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OperationContextPromptRunnerTest {

    private fun createOperationContextPromptRunnerWithDefaults(context: OperationContext): OperationContextPromptRunner {
        return OperationContextPromptRunner(
            context = context,
            llm = LlmOptions(),
            toolGroups = emptySet(),
            toolObjects = emptyList(),
            promptContributors = emptyList(),
            contextualPromptContributors = emptyList(),
            generateExamples = false,
        )
    }

    @Nested
    inner class Core {

        @Test
        fun `test change LlmOptions`() {
            val llm = LlmOptions.withModel("my-model").withTemperature(1.0)
            val ocpr = createOperationContextPromptRunnerWithDefaults(mockk<OperationContext>())
                .withLlm(llm)
            assertEquals(llm, ocpr.llm, "LlmOptions not set correctly")
        }

        @Test
        fun `test toolObject instance`() {
            val llm = LlmOptions.withModel("my-model").withTemperature(1.0)
            val wumpus = Wumpus("wumpuses-have-tools")
            val ocpr = createOperationContextPromptRunnerWithDefaults(mockk<OperationContext>())
                .withLlm(llm)
                .withToolObject(wumpus)
            assertEquals(1, ocpr.toolObjects.size, "Must have one tool object")
            assertEquals(wumpus, ocpr.toolObjects[0].obj, "Tool object instance not set correctly")
            assertEquals(
                StringTransformer.IDENTITY,
                ocpr.toolObjects[0].namingStrategy,
                "Tool object naming strategy not set correctly"
            )
        }

        @Test
        fun `test ToolObject instance with custom naming strategy`() {
            val llm = LlmOptions.withModel("my-model").withTemperature(1.0)
            val wumpus = Wumpus("wumpuses-have-tools")
            val namingStrategy = StringTransformer { it.replace("_", " ") }
            val ocpr = createOperationContextPromptRunnerWithDefaults(mockk<OperationContext>())
                .withLlm(llm)
                .withToolObject(ToolObject(wumpus).withNamingStrategy(namingStrategy))
            assertEquals(1, ocpr.toolObjects.size, "Must have one tool object")
            assertEquals(wumpus, ocpr.toolObjects[0].obj, "Tool object instance not set correctly")
            assertEquals(
                namingStrategy,
                ocpr.toolObjects[0].namingStrategy,
                "Tool object naming strategy not set correctly"
            )
        }

        @Test
        @Disabled("test not implemented yet")
        fun `test contextual prompt contributors`() {
        }

        @Test
        @Disabled("test not implemented yet")
        fun `test withPromptElements`() {
        }

        @Test
        fun `test evaluateCondition`() {
            val mockOperationContext = mockk<OperationContext>()
            every { mockOperationContext.processContext } returns mockk()
            every { mockOperationContext.processContext.agentProcess } returns mockk()
            every {
                mockOperationContext.processContext.createObject(
                    any(),
                    any(),
                    Determination::class.java,
                    any(),
                    null
                )
            } answers {
                val messages = firstArg<List<Message>>()
                val prompt = (messages[0] as UserMessage).content
                assertTrue(prompt.contains("Evaluate this condition"), "Prompt didn't contain evaluate: $prompt")
                Determination(
                    result = true,
                    confidence = 0.8,
                    explanation = "Mocked explanation"
                )
            }
            every { mockOperationContext.operation } returns mockk<Operation>(relaxed = true)

            val runner = OperationContextPromptRunner(
                context = mockOperationContext,
                llm = LlmOptions(),
                toolGroups = emptySet(),
                toolObjects = emptyList(),
                promptContributors = emptyList(),
                contextualPromptContributors = emptyList(),
                generateExamples = false,
            )

            val result = runner.evaluateCondition("condition", "context", 0.5)
            assertTrue(result)
        }

    }

    @Nested
    inner class ObjectCreator {

        @Test
        fun `test no examples`() {
            val duke = Dog("Duke")
            val context = FakeOperationContext()
            val pr = spyk(createOperationContextPromptRunnerWithDefaults(context))
            val um = slot<List<Message>>()
            every { pr.createObject(capture(um), Dog::class.java) } returns duke
            val result = pr
                .creating(Dog::class.java)
                .fromPrompt("foo bar")
            assertEquals(duke, result, "Dog instance not returned correctly")
            assertEquals(1, um.captured.size, "Must be one message")
            assertEquals(um.captured[0].content, "foo bar", "Example not included in prompt")
        }

        @Test
        fun `test example`() {
            val context = FakeOperationContext()
            val pr = spyk(createOperationContextPromptRunnerWithDefaults(context))
            val pr1 = (pr
                .creating(Dog::class.java)
                .withExample("Good dog", Dog("Duke")) as PromptRunnerObjectCreator).promptRunner
            assertEquals(1, pr1.promptContributors.size, "Must be one contributor")
            val eg = pr1.promptContributors[0].contribution()
            assertTrue(eg.contains("Duke"), "Should include example dog name")
            assertTrue(eg.contains("Good dog"), "Should include example description")
            assertTrue(eg.contains("{"), "Should include JSON")
        }
    }

}
