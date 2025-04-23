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
import com.embabel.agent.event.AgentProcessFunctionCallRequestEvent
import com.embabel.agent.spi.ToolDecorator
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.util.time
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import java.time.Duration

class DefaultToolDecorator : ToolDecorator {

    override fun decorate(
        tool: ToolCallback,
        agentProcess: AgentProcess,
        llmOptions: LlmOptions,
    ): ToolCallback {
        return tool.withEventPublication(agentProcess, llmOptions)
    }
}

/**
 * HOF to decorate a ToolCallback to time the call and emit events.
 */
fun ToolCallback.withEventPublication(agentProcess: AgentProcess, llmOptions: LlmOptions): ToolCallback =
    this as? EventPublishingToolCallback ?: EventPublishingToolCallback(this, agentProcess, llmOptions)

class EventPublishingToolCallback(
    private val delegate: ToolCallback,
    private val agentProcess: AgentProcess,
    private val llmOptions: LlmOptions,
) : ToolCallback {

    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(toolInput: String): String {
        val functionCallRequestEvent = AgentProcessFunctionCallRequestEvent(
            agentProcess = agentProcess,
            llmOptions = llmOptions,
            function = delegate.toolDefinition.name(),
            toolInput = toolInput,
        )
        agentProcess.processContext.onProcessEvent(functionCallRequestEvent)
        val (response, millis) = time {
            delegate.call(toolInput)
        }
        agentProcess.processContext.onProcessEvent(
            functionCallRequestEvent.responseEvent(
                response = response,
                runningTime = Duration.ofMillis(millis),
            )
        )
        return response
    }
}
