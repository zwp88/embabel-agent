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
 * Return an ambient prompt runner
 */
fun using(llm: LlmOptions? = null): PromptRunner {
    return ActionReturnPromptRunner(llm)
}

val usingDefaultLlm: PromptRunner = ActionReturnPromptRunner(llm = null)

/**
 * PromptRunner implementation that can be used to return a value
 */
private class ActionReturnPromptRunner(
    val llm: LlmOptions?,
) : PromptRunner {

    override fun <T> createObject(prompt: String, outputClass: Class<T>): T {
        throw ExecutePromptException(prompt = prompt, llm = llm, requireResult = true)
    }

    override fun <T> createObjectIfPossible(prompt: String, outClass: Class<T>): T? {
        throw ExecutePromptException(prompt = prompt, llm = llm, requireResult = true)
    }

}
