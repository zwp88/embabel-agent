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
package com.embabel.agent.spi.support

import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.agent.support.SimpleTestAgent
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelProvider
import com.embabel.common.ai.model.ModelSelectionCriteria
import com.embabel.common.textio.template.JinjavaTemplateRenderer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.DefaultChatOptions
import org.springframework.ai.chat.prompt.Prompt
import kotlin.test.assertEquals

class FakeChatModel(
    val response: String,
    private val options: ChatOptions = DefaultChatOptions()
) : ChatModel {

    override fun getDefaultOptions(): ChatOptions = options

    override fun call(prompt: Prompt?): ChatResponse? {
        return ChatResponse(
            listOf(
                Generation(AssistantMessage(response))
            )
        )
    }
}


class ChatClientLlmOperationsTest {

    data class Setup(
        val llmOperations: LlmOperations,
        val mockAgentProcess: AgentProcess
    )

    private fun createChatClientLlmOperations(fakeChatModel: FakeChatModel): Setup {
        val ese = EventSavingAgenticEventListener()
        val mockProcessContext = mockk<ProcessContext>()
        every { mockProcessContext.platformServices } returns mockk()
        every { mockProcessContext.platformServices.agentPlatform } returns mockk()
        every { mockProcessContext.platformServices.agentPlatform.toolGroupResolver } returns RegistryToolGroupResolver(
            "mt",
            emptyList()
        )
        every { mockProcessContext.platformServices.eventListener } returns ese
        val mockAgentProcess = mockk<AgentProcess>()
        every { mockProcessContext.onProcessEvent(any()) } answers { ese.onProcessEvent(firstArg()) }
        every { mockProcessContext.agentProcess } returns mockAgentProcess

        every { mockAgentProcess.agent } returns SimpleTestAgent
        every { mockAgentProcess.processContext } returns mockProcessContext

        val mockModelProvider = mockk<ModelProvider>()
        val crit = slot<ModelSelectionCriteria>()
        val fakeLlm = Llm("fake", fakeChatModel)
        every { mockModelProvider.getLlm(capture(crit)) } returns fakeLlm
        val cco = ChatClientLlmOperations(mockModelProvider, DefaultToolDecorator(), JinjavaTemplateRenderer())
        return Setup(cco, mockAgentProcess)
    }

    data class Dog(val name: String)

    @Nested
    inner class CreateObject {
        @Test
        fun `returns string`() {
            val fakeChatModel = FakeChatModel("fake response")

            val setup = createChatClientLlmOperations(fakeChatModel)
            val result = setup.llmOperations.createObject(
                prompt = "prompt",
                interaction = LlmInteraction(
                    id = InteractionId("id"), llm = LlmOptions()
                ),
                outputClass = String::class.java,
                action = SimpleTestAgent.actions.first(),
                agentProcess = setup.mockAgentProcess,
            )
            assertEquals(fakeChatModel.response, result)
        }

        @Test
        fun `returns data class`() {
            val duke = Dog("Duke")

            val fakeChatModel = FakeChatModel(jacksonObjectMapper().writeValueAsString(duke))

            val setup = createChatClientLlmOperations(fakeChatModel)
            val result = setup.llmOperations.createObject(
                prompt = "prompt",
                interaction = LlmInteraction(
                    id = InteractionId("id"), llm = LlmOptions()
                ),
                outputClass = Dog::class.java,
                action = SimpleTestAgent.actions.first(),
                agentProcess = setup.mockAgentProcess,
            )
            assertEquals(duke, result)
        }
    }

    @Nested
    inner class CreateObjectIfPossible {

        @Test
        fun `returns data class - success`() {
            val duke = Dog("Duke")

            val fakeChatModel = FakeChatModel(jacksonObjectMapper().writeValueAsString(MaybeReturn(success = duke)))

            val setup = createChatClientLlmOperations(fakeChatModel)
            val result = setup.llmOperations.createObjectIfPossible(
                prompt = "prompt",
                interaction = LlmInteraction(
                    id = InteractionId("id"), llm = LlmOptions()
                ),
                outputClass = Dog::class.java,
                action = SimpleTestAgent.actions.first(),
                agentProcess = setup.mockAgentProcess,
            )
            assertEquals(duke, result.getOrThrow())
        }

        @Test
        fun `returns data class - failure`() {
            val fakeChatModel =
                FakeChatModel(jacksonObjectMapper().writeValueAsString(MaybeReturn<Dog>(failure = "didn't work")))

            val setup = createChatClientLlmOperations(fakeChatModel)
            val result = setup.llmOperations.createObjectIfPossible(
                prompt = "prompt",
                interaction = LlmInteraction(
                    id = InteractionId("id"), llm = LlmOptions()
                ),
                outputClass = Dog::class.java,
                action = SimpleTestAgent.actions.first(),
                agentProcess = setup.mockAgentProcess,
            )
            assertTrue(result.isFailure)
        }
    }

}
