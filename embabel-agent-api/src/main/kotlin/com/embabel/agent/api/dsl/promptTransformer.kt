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
package com.embabel.agent.api.dsl

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.core.ActionQos
import com.embabel.agent.core.Condition
import com.embabel.agent.core.Transition
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.ZeroToOne
import org.springframework.ai.tool.ToolCallback

/**
 * Use in DSL to perform a transform
 */
inline fun <reified I, reified O : Any> promptTransformer(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = "it",
    outputVarName: String = "it",
    cost: ZeroToOne = 0.0,
    transitions: List<Transition> = emptyList(),
    toolGroups: Collection<String> = emptyList(),
    qos: ActionQos = ActionQos(),
    referencedInputProperties: Set<String>? = null,
    llm: LlmOptions = LlmOptions(),
    expectation: Condition? = null,
    canRerun: Boolean = false,
    toolCallbacks: List<ToolCallback> = emptyList(),
    noinline prompt: (actionContext: TransformationActionContext<I, O>) -> String,
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
        inputClass = I::class.java,
        outputClass = O::class.java,
        referencedInputProperties = referencedInputProperties,
        toolGroups = toolGroups,
    ) {
        it.promptRunner(
            llm = llm,
            toolCallbacks = toolCallbacks,
            promptContributors = emptyList(),
        ).createObject(
            prompt = prompt(it),
            outputClass = O::class.java,
        )
    }
}
