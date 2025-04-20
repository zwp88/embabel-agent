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
package com.embabel.agent.spi

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.api.common.PromptContributor
import com.embabel.agent.core.Action
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.support.PromptContributorConsumer
import com.embabel.agent.event.LlmRequestEvent
import org.springframework.ai.tool.ToolCallback

/**
 * All prompt interactions through the platform need a unique id
 * This allows LLM interactions to be optimized by an AgentPlatform
 */
@JvmInline
value class InteractionId(val value: String) {

    override fun toString(): String = value
}

interface LlmCall: PromptContributorConsumer {
    val llm: LlmOptions?
    val toolCallbacks: List<ToolCallback>
    override val promptContributors: List<PromptContributor>
}

/**
 * Encapsulates an interaction with an LLM.
 * @param id Unique identifier for the interaction.
 * This is per action, not per process.
 * @param llm LLM options to use, specifying model and hyperparameters
 * @param toolCallbacks Tool callbacks to use for this interaction
 */
data class LlmInteraction(
    val id: InteractionId,
    override val llm: LlmOptions = LlmOptions(),
    override val toolCallbacks: List<ToolCallback> = emptyList(),
    override val promptContributors: List<PromptContributor> = emptyList(),
) : LlmCall

/**
 * Wraps LLM operations.
 * All LLM operations go through this,
 * allowing the AgentPlatform to mediate them.
 * Not directly for user code. Prefer PromptRunner
 * An LlmOperations implementation is responsible for resolving all relevant
 * tool callbacks for the current AgentProcess (in addition to those passed in directly),
 * and emitting events.
 * @see com.embabel.agent.api.common.PromptRunner
 */
interface LlmOperations {

    /**
     * Generate text
     * @param prompt Prompt to generate text from
     * @param interaction Llm options and tool callbacks to use, plus unique identifier
     * @param agentProcess Agent process we are running within
     * @param action Action we are running within if we are running within an action
     */
    fun generate(
        prompt: String,
        interaction: LlmInteraction,
        agentProcess: AgentProcess,
        action: Action?,
    ): String

    /**
     * Create an output object
     * @param prompt Function to generate the prompt from the input object
     * @param interaction Llm options and tool callbacks to use, plus unique identifier
     * @param outputClass Class of the output object
     * @param agentProcess Agent process we are running within
     * @param action Action we are running within if we are running within an action
     */
    fun <O> createObject(
        prompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): O

    /**
     * Try to create an output object.
     * @param prompt Function to generate the prompt from the input object
     * @param interaction Llm options and tool callbacks to use, plus unique identifier
     * @param outputClass Class of the output object
     * @param agentProcess Agent process we are running within
     * @param action Action we are running within if we are running within an action
     */
    fun <O> createObjectIfPossible(
        prompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): Result<O>

    /**
     * Low level transform
     * @param prompt
     * @param interaction The LLM call options
     * @param outputClass Class of the output object
     * @param llmRequestEvent Event already published for this request if one has been
     */
    fun <O> doTransform(
        prompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        llmRequestEvent: LlmRequestEvent<O>?,
    ): O

}
