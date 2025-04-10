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
package com.embabel.agent.support

import com.embabel.agent.AgentProcess
import com.embabel.agent.event.AgentProcessFunctionCallRequestEvent
import com.embabel.agent.primitive.LlmOptions
import com.embabel.common.util.time
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import java.time.Duration

/**
 * Decorate a ToolCallback to be aware of the AgentProcess
 * and time the call and emit events.
 */
fun ToolCallback.forProcess(agentProcess: AgentProcess, llmOptions: LlmOptions): ToolCallback =
    this as? AgentProcessAwareToolCallback ?: AgentProcessAwareToolCallback(this, agentProcess, llmOptions)

private class AgentProcessAwareToolCallback(
    private val delegate: ToolCallback,
    private val agentProcess: AgentProcess,
    private val llmOptions: LlmOptions,
) : ToolCallback {

    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(toolInput: String): String {
        val arguments = mapOf("json" to toolInput)

        val functionCallRequestEvent = AgentProcessFunctionCallRequestEvent(
            agentProcess = agentProcess,
            llmOptions = llmOptions,
            function = delegate.toolDefinition.name(),
            arguments = arguments,
        )
        agentProcess.processContext.platformServices.eventListener.onProcessEvent(functionCallRequestEvent)
        val (response, millis) = time {
            delegate.call(toolInput)
        }
        agentProcess.processContext.platformServices.eventListener.onProcessEvent(
            functionCallRequestEvent.responseEvent(
                response = response,
                runningTime = Duration.ofMillis(millis),
            )
        )
        return response
    }
}
