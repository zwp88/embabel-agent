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

import com.embabel.agent.core.Action
import com.embabel.agent.core.Blackboard
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.ToolGroupConsumer
import com.embabel.agent.core.support.safelyGetToolCallbacks
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.experimental.primitive.Determination
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.CurrentDate
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.Named
import com.embabel.common.core.types.ZeroToOne
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback

/**
 * Context for any operation. Exposes blackboard and process context.
 * @param processContext the process context
 */
interface OperationContext : Blackboard, ToolGroupConsumer {

    val processContext: ProcessContext

    /**
     * Action or operation that is being executed.
     */
    val operation: Named

    /**
     * Create a prompt runner for this context.
     * Application code should always go through this method to run LLM operations.
     * @param llm the LLM options to use
     * @param toolGroups extra local tool groups to use, in addition to those declared on the action if
     * we're in an action
     * @param toolCallbacks extra tool callbacks to use
     * @param promptContributors extra prompt contributors to use, in addition to those declared on the action if
     * we're in an action, or at agent level
     */
    fun promptRunner(
        llm: LlmOptions = LlmOptions(),
        toolGroups: Collection<String> = emptyList(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        promptContributors: List<PromptContributor?> = emptyList(),
    ): PromptRunner {
//        val updatedToolCallbacks = toolCallbacksOnDomainObjects().toMutableList()
        // Add any tool callbacks that are not already in the list
//        updatedToolCallbacks += toolCallbacks.filter { tc -> !updatedToolCallbacks.any { it.toolDefinition.name() == tc.toolDefinition.name() } }
        val promptContributorsToUse = promptContributors + CurrentDate()
        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = toolGroups,
            toolCallbacks = toolCallbacks,
            promptContributors = promptContributorsToUse.filterNotNull().distinctBy { it.promptContribution().role },
        )
    }

    companion object {
        operator fun invoke(
            processContext: ProcessContext,
            operation: Named,
            toolGroups: List<String>,
        ): OperationContext =
            SimpleOperationContext(
                processContext = processContext,
                operation = operation,
                toolGroups = toolGroups,
            )
    }
}

private class SimpleOperationContext(
    override val processContext: ProcessContext,
    override val operation: Named,
    override val toolGroups: Collection<String>,
) : OperationContext, Blackboard by processContext.agentProcess {
    override fun toString(): String {
        return "SimpleOperationContext(processContext=$processContext)"
    }
}

/**
 * Context for actions
 * @param processContext the process context
 * @param action the action being executed, if one is executing.
 * This is useful for getting tools etc.
 */
interface ActionContext : OperationContext {
    override val processContext: ProcessContext
    val action: Action?

    // TODO default LLM options from action
    override fun promptRunner(
        llm: LlmOptions,
        toolGroups: Collection<String>,
        toolCallbacks: List<ToolCallback>,
        promptContributors: List<PromptContributor?>,
    ): PromptRunner {
        val toolCallbacksToUse = toolCallbacks + toolCallbacksOnDomainObjects()
        val promptContributorsToUse = promptContributors + CurrentDate()

        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = this.toolGroups + toolGroups,
            toolCallbacks = toolCallbacksToUse,
            promptContributors = promptContributorsToUse.filterNotNull().distinctBy { it.promptContribution().role },
        )
    }

    fun agentPlatform() = processContext.platformServices.agentPlatform

    fun toolCallbacksOnDomainObjects(): List<ToolCallback>

}

/**
 * Uses the platform's LlmOperations to execute the prompt
 */
private class OperationContextPromptRunner(
    private val context: OperationContext,
    override val llm: LlmOptions,
    override val toolGroups: Collection<String>,
    override val toolCallbacks: List<ToolCallback>,
    override val promptContributors: List<PromptContributor>,
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
                toolCallbacks = toolCallbacks,
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
                toolCallbacks = toolCallbacks,
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

interface InputsActionContext : ActionContext {
    val inputs: List<Any>

    @Suppress("UNCHECKED_CAST")
    override fun toolCallbacksOnDomainObjects(): List<ToolCallback> {
        val instances = mutableListOf<Any>()
        inputs.forEach { input ->
            when (input) {
                is Array<*> -> instances += input.toList()
                is Collection<*> -> instances += input
                else -> instances += input
            }
        }
        return safelyGetToolCallbacks(instances)
    }

}

/**
 * Takes a single input
 */
interface InputActionContext<I> : InputsActionContext {
    val input: I

    override val inputs: List<Any> get() = listOfNotNull(input)
}

data class TransformationActionContext<I, O>(
    override val input: I,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputActionContext<I>, Blackboard by processContext.agentProcess,
    AgenticEventListener by processContext {

    override val toolGroups: Collection<String>
        get() = action?.toolGroups ?: emptyList()

    override val operation = action ?: error("No action in context")
}
