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


/**
 * Interface for executing prompts
 * A PromptRunner interface allows more control over prompt execution
 * and reuse between different execution
 */
interface PromptRunner {

    infix fun generateText(prompt: String): String =
        createObject(prompt, String::class.java)

    /**
     * Create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObject(prompt: String, outputClass: Class<T>): T

    /**
     * Try to create an object of the given type using the given prompt and LLM options from context
     * (process context or implementing class).
     * Prompt is typically created within the scope of an
     * @Action method that provides access to
     * domain object instances, offering type safety.
     */
    fun <T> createObjectIfPossible(prompt: String, outputClass: Class<T>): T?

}

inline infix fun <reified T> PromptRunner.createObject(prompt: String): T =
    createObject(prompt, T::class.java)

inline fun <reified T> PromptRunner.createObjectIfPossible(prompt: String): T? =
    createObjectIfPossible(prompt, T::class.java)


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
