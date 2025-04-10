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
package com.embabel.agent.dsl

import com.embabel.agent.*
import com.embabel.agent.primitive.LlmOptions
import com.embabel.common.util.kotlin.loggerFor
import org.springframework.ai.tool.ToolCallback

inline fun <reified I, reified O : Any> promptTransformer(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = "it",
    outputVarName: String = "it",
    cost: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolGroups: Collection<String> = emptyList(),
    qos: Qos = Qos(),
    referencedInputProperties: Set<String>? = null,
    noinline prompt: (payload: TransformationPayload<I, O>) -> String,
    llmOptions: LlmOptions = LlmOptions(),
    expectation: Condition? = null,
    canRerun: Boolean = false,
    toolCallbacks: List<ToolCallback> = emptyList(),
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
        promptTransform(
            it = it,
            prompt = prompt,
            llmOptions = llmOptions,
            toolCallbacks = toolCallbacks,
        )
    }
}

inline fun <reified I, reified O : Any> llmTransform(
    input: I,
    processContext: ProcessContext,
    action: Action? = null,
    noinline prompt: (TransformationPayload<I, O>) -> String,
    llmOptions: LlmOptions = LlmOptions(),
    toolCallbacks: List<ToolCallback> = emptyList(),
): O = promptTransform(
    it = TransformationPayload(
        input = input,
        processContext = processContext,
        inputClass = I::class.java,
        outputClass = O::class.java,
        action = action,
    ),
    prompt = prompt,
    llmOptions = llmOptions,
    toolCallbacks = toolCallbacks,
)

inline fun <I, reified O : Any> promptTransform(
    it: TransformationPayload<I, O>,
    noinline prompt: (TransformationPayload<I, O>) -> String,
    llmOptions: LlmOptions = LlmOptions(),
    toolCallbacks: List<ToolCallback> = emptyList(),
): O {

    val literalPrompt = prompt(it)
    loggerFor<Transformer<I, O>>().debug("Using LLM to transform input of type ${it.inputClass.simpleName} to ${it.outputClass.simpleName}")

    return it.transform(
        input = it.input,
        prompt = { literalPrompt },
        llmOptions = llmOptions,
        toolCallbacks = toolCallbacks,
        outputClass = O::class.java,
    )
}
