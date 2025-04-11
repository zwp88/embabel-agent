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
package com.embabel.agent

import com.embabel.agent.event.LlmTransformRequestEvent
import com.embabel.agent.primitive.LlmOptions
import com.embabel.agent.spi.support.forProcess
import com.embabel.common.util.time
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import java.time.Duration

/**
 * Uses an LLM to transform an input into an output.
 * All LLM operations go through this,
 * allowing the AgentPlatform to mediate them.
 */
interface LlmTransformer {

    /**
     * Perform a transformation from the given input object
     * to the output object.
     * @param input Input object
     * @param prompt Function to generate the prompt from the input object
     * @param llmOptions Options for the LLM. Controls model and hyperparameters
     * @param toolCallbacks Tool callbacks to use for this transformation.
     * @param outputClass Class of the output object
     * @param agentProcess Agent process we are running within
     * @param action Action we are running within if we are running within an action
     */
    fun <I, O> transform(
        input: I,
        prompt: (input: I) -> String,
        llmOptions: LlmOptions = LlmOptions.Companion(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): O

    /**
     * Low level transform
     */
    fun <I, O> doTransform(
        input: I,
        literalPrompt: String,
        llmOptions: LlmOptions,
        allToolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): O
}

/**
 * Find all tool callbacks and decorate them to be aware of the platform
 * All LlmTransformers should extend this.
 * Also emits events.
 */
abstract class AbstractLlmTransformer : LlmTransformer {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    final override fun <I, O> transform(
        input: I,
        prompt: (I) -> String,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): O {
        val toolGroupResolver = agentProcess.processContext.platformServices.agentPlatform.toolGroupResolver
        val allToolCallbacks =
            (toolCallbacks + agentProcess.processContext.agentProcess.agent.resolveToolCallbacks(
                toolGroupResolver,
            ) + (action?.resolveToolCallbacks(toolGroupResolver)
                ?: emptySet())).distinctBy { it.toolDefinition.name() }
        val literalPrompt = prompt(input)
        val transformRequestEvent = LlmTransformRequestEvent(
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
        val (response, ms) = time {
            doTransform(
                input = input,
                literalPrompt = literalPrompt,
                llmOptions = llmOptions,
                allToolCallbacks = allToolCallbacks.map { it.forProcess(agentProcess, llmOptions) },
                outputClass = outputClass,
            )
        }
        logger.debug("LLM response={}", response)
        agentProcess.processContext.platformServices.eventListener.onProcessEvent(
            transformRequestEvent.responseEvent(
                response = response,
                runningTime = Duration.ofMillis(ms),
            ),
        )
        return response
    }
}
