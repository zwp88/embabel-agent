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

import com.embabel.agent.experimental.primitive.Determination
import com.embabel.agent.spi.LlmCall
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.ZeroToOne
import org.springframework.ai.tool.ToolCallback


/**
 * Convenience interface for executing prompts.
 * User code should always use this interface to execute prompts.
 * A PromptRunner is immutable once constructed, and has determined
 * LLM and hyperparameters.
 * Thus, a PromptRunner can be reused within an action implementation.
 * A contextual facade to LlmOperations.
 * @see com.embabel.agent.spi.LlmOperations
 */
interface PromptRunner : LlmCall {

    /**
     * Additional objects with @Tool annotation for use in this PromptRunner
     */
    val toolObjects: List<Any>

    /**
     * Generate text
     */
    infix fun generateText(prompt: String): String =
        createObject(
            prompt = prompt,
            outputClass = String::class.java,
        )

    /**
     * Create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObject(
        prompt: String,
        outputClass: Class<T>,
    ): T

    /**
     * Try to create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObjectIfPossible(
        prompt: String,
        outputClass: Class<T>,
    ): T?

    fun evaluateCondition(
        condition: String,
        context: String,
        confidenceThreshold: ZeroToOne = 0.8,
    ): Boolean

    fun withToolGroup(toolGroup: String): PromptRunner

    /**
     * Add a tool object to the prompt runner.
     * @param toolObject the object to add. If it is null, nothing is done.
     * This is not an error
     */
    fun withToolObject(toolObject: Any?): PromptRunner

    fun withPromptContributor(promptContributor: PromptContributor): PromptRunner

}

inline infix fun <reified T> PromptRunner.createObject(prompt: String): T =
    createObject(prompt, T::class.java)

/** Method overloading is evil */
inline infix fun <reified T> PromptRunner.create(prompt: String): T =
    createObject(prompt, T::class.java)

inline fun <reified T> PromptRunner.createObjectIfPossible(prompt: String): T? =
    createObjectIfPossible(prompt, T::class.java)

interface LlmObjectCreationRequest : LlmCall {
    val requireResult: Boolean
    val outputClass: Class<*>
}

interface LlmCallRequest : LlmObjectCreationRequest {
    val prompt: String
}

/**
 * Exception thrown to indicate that a prompt should be executed.
 * This is not a real failure but meant to be intercepted by infrastructure.
 * It allows us to maintain strong typing.
 * @param requireResult whether to require a result or allow the LLM to
 * say that it cannot produce a result
 * @param llm llm to use. Contextual LLM will be used if not set
 */
sealed class ExecutePromptException(
    override val requireResult: Boolean,
    override val llm: LlmOptions? = null,
    override val outputClass: Class<*>,
    override val toolCallbacks: List<ToolCallback>,
    val toolObjects: List<Any>,
    override val promptContributors: List<PromptContributor>,
    override val generateExamples: Boolean?,
) : LlmObjectCreationRequest, RuntimeException(
    "Not a real failure but meant to be intercepted by infrastructure"
) {

    override val name = "ExecutePromptException"
}

class CreateObjectPromptException(
    override val prompt: String,
    requireResult: Boolean,
    llm: LlmOptions? = null,
    outputClass: Class<*>,
    override val toolGroups: Set<String>,
    toolCallbacks: List<ToolCallback>,
    toolObjects: List<Any>,
    promptContributors: List<PromptContributor>,
    generateExamples: Boolean? = null,
) : ExecutePromptException(
    requireResult = requireResult,
    llm = llm,
    outputClass = outputClass,
    toolCallbacks = toolCallbacks,
    toolObjects = toolObjects,
    promptContributors = promptContributors,
    generateExamples = generateExamples,
), LlmCallRequest

class EvaluateConditionPromptException(
    val condition: String,
    val context: String,
    val confidenceThreshold: ZeroToOne,
    requireResult: Boolean,
    llm: LlmOptions? = null,
    override val toolGroups: Set<String>,
    toolObjects: List<Any>,
    toolCallbacks: List<ToolCallback>,
    promptContributors: List<PromptContributor>,
    generateExamples: Boolean? = null,
) : ExecutePromptException(
    requireResult = requireResult,
    llm = llm,
    outputClass = Determination::class.java,
    toolCallbacks = toolCallbacks,
    toolObjects = toolObjects,
    promptContributors = promptContributors,
    generateExamples = generateExamples,
)
