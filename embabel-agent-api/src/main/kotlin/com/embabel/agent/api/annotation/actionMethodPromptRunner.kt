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
package com.embabel.agent.api.annotation

import com.embabel.agent.api.annotation.support.MethodReturnPromptRunner
import com.embabel.agent.api.common.PromptRunner
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import org.springframework.ai.tool.ToolCallback

/**
 * Return a prompt runner for use to return in an @Action method, specifying
 * LLM options.
 * @param llm specification of LLM to use
 * @param toolGroups tool groups to request
 * @param toolCallbacks individual tools to expose
 * @param toolObjects objects with @Tool annotations to expose to the LLM.
 * Note that domain objects passed into the @Action method will automatically
 * expose any @Tool methods.
 * @promptContributors promptContributors to expose
 * @generateExamples whether to override default example generation
 */
@JvmOverloads
fun using(
    llm: LlmOptions? = null,
    toolGroups: Set<String> = emptySet(),
    toolCallbacks: List<ToolCallback> = emptyList(),
    toolObjects: List<Any> = emptyList(),
    promptContributors: List<PromptContributor> = emptyList(),
    generateExamples: Boolean? = null,
): PromptRunner =
    MethodReturnPromptRunner(
        llm = llm,
        toolGroups = toolGroups,
        toolCallbacks = toolCallbacks,
        toolObjects = toolObjects,
        promptContributors = promptContributors,
        generateExamples = generateExamples,
    )

/**
 * Convenience method to return a prompt runner for use to return in an @Action method
 * that uses the given model with default hyperparameters
 * @param model name of LLM to use
 * @param toolGroups tool groups to request
 * @param toolCallbacks individual tools to expose
 * @param toolObjects objects with @Tool annotations to expose to the LLM.
 * Note that domain objects passed into the @Action method will automatically
 * expose any @Tool methods.
 * @promptContributors promptContributors to expose
 * @generateExamples whether to override default example generation
 */
@JvmOverloads
fun usingModel(
    model: String,
    toolGroups: Set<String> = emptySet(),
    toolCallbacks: List<ToolCallback> = emptyList(),
    toolObjects: List<Any> = emptyList(),
    promptContributors: List<PromptContributor> = emptyList(),
    generateExamples: Boolean? = null,
): PromptRunner =
    MethodReturnPromptRunner(
        llm = LlmOptions(model = model),
        toolGroups = toolGroups,
        toolCallbacks = toolCallbacks,
        toolObjects = toolObjects,
        promptContributors = promptContributors,
        generateExamples = generateExamples,
    )

/**
 * Return a PromptRunner instance for use in @Action methods
 * that uses default LLM and hyperparameters.
 */
val usingDefaultLlm: PromptRunner =
    MethodReturnPromptRunner(
        llm = null,
        toolGroups = emptySet(),
        toolCallbacks = emptyList(),
        toolObjects = emptyList(),
        promptContributors = emptyList(),
        generateExamples = null,
    )
