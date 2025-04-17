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
package com.embabel.agent.core.support

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.core.Action
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.event.LlmRequestEvent
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmOperations
import com.embabel.agent.spi.support.forProcess
import com.embabel.common.util.time
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import java.time.Duration

/**
 * Convenient superclass for LlmOperations implementations,
 * which should normally extend this
 * Find all tool callbacks and decorate them to be aware of the platform
 * Also emits events.
 */
abstract class AbstractLlmOperations : LlmOperations {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun generate(
        prompt: String,
        interactionId: InteractionId,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        agentProcess: AgentProcess,
        action: Action?
    ): String = transform(
        input = Unit,
        prompt = { prompt },
        interactionId = interactionId,
        llmOptions = llmOptions,
        toolCallbacks = toolCallbacks,
        outputClass = String::class.java,
        agentProcess = agentProcess,
        action = action,
    )

    final override fun <I, O> transform(
        input: I,
        prompt: (I) -> String,
        interactionId: InteractionId,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): O {
        val (allToolCallbacks, literalPrompt, transformRequestEvent) = setup<I, O>(
            agentProcess,
            toolCallbacks,
            action,
            prompt,
            input,
            outputClass,
            llmOptions
        )
        val (response, ms) = time {
            doTransform(
                input = input,
                literalPrompt = literalPrompt,
                interactionId = interactionId,
                llmOptions = llmOptions,
                allToolCallbacks = allToolCallbacks.map { it.forProcess(agentProcess, llmOptions) },
                outputClass = outputClass,
            )
        }
        logger.debug("LLM response={}", response)
        agentProcess.processContext.onProcessEvent(
            transformRequestEvent.responseEvent(
                response = response,
                runningTime = Duration.ofMillis(ms),
            ),
        )
        return response
    }

    final override fun <I, O> transformIfPossible(
        input: I,
        prompt: (I) -> String,
        interactionId: InteractionId,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?
    ): Result<O> {
        val (allToolCallbacks, literalPrompt, transformRequestEvent) = setup<I, O>(
            agentProcess,
            toolCallbacks,
            action,
            prompt,
            input,
            outputClass,
            llmOptions
        )
        val (response, ms) = time {
            doTransformIfPossible(
                input = input,
                literalPrompt = literalPrompt,
                llmOptions = llmOptions,
                allToolCallbacks = allToolCallbacks.map { it.forProcess(agentProcess, llmOptions) },
                outputClass = outputClass,
            )
        }
        logger.debug("LLM response={}", response)
        agentProcess.processContext.onProcessEvent(
            transformRequestEvent.maybeResponseEvent(
                response = response,
                runningTime = Duration.ofMillis(ms),
            ),
        )
        return response
    }

    protected abstract fun <I, O> doTransformIfPossible(
        input: I,
        literalPrompt: String,
        llmOptions: LlmOptions,
        allToolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): Result<O>

    private fun <I, O> setup(
        agentProcess: AgentProcess,
        toolCallbacks: List<ToolCallback>,
        action: Action?,
        prompt: (I) -> String,
        input: I,
        outputClass: Class<O>,
        llmOptions: LlmOptions
    ): Triple<List<ToolCallback>, String, LlmRequestEvent<I, O>> {
        val toolGroupResolver = agentProcess.processContext.platformServices.agentPlatform.toolGroupResolver
        val allToolCallbacks =
            (toolCallbacks + agentProcess.processContext.agentProcess.agent.resolveToolCallbacks(
                toolGroupResolver,
            ) + (action?.resolveToolCallbacks(toolGroupResolver)
                ?: emptySet())).distinctBy { it.toolDefinition.name() }
        val literalPrompt = prompt(input)
        val transformRequestEvent = LlmRequestEvent(
            agentProcess = agentProcess,
            input = input,
            outputClass = outputClass,
            llmOptions = llmOptions,
            prompt = literalPrompt,
            tools = allToolCallbacks,
        )
        agentProcess.processContext.platformServices.eventListener.onProcessEvent(
            transformRequestEvent
        )
        return Triple(allToolCallbacks, literalPrompt, transformRequestEvent)
    }
}
