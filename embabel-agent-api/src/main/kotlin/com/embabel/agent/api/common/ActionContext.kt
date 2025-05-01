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
import com.embabel.agent.core.support.safelyGetToolCallbacks
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback

/**
 * Context for any operation. Exposes blackboard and process context.
 * @param processContext the process context
 */
interface OperationContext : Blackboard {
    val processContext: ProcessContext

    companion object {
        operator fun invoke(processContext: ProcessContext): OperationContext =
            SimpleOperationContext(processContext)
    }
}

private class SimpleOperationContext(
    override val processContext: ProcessContext,
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
    fun promptRunner(
        llm: LlmOptions,
        toolCallbacks: List<ToolCallback> = emptyList(),
        promptContributors: List<PromptContributor?> = emptyList(),
    ): PromptRunner {
        val updatedToolCallbacks = toolCallbacksOnDomainObjects().toMutableList()
        // Add any tool callbacks that are not already in the list
        updatedToolCallbacks += toolCallbacks.filter { tc -> !updatedToolCallbacks.any { it.toolDefinition.name() == tc.toolDefinition.name() } }
        return ActionContextPromptRunner(
            this,
            llm = llm,
            toolCallbacks = updatedToolCallbacks,
            promptContributors = promptContributors.filterNotNull(),
        )
    }

    fun agentPlatform() = processContext.platformServices.agentPlatform

    fun toolCallbacksOnDomainObjects(): List<ToolCallback>

}

/**
 * Uses the platform's LlmOperations to execute the prompt
 * Merely a convenience.
 */
private class ActionContextPromptRunner(
    private val context: ActionContext,
    override val llm: LlmOptions,
    override val toolCallbacks: List<ToolCallback>,
    override val promptContributors: List<PromptContributor>,
) : PromptRunner {

    private fun idForPrompt(prompt: String, outputClass: Class<*>): InteractionId {
        return InteractionId("${context.action?.name}-${outputClass.name}")
    }

    override fun <T> createObject(
        prompt: String,
        outputClass: Class<T>,
    ): T {
        return context.processContext.createObject<T>(
            prompt = prompt,
            interaction = LlmInteraction(
                llm = llm,
                toolCallbacks = toolCallbacks,
                promptContributors = promptContributors,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = context.action,
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
                toolCallbacks = toolCallbacks,
                promptContributors = promptContributors,
                id = idForPrompt(prompt, outputClass),
            ),
            outputClass = outputClass,
            agentProcess = context.processContext.agentProcess,
            action = context.action,
        )
        if (result.isFailure) {
            loggerFor<ActionContextPromptRunner>().warn(
                "Failed to create object of type {} with prompt {}: {}",
                outputClass.name,
                prompt,
                result.exceptionOrNull()?.message,
            )
        }
        return result.getOrNull()
    }
}

interface InputActionContext<I> : ActionContext {
    val input: I

    override fun toolCallbacksOnDomainObjects(): List<ToolCallback> {
        val inp = input
        val instances: Collection<*> = when (inp) {
            is Array<*> -> inp.toList()
            is Collection<*> -> input as Collection<*>
            else -> listOf(input)
        }
        return safelyGetToolCallbacks(instances as Collection<Any>)
    }

}

data class TransformationActionContext<I, O>(
    override val input: I,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputActionContext<I>, Blackboard by processContext.agentProcess,
    AgenticEventListener by processContext
