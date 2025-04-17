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

import com.embabel.agent.core.Action
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.event.LlmRequestEvent
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.agent.spi.support.withEventPublication
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
        interaction: LlmInteraction,
        agentProcess: AgentProcess,
        action: Action?
    ): String = transform(
        input = Unit,
        prompt = { prompt },
        interaction = interaction,
        outputClass = String::class.java,
        agentProcess = agentProcess,
        action = action,
    )

    final override fun <I, O> transform(
        input: I,
        prompt: (I) -> String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): O {
        val (allToolCallbacks, literalPrompt, transformRequestEvent) = setup<I, O>(
            agentProcess = agentProcess,
            interaction = interaction,
            action = action,
            prompt = prompt,
            input = input,
            outputClass = outputClass,
        )
        val (response, ms) = time {
            doTransform(
                input = input,
                literalPrompt = literalPrompt,
                interaction = interaction.copy(toolCallbacks = allToolCallbacks.map {
                    it.withEventPublication(
                        agentProcess,
                        interaction.llm
                    )
                }),
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
        interaction: LlmInteraction,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?
    ): Result<O> {
        val (allToolCallbacks, literalPrompt, transformRequestEvent) = setup<I, O>(
            agentProcess = agentProcess,
            interaction = interaction,
            action = action,
            prompt = prompt,
            input = input,
            outputClass = outputClass,
        )
        val (response, ms) = time {
            doTransformIfPossible(
                input = input,
                literalPrompt = literalPrompt,
                interaction = interaction.copy(toolCallbacks = allToolCallbacks.map {
                    it.withEventPublication(
                        agentProcess,
                        interaction.llm
                    )
                }),
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
        interaction: LlmInteraction,
        outputClass: Class<O>,
    ): Result<O>

    private fun <I, O> setup(
        agentProcess: AgentProcess,
        interaction: LlmInteraction,
        action: Action?,
        prompt: (I) -> String,
        input: I,
        outputClass: Class<O>,
    ): Triple<List<ToolCallback>, String, LlmRequestEvent<I, O>> {
        val toolGroupResolver = agentProcess.processContext.platformServices.agentPlatform.toolGroupResolver
        val allToolCallbacks =
            (interaction.toolCallbacks + agentProcess.processContext.agentProcess.agent.resolveToolCallbacks(
                toolGroupResolver,
            ) + (action?.resolveToolCallbacks(toolGroupResolver)
                ?: emptySet())).distinctBy { it.toolDefinition.name() }
        val literalPrompt = prompt(input)
        val transformRequestEvent = LlmRequestEvent(
            agentProcess = agentProcess,
            input = input,
            outputClass = outputClass,
            interaction = interaction.copy(toolCallbacks = allToolCallbacks),
            prompt = literalPrompt,
        )
        agentProcess.processContext.platformServices.eventListener.onProcessEvent(
            transformRequestEvent
        )
        return Triple(allToolCallbacks, literalPrompt, transformRequestEvent)
    }
}
