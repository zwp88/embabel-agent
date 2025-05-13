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
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.CurrentDate
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.Named
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
        toolGroups: Set<String> = emptySet(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        promptContributors: List<PromptContributor> = emptyList(),
        generateExamples: Boolean = false,
    ): PromptRunner {
        val promptContributorsToUse = (promptContributors + CurrentDate()).distinctBy { it.promptContribution().role }
        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = toolGroups,
            toolCallbacks = toolCallbacks,
            promptContributors = promptContributorsToUse,
            generateExamples = generateExamples,
        )
    }

    companion object {
        operator fun invoke(
            processContext: ProcessContext,
            operation: Named,
            toolGroups: Set<String>,
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
    override val toolGroups: Set<String>,
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
        toolGroups: Set<String>,
        toolCallbacks: List<ToolCallback>,
        promptContributors: List<PromptContributor>,
        generateExamples: Boolean,
    ): PromptRunner {
        val toolCallbacksToUse =
            (toolCallbacks + toolCallbacksOnDomainObjects()).distinctBy { it.toolDefinition.name() }
        val promptContributorsToUse = (promptContributors + CurrentDate()).distinctBy { it.promptContribution().role }

        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = this.toolGroups + toolGroups,
            toolCallbacks = toolCallbacksToUse,
            promptContributors = promptContributorsToUse,
            generateExamples = generateExamples,
        )
    }

    fun agentPlatform() = processContext.platformServices.agentPlatform

    fun toolCallbacksOnDomainObjects(): List<ToolCallback>

}

interface InputsActionContext : ActionContext {
    val inputs: List<Any>

    @Suppress("UNCHECKED_CAST")
    override fun toolCallbacksOnDomainObjects(): List<ToolCallback> {
        val instances = mutableListOf<Any>()
        inputs.forEach { input ->
            instances += when (input) {
                is Array<*> -> input.toList()
                is Collection<*> -> input
                else -> input
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
    override val action: Action,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputActionContext<I>, Blackboard by processContext.agentProcess,
    AgenticEventListener by processContext {

    override val toolGroups: Set<String>
        get() = action.toolGroups

    override val operation = action
}
