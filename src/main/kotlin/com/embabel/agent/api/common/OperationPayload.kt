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

import com.embabel.agent.core.Action
import com.embabel.agent.core.Blackboard
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import org.springframework.ai.tool.ToolCallback

/**
 * Payload for any operation
 * @param processContext the process context
 * @param action the action being executed, if one is executing.
 * This is useful for getting tools etc.
 */
interface OperationPayload : Blackboard {
    val processContext: ProcessContext
    val action: Action?

    // TODO default LLM options from action
    fun promptRunner(llm: LlmOptions): PromptRunner =
        OperationPayloadPromptRunner(this, llm)

    fun agentPlatform() = processContext.platformServices.agentPlatform

}

/**
 * Uses the platform's LlmOperations to execute the prompt
 * Merely a convenience.
 */
private class OperationPayloadPromptRunner(
    private val payload: OperationPayload,
    private val llm: LlmOptions,
) : PromptRunner {

    private fun idForPrompt(prompt: String, outputClass: Class<*>): InteractionId {
        return InteractionId("${payload.action?.name}-${outputClass.name}")
    }

    override fun <T> createObject(
        prompt: String,
        outputClass: Class<T>,
        toolCallbacks: List<ToolCallback>,
    ): T {
        return payload.processContext.createObject<T>(
            prompt = prompt,
            interaction = LlmInteraction(
                llm = llm,
                toolCallbacks = toolCallbacks,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = payload.processContext.agentProcess,
            action = payload.action,
        )
    }

    override fun <T> createObjectIfPossible(
        prompt: String,
        outputClass: Class<T>,
        toolCallbacks: List<ToolCallback>,
    ): T? {
        return payload.processContext.createObjectIfPossible<T>(
            prompt = prompt,
            interaction = LlmInteraction(
                llm = llm,
                toolCallbacks = toolCallbacks,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = payload.processContext.agentProcess,
            action = payload.action,
        ).getOrNull()
    }
}

interface InputPayload<I> : OperationPayload {
    val input: I

}

data class TransformationPayload<I, O>(
    override val input: I,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputPayload<I>, Blackboard by processContext.agentProcess,
    AgenticEventListener by processContext
