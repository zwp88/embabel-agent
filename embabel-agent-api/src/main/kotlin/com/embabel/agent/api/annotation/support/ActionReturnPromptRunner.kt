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

import com.embabel.agent.api.common.ExecutePromptException
import com.embabel.agent.api.common.PromptRunner
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import org.springframework.ai.tool.ToolCallback

/**
 * Return an ambient prompt runner for use to return in an @Action method.
 */
fun using(
    llm: LlmOptions? = null,
    toolCallbacks: List<ToolCallback> = emptyList(),
    promptContributors: List<PromptContributor> = emptyList(),
): PromptRunner =
    ActionReturnPromptRunner(llm = llm, toolCallbacks = toolCallbacks, promptContributors = promptContributors)


val usingDefaultLlm: PromptRunner =
    ActionReturnPromptRunner(llm = null, toolCallbacks = emptyList(), promptContributors = emptyList())

/**
 * PromptRunner implementation that can be used to return a value
 * from an Action.
 */
private class ActionReturnPromptRunner(
    override val llm: LlmOptions?,
    override val toolCallbacks: List<ToolCallback>,
    override val promptContributors: List<PromptContributor>,
) : PromptRunner {

    override fun <T> createObject(
        prompt: String,
        outputClass: Class<T>,
    ): T {
        throw ExecutePromptException(
            prompt = prompt,
            llm = llm,
            requireResult = true,
            outputClass = outputClass,
            toolCallbacks = toolCallbacks,
            promptContributors = promptContributors,
        )
    }

    override fun <T> createObjectIfPossible(
        prompt: String,
        outputClass: Class<T>,
    ): T? {
        throw ExecutePromptException(
            prompt = prompt,
            llm = llm,
            requireResult = false,
            outputClass = outputClass,
            toolCallbacks = toolCallbacks,
            promptContributors = promptContributors,
        )
    }

}
