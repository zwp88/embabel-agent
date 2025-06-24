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
import com.embabel.agent.event.ToolCallRequestEvent
import com.embabel.agent.event.ToolCallResponseEvent
import com.embabel.agent.spi.OperationScheduler
import com.embabel.agent.spi.PlatformServices
import com.embabel.agent.testing.common.EventSavingAgenticEventListener
import com.embabel.common.ai.model.LlmOptions
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.method.MethodToolCallback

class SimpleTools {

    @Tool
    fun testTool(
    ): String {
        return "foobar"
    }
}

class ToolDecoratorsKtTest {

    @Test
    fun `preserves metadata`() {
        val tool = ToolCallbacks.from(SimpleTools()).single()
        val agentProcess = mockk<AgentProcess>()
        val llm = LlmOptions()
        val decorated = tool.withEventPublication(agentProcess, null, llm)
        assertEquals(tool.toolDefinition.name(), decorated.toolDefinition.name())
        assertEquals(tool.toolDefinition.inputSchema(), decorated.toolDefinition.inputSchema())
    }

    @Test
    fun `has same return`() {
        val tool = ToolCallbacks.from(SimpleTools()).single()
        val mockPlatformServices = mockk<PlatformServices>()
        every { mockPlatformServices.eventListener } returns EventSavingAgenticEventListener()
        val agentProcess = mockk<AgentProcess>()
        every { agentProcess.processContext.platformServices } returns mockPlatformServices
        every { agentProcess.processContext.onProcessEvent(any()) } answers {
            // Do nothing
        }
        every { agentProcess.processContext.platformServices } returns mockPlatformServices
        every { mockPlatformServices.operationScheduler } returns OperationScheduler.PRONTO
        val llm = LlmOptions()
        val decorated = tool.withEventPublication(agentProcess, null, llm)
        val rawResult = tool.call("{}")
        val decoratedRest = decorated.call("{}")
        assertEquals(rawResult, decoratedRest)
    }

    @Test
    fun `emits events`() {
        val ese = EventSavingAgenticEventListener()
        val tool = ToolCallbacks.from(SimpleTools()).single()
        val mockPlatformServices = mockk<PlatformServices>()
        every { mockPlatformServices.eventListener } returns ese
        val agentProcess = mockk<AgentProcess>()
        every { agentProcess.processContext.onProcessEvent(any()) } answers {
            ese.onProcessEvent(firstArg())
        }
        every { agentProcess.processContext.platformServices } returns mockPlatformServices
        every { mockPlatformServices.operationScheduler } returns OperationScheduler.PRONTO
        val llm = LlmOptions()
        val decorated = tool.withEventPublication(agentProcess, null, llm)
        decorated.call("{}")
        assertEquals(2, ese.processEvents.size)
        assertEquals(0, ese.platformEvents.size)
        val fce = ese.processEvents.filterIsInstance<ToolCallRequestEvent>().single()
        val fre = ese.processEvents.filterIsInstance<ToolCallResponseEvent>().single()
        assertEquals(decorated.toolDefinition.name(), fce.tool)
        assertEquals(decorated.toolDefinition.name(), fre.tool, decorated.toolDefinition.name())
        assertEquals(llm, fce.llmOptions)
        assertEquals(llm, fre.llmOptions)
    }

    class TakesDogTools {

        @Tool
        fun pat(
            dog: Dog,
        ): String {
            return dog.name
        }
    }

    data class Dog(val name: String, val breed: String)

    @Test
    fun `keeps types return`() {
        val tool = ToolCallbacks.from(TakesDogTools()).single()
        val mockPlatformServices = mockk<PlatformServices>()
        every { mockPlatformServices.eventListener } returns EventSavingAgenticEventListener()
        val agentProcess = mockk<AgentProcess>()
        every { agentProcess.processContext.platformServices } returns mockPlatformServices
        every { agentProcess.processContext.onProcessEvent(any()) } answers {
            // Do nothing
        }
        every { agentProcess.processContext.platformServices } returns mockPlatformServices
        every { mockPlatformServices.operationScheduler } returns OperationScheduler.PRONTO
        val llm = LlmOptions()
        val decorated = TypedToolCallback(tool).withEventPublication(agentProcess, null, llm)
        val payload = jacksonObjectMapper().writeValueAsString(Dog("duke", "golden retriever"))
        val rawInput = "{ \"dog\": $payload }"
        println(rawInput)
        val rawResult = tool.call(rawInput)
        val decoratedRest = decorated.call(rawInput)
        assertEquals(rawResult, decoratedRest)
    }

}


class TypedToolCallback(
    private val delegate: ToolCallback
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(toolInput: String): String {
        if (delegate is MethodToolCallback) {
            try {
                val toolMethod = delegate.javaClass.getDeclaredField("toolMethod")
                println(toolMethod)
            } catch (e: NoSuchFieldException) {
                // Ignore this
            }
        }
        return delegate.call(toolInput)
    }

}

class ToolCallBlockedException(
    val tool: String,
    val inputClass: Class<*>,
    val input: Any,
) : RuntimeException(
    "Tool call for $tool with input $input was blocked by a ToolCallMonitor."
)

class ToolCallResponseBlockedException(
    val tool: String,
    val inputClass: Class<*>,
    val input: Any,
    val toolOutput: String,
) : RuntimeException(
    "Tool call for $tool with input $input had response $toolOutput blocked by a ToolCallMonitor."
)

interface ToolCallMonitor<T> {

    val inputClass: Class<T>

    @Throws(ToolCallBlockedException::class)
    fun onToolCallRequest(tool: String, input: T, agentProcess: AgentProcess): T = input

    @Throws(ToolCallResponseBlockedException::class)
    fun onToolCallResponse(tool: String, input: T, toolOutput: String, agentProcess: AgentProcess): String = toolOutput
}