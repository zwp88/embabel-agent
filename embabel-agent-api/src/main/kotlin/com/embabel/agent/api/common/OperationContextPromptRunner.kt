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

import com.embabel.agent.core.support.safelyGetToolCallbacks
import com.embabel.agent.experimental.primitive.Determination
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.ZeroToOne
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback

/**
 * Uses the platform's LlmOperations to execute the prompt
 */
internal class OperationContextPromptRunner(
    private val context: OperationContext,
    override val llm: LlmOptions,
    override val toolGroups: Set<String>,
    override val toolCallbacks: List<ToolCallback>,
    override val toolObjects: List<Any>,
    override val promptContributors: List<PromptContributor>,
    override val generateExamples: Boolean?,
) : PromptRunner {

    override val name = "OperationContextPromptRunner"

    val action = (context as? ActionContext)?.action

    private fun idForPrompt(prompt: String, outputClass: Class<*>): InteractionId {
        return InteractionId("${context.operation.name}-${outputClass.name}")
    }

    override fun <T> createObject(
        prompt: String,
        outputClass: Class<T>,
    ): T {
        return context.processContext.createObject<T>(
            prompt = prompt,
            interaction = LlmInteraction(
                llm = llm,
                toolGroups = this.toolGroups + toolGroups,
                toolCallbacks = toolCallbacks + safelyGetToolCallbacks(toolObjects),
                promptContributors = promptContributors,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = action,
        )
    }

    override fun <T> createObjectIfPossible(
        prompt: String,
        outputClass: Class<T>,
    ): T? {
        val result = context.processContext.createObjectIfPossible<T>(
            prompt = prompt,
            interaction = LlmInteraction(
                llm = llm,
                toolGroups = this.toolGroups + toolGroups,
                toolCallbacks = toolCallbacks + safelyGetToolCallbacks(toolObjects),
                promptContributors = promptContributors,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = action,
        )
        if (result.isFailure) {
            loggerFor<OperationContextPromptRunner>().warn(
                "Failed to create object of type {} with prompt {}: {}",
                outputClass.name,
                prompt,
                result.exceptionOrNull()?.message,
            )
        }
        return result.getOrNull()
    }

    override fun evaluateCondition(
        condition: String,
        context: String,
        confidenceThreshold: ZeroToOne
    ): Boolean {
        val prompt =
            """
            Evaluate this condition given the context.
            Return "result": whether you think it is true, your confidence level from 0-1,
            and an explanation of what you base this on.

            # Condition
            $condition

            # Context
            $context
            """.trimIndent()
        val determination = createObject(
            prompt = prompt,
            outputClass = Determination::class.java,
        )
        loggerFor<OperationContextPromptRunner>().info(
            "Condition {}: determination from {} was {}",
            condition,
            llm.criteria,
            determination,
        )
        return determination.result && determination.confidence >= confidenceThreshold
    }
}
