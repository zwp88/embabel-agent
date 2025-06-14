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

import com.embabel.agent.api.common.support.OperationContextPromptRunner
import com.embabel.agent.core.*
import com.embabel.agent.event.AgenticEventListener
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.CurrentDate
import com.embabel.common.ai.prompt.PromptContributor

/**
 * Context for any operation. Exposes blackboard and process context.
 * @param processContext the process context
 */
interface OperationContext : Blackboard, ToolGroupConsumer {

    val processContext: ProcessContext

    /**
     * Action or operation that is being executed.
     */
    val operation: Operation

    /**
     * Create a prompt runner for this context.
     * Application code should always go through this method to run LLM operations.
     * @param llm the LLM options to use
     * @param toolGroups extra local tool groups to use, in addition to those declared on the action if
     * we're in an action
     * @param promptContributors extra prompt contributors to use, in addition to those declared on the action if
     * we're in an action, or at agent level
     */
    fun promptRunner(
        llm: LlmOptions = LlmOptions(),
        toolGroups: Set<String> = emptySet(),
        toolObjects: List<Any> = emptyList(),
        promptContributors: List<PromptContributor> = emptyList(),
        generateExamples: Boolean = false,
    ): PromptRunner {
        val promptContributorsToUse = (promptContributors + CurrentDate()).distinctBy { it.promptContribution().role }
        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = toolGroups,
            toolObjects = toolObjects,
            promptContributors = promptContributorsToUse,
            generateExamples = generateExamples,
        )
    }

    /**
     * Create a prompt runner for this context
     * that can be customized later.
     * Principally for use from Java.
     */
    fun promptRunner(): PromptRunner = promptRunner(
        llm = LlmOptions(),
    )

    companion object {
        operator fun invoke(
            processContext: ProcessContext,
            operation: Operation,
            toolGroups: Set<String>,
        ): OperationContext =
            MinimalOperationContext(
                processContext = processContext,
                operation = operation,
                toolGroups = toolGroups,
            )
    }
}

private class MinimalOperationContext(
    override val processContext: ProcessContext,
    override val operation: Operation,
    override val toolGroups: Set<String>,
) : OperationContext, Blackboard by processContext.agentProcess {
    override fun toString(): String {
        return "MinimalOperationContext(processContext=$processContext, operation=${operation.name})"
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
        toolObjects: List<Any>,
        promptContributors: List<PromptContributor>,
        generateExamples: Boolean,
    ): PromptRunner {
        val promptContributorsToUse = (promptContributors + CurrentDate()).distinctBy { it.promptContribution().role }

        return OperationContextPromptRunner(
            this,
            llm = llm,
            toolGroups = this.toolGroups + toolGroups,
            toolObjects = (toolObjects + domainObjectInstances()).distinct(),
            promptContributors = promptContributorsToUse,
            generateExamples = generateExamples,
        )
    }

    fun agentPlatform() = processContext.platformServices.agentPlatform

    /**
     * Return the domain object instances that are relevant for this action context.
     * They may expose tools.
     */
    fun domainObjectInstances(): List<Any>

}

/**
 * ActionContext with multiple inputs
 */
interface InputsActionContext : ActionContext {
    val inputs: List<Any>

    override fun domainObjectInstances(): List<Any> = inputs

}

/**
 * ActionContext with a single input
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
