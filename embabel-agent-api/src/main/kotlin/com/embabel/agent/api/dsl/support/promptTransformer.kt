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
package com.embabel.agent.api.dsl.support

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.core.ActionQos
import com.embabel.agent.core.Condition
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.Transition
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.ZeroToOne
import org.springframework.ai.tool.ToolCallback

/**
 * Supports AgentBuilder. Not fur direct use in user code.
 */
fun <I, O : Any> promptTransformer(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = IoBinding.DEFAULT_BINDING,
    outputVarName: String = IoBinding.DEFAULT_BINDING,
    inputClass: Class<I>,
    outputClass: Class<O>,
    cost: ZeroToOne = 0.0,
    transitions: List<Transition> = emptyList(),
    toolGroups: Collection<String> = emptyList(),
    qos: ActionQos = ActionQos(),
    referencedInputProperties: Set<String>? = null,
    llm: LlmOptions = LlmOptions(),
    promptContributors: List<PromptContributor> = emptyList(),
    expectation: Condition? = null,
    canRerun: Boolean = false,
    toolCallbacks: Collection<ToolCallback> = emptyList(),
    prompt: (actionContext: TransformationActionContext<I, O>) -> String,
): Transformer<I, O> {
    val expectationTransition = expectation?.let {
        Transition(
            to = name,
            condition = name,
        )
    }
    return Transformer<I, O>(
        name = name,
        description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        cost = cost,
        transitions = (transitions + expectationTransition).filterNotNull(),
        qos = qos,
        canRerun = canRerun,
        inputVarName = inputVarName,
        outputVarName = outputVarName,
        inputClass = inputClass,
        outputClass = outputClass,
        referencedInputProperties = referencedInputProperties,
        toolGroups = toolGroups,
    ) {
        it.promptRunner(
            llm = llm,
            toolCallbacks = toolCallbacks.toList(),
            promptContributors = promptContributors,
        ).createObject(
            prompt = prompt(it),
            outputClass = outputClass,
        )
    }
}
