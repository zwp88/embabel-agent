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
package com.embabel.agent.annotation.support

import com.embabel.agent.primitive.BuildableLlmOptions
import com.embabel.agent.primitive.LlmOptions

/**
 * Run a prompt.
 */
object Prompt {

    /**
     * Run a prompt using default LLM
     */
    @JvmStatic
    fun <T> run(prompt: String): T {
        throw ExecutePromptException(prompt)
    }

    /**
     * Run a prompt with the given LLM and hyperparameters.
     * Typesafe.
     */
    @JvmStatic
    fun <T> run(prompt: String, llm: LlmOptions): T {
        throw ExecutePromptException(prompt, llm)
    }

}

/**
 * Allow more control over prompt execution
 * and reuse between different execution
 * With methods make it easier to use from Java in a builder style
 */
class PromptRunner(
    val llm: LlmOptions = LlmOptions(),
) {

    /**
     * Run a prompt. Type safe.
     */
    fun <T> run(prompt: String): T {
        throw ExecutePromptException(prompt, llm = llm)
    }

    fun withTemperature(temperature: Double): PromptRunner {
        return PromptRunner(BuildableLlmOptions(llm).copy(temperature = temperature))
    }

    fun withModel(model: String): PromptRunner {
        return PromptRunner(BuildableLlmOptions(llm).copy(model = model))
    }

}

/**
 * Exception thrown to indicate that a prompt should be executed.
 * This is not a real failure but meant to be intercepted by infrastructure.
 * It allows us to maintain strong typing.
 * @param prompt prompt to run
 * @param llm llm to use
 */
internal class ExecutePromptException(
    val prompt: String,
    val llm: LlmOptions? = null,
) : RuntimeException(
    "Not a real failure but meant to be intercepted by infrastructure: Generated prompt=[$prompt]"
)
