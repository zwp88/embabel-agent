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

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.core.*
import com.embabel.agent.event.AgenticEventListener
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.ModelProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.DefaultChatOptions
import org.springframework.ai.chat.prompt.Prompt

data class Person(val name: String)

data class WierdPerson(val name: String, val age: Int, val weirdness: String)

data class Return(
    val result: Result<*>,
    val capturedPrompt: String,
)

class ChatClientLlmTransformerTest {

    @Nested
    inner class Transform {

        @Nested
        inner class HappyPath {

            @Test
            fun `happy path`() {
                val person = Person("John")
                val result = runWithPromptReturning(jacksonObjectMapper().writeValueAsString(person))
                assertEquals(person, result)
            }

            @Test
            fun `events emitted`() {
                val ese = EventSavingAgenticEventListener()
                val person = Person("John")
                val result = runWithPromptReturning(
                    llmReturn = jacksonObjectMapper().writeValueAsString(person),
                    eventListener = ese,
                )
                assertEquals(person, result)
                assertEquals(2, ese.processEvents.size)
            }


        }

        @Nested
        inner class Errors {

            @Test
            @Disabled("Decide on correct behavior")
            fun `non JSON return`() {
                val result = runWithPromptReturning("This ain't no JSON")
            }

            @Test
            @Disabled("Decide on correct behavior")
            fun `irrelevant JSON return`() {
                runWithPromptReturning(
                    jacksonObjectMapper().writeValueAsString(
                        mapOf(
                            "foo" to "bar",
                        ),
                    ),
                )
            }

        }

        fun runWithPromptReturning(
            llmReturn: String,
            eventListener: AgenticEventListener = EventSavingAgenticEventListener(),
        ): Any {
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns eventListener
            val mockAgentPlatform = mockk<AgentPlatform>()
            every { mockAgentPlatform.toolGroupResolver } returns RegistryToolGroupResolver("mt", emptyList())
            every { mockPlatformServices.agentPlatform } returns mockAgentPlatform
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.processContext.platformServices } returns mockPlatformServices
            val mockAgent = mockk<Agent>()
            every { mockAgent.resolveToolCallbacks(any()) } returns emptySet()
            every { mockAgentProcess.agent } returns mockAgent
            val mockProcessContext = mockk<ProcessContext>()
            every { mockProcessContext.platformServices } returns mockPlatformServices
            every { mockProcessContext.agentProcess } returns mockAgentProcess
            every { mockAgentProcess.processContext } returns mockProcessContext
            val mockModelProvider = mockk<ModelProvider>()
            val mockChatModel = mockk<ChatModel>()
            every { mockChatModel.defaultOptions } returns DefaultChatOptions()
            val promptSlot = slot<Prompt>()
            every { mockChatModel.call(capture(promptSlot)) } returns ChatResponse(
                listOf(
                    Generation(AssistantMessage(llmReturn)),
                ),
            )
            every { mockModelProvider.getLlm(any()) } returns Llm("test", mockChatModel)

            val transformer = ChatClientLlmOperations(mockModelProvider)
            return transformer.transform(
                input = "Hello, world!",
                prompt = { "Say hello" },
                llmOptions = LlmOptions(),
                toolCallbacks = emptyList(),
                agentProcess = mockAgentProcess,
                action = null,
                outputClass = Person::class.java,
            )
        }
    }

    @Nested
    inner class MaybeTransform {

        @Nested
        inner class HappyPath {

            @Test
            fun `happy path`() {
                val person = Person("John")
                val result = runWithPromptReturning(
                    llmReturn = jacksonObjectMapper().writeValueAsString(MaybeReturn(person)),
                    outputClass = Person::class.java,
                )
                assertEquals(Result.success(person), result.result)
            }

            @Test
            fun `events emitted`() {
                val ese = EventSavingAgenticEventListener()
                val person = Person("John")
                val result = runWithPromptReturning(
                    llmReturn = jacksonObjectMapper().writeValueAsString(MaybeReturn(person)),
                    eventListener = ese,
                    outputClass = Person::class.java,
                )
                assertEquals(Result.success(person), result.result)
                assertEquals(2, ese.processEvents.size)
            }

            @Test
            fun `schema contains type info`() {
                val ese = EventSavingAgenticEventListener()
                val person = WierdPerson("Marmaduke", 24, "weird")
                val result = runWithPromptReturning(
                    llmReturn = jacksonObjectMapper().writeValueAsString(MaybeReturn(success = person)),
                    eventListener = ese,
                    outputClass = WierdPerson::class.java,
                )
                assertEquals(Result.success(person), result.result)
                assertTrue(
                    result.capturedPrompt.contains("weirdness"),
                    "Prompt should contain 'weirdness' field:\n${result.capturedPrompt}",
                )
            }

            @Test
            fun `schema contains alternative option`() {
                val ese = EventSavingAgenticEventListener()
                val result = runWithPromptReturning(
                    llmReturn =
                        jacksonObjectMapper().writeValueAsString(
                            MaybeReturn(
                                success = null,
                                failure = "couldn't do it"
                            )
                        ),
                    eventListener = ese,
                    outputClass = WierdPerson::class.java,
                )
                assertTrue(result.result.isFailure)
                assertTrue(
                    result.capturedPrompt.contains("impossible"),
                    "Prompt should contain the word 'impossible':\n${result.capturedPrompt}",
                )
            }

        }

        @Nested
        inner class Errors {

            @Test
            @Disabled("Decide on correct behavior")
            fun `non JSON return`() {
                val result = runWithPromptReturning(
                    llmReturn = "This ain't no JSON",
                    outputClass = Person::class.java,
                )
            }

            @Test
            @Disabled("Decide on correct behavior")
            fun `irrelevant JSON return`() {
                runWithPromptReturning(
                    llmReturn = jacksonObjectMapper().writeValueAsString(
                        mapOf(
                            "foo" to "bar",
                        ),
                    ),
                    outputClass = Person::class.java,
                )
            }

        }


        fun runWithPromptReturning(
            llmReturn: String,
            eventListener: AgenticEventListener = EventSavingAgenticEventListener(),
            outputClass: Class<*>,
        ): Return {
            val mockPlatformServices = mockk<PlatformServices>()
            every { mockPlatformServices.eventListener } returns eventListener
            val mockAgentPlatform = mockk<AgentPlatform>()
            every { mockAgentPlatform.toolGroupResolver } returns RegistryToolGroupResolver("mt", emptyList())
            every { mockPlatformServices.agentPlatform } returns mockAgentPlatform
            val mockAgentProcess = mockk<AgentProcess>()
            every { mockAgentProcess.processContext.platformServices } returns mockPlatformServices
            val mockAgent = mockk<Agent>()
            every { mockAgent.resolveToolCallbacks(any()) } returns emptySet()
            every { mockAgentProcess.agent } returns mockAgent
            val mockProcessContext = mockk<ProcessContext>()
            every { mockProcessContext.platformServices } returns mockPlatformServices
            every { mockProcessContext.agentProcess } returns mockAgentProcess
            every { mockAgentProcess.processContext } returns mockProcessContext
            val mockModelProvider = mockk<ModelProvider>()
            val mockChatModel = mockk<ChatModel>()
            every { mockChatModel.defaultOptions } returns DefaultChatOptions()
            val promptSlot = slot<Prompt>()
            every { mockChatModel.call(capture(promptSlot)) } returns ChatResponse(
                listOf(
                    Generation(AssistantMessage(llmReturn)),
                ),
            )
            every { mockModelProvider.getLlm(any()) } returns Llm("test", mockChatModel)

            val transformer = ChatClientLlmOperations(mockModelProvider)
            val result = transformer.transformIfPossible(
                input = "Hello, world!",
                prompt = { "Say hello" },
                llmOptions = LlmOptions(),
                toolCallbacks = emptyList(),
                agentProcess = mockAgentProcess,
                action = null,
                outputClass = outputClass,
            )
            return Return(
                result = result,
                capturedPrompt = promptSlot.captured.toString(),
            )
        }
    }

}
