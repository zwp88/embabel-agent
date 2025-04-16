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
package com.embabel.agent.api.annotation.support

import com.embabel.agent.core.primitive.BuildableLlmOptions
import com.embabel.agent.core.primitive.LlmOptions

/**
 * Interface for executing prompts
 */
interface PromptRunner {

    /**
     * Create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObject(prompt: String): T

    /**
     * Try to create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObjectIfPossible(prompt: String): T?

    companion object {

        operator fun invoke(llm: LlmOptions? = null): BuildablePromptRunner {
            return BuildablePromptRunner(llm)
        }
    }

}

/**
 * Run a prompt.
 */
object Prompt : PromptRunner {

    override fun <T> createObject(prompt: String): T {
        throw ExecutePromptException(prompt = prompt, requireResult = true)
    }

    override fun <T> createObjectIfPossible(prompt: String): T? {
        throw ExecutePromptException(prompt = prompt, requireResult = true)
    }

    /**
     * Run a prompt with the given LLM and hyperparameters.
     * Typesafe.
     */
    @JvmStatic
    fun <T> run(prompt: String, llm: LlmOptions): T {
        throw ExecutePromptException(prompt = prompt, llm = llm, requireResult = true)
    }

}

/**
 * Allow more control over prompt execution
 * and reuse between different execution
 * With methods make it easier to use from Java in a builder style
 */
class BuildablePromptRunner(
    val llm: LlmOptions? = null,
) : PromptRunner {

    override fun <T> createObject(prompt: String): T {
        throw ExecutePromptException(prompt = prompt, llm = llm, requireResult = true)
    }

    override fun <T> createObjectIfPossible(prompt: String): T? {
        throw ExecutePromptException(prompt = prompt, llm = llm, requireResult = true)
    }

    fun withTemperature(temperature: Double): PromptRunner {
        return BuildablePromptRunner(BuildableLlmOptions(llm ?: LlmOptions()).copy(temperature = temperature))
    }

    fun withModel(model: String): PromptRunner {
        return BuildablePromptRunner(BuildableLlmOptions(llm ?: LlmOptions()).copy(model = model))
    }

}

/**
 * Exception thrown to indicate that a prompt should be executed.
 * This is not a real failure but meant to be intercepted by infrastructure.
 * It allows us to maintain strong typing.
 * @param prompt prompt to run
 * @param requireResult whether to require a result or allow the LLM to
 * say that it cannot produce a result
 * @param llm llm to use. Contextual LLM will be used if not set
 */
internal class ExecutePromptException(
    val prompt: String,
    val requireResult: Boolean,
    val llm: LlmOptions? = null,
) : RuntimeException(
    "Not a real failure but meant to be intercepted by infrastructure: Generated prompt=[$prompt]"
)
