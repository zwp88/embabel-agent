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
package com.embabel.agent.core

import com.embabel.agent.core.primitive.LlmOptions
import org.springframework.ai.tool.ToolCallback

/**
 * Uses an LLM to transform an input into an output.
 * All LLM operations go through this,
 * allowing the AgentPlatform to mediate them.
 * An LlmTransformer is responsible for resolving all relevant
 * tool callbacks for the current AgentProcess, and emitting
 * events.
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
     * Perform a transformation from the given input object
     * to the output object which might not succeed.
     * @param input Input object
     * @param prompt Function to generate the prompt from the input object
     * @param llmOptions Options for the LLM. Controls model and hyperparameters
     * @param toolCallbacks Tool callbacks to use for this transformation.
     * @param outputClass Class of the output object
     * @param agentProcess Agent process we are running within
     * @param action Action we are running within if we are running within an action
     */
    fun <I, O> transformIfPossible(
        input: I,
        prompt: (input: I) -> String,
        llmOptions: LlmOptions = LlmOptions.Companion(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?,
    ): Result<O>

    /**
     * Low level transform, which can also be called
     * directly by user code.
     */
    fun <I, O> doTransform(
        input: I,
        literalPrompt: String,
        llmOptions: LlmOptions,
        allToolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): O

}
