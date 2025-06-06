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

import com.embabel.agent.api.common.support.TransformationAction
import com.embabel.agent.core.Action
import com.embabel.agent.core.Agent
import com.embabel.agent.core.resultOfType

fun <I, O : Any> asTransformation(agent: Agent, outputClass: Class<O>) = Transformation<I, O> {
    val childAgentProcess = it.agentPlatform().createChildProcess(
        agent = agent,
        parentAgentProcess = it.processContext.agentProcess,
    )
    val childProcessResult = childAgentProcess.run()
    childProcessResult.resultOfType(outputClass)
}

/**
 * Creates a transformation action from an agent
 */
inline fun <reified I, reified O : Any> Agent.asTransformation() = Transformation<I, O> {
    val childAgentProcess = it.agentPlatform().createChildProcess(
        agent = this,
        parentAgentProcess = it.processContext.agentProcess,
    )
    val childProcessResult = childAgentProcess.run()
    childProcessResult.resultOfType()
}

fun <I, O : Any> asAction(agent: Agent, inputClass: Class<I>, outputClass: Class<O>): Action =
    agentTransformer<I, O>(agent, inputClass, outputClass)


/**
 * Expose this agent as an action of the given transformation type
 */
inline fun <reified I, reified O : Any> Agent.asAction(): Action =
    agentTransformer<I, O>(this)

fun <I, O : Any> asAction(agentName: String, inputClass: Class<I>, outputClass: Class<O>): Action {
    return TransformationAction(
        name = "@action-${agentName}",
        description = "@action-${agentName}",
        pre = emptyList(),
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        inputClass = inputClass,
        outputClass = outputClass,
        toolGroups = emptySet(),
    ) {
        val agent: Agent = it.processContext.platformServices.agentPlatform.agents().singleOrNull() {
            it.name == agentName
        } ?: throw IllegalArgumentException(
            "Agent '$agentName' not found: Known agents:\n\t${
                it.processContext.platformServices.agentPlatform.agents().joinToString("\n\t") { it.name }
            }"
        )
        asTransformation<I, O>(agent, outputClass).transform(it)
    }
}

inline fun <reified I, reified O : Any> asAction(agentName: String): Action =
    asAction<I, O>(agentName, I::class.java, O::class.java)

inline fun <reified I, reified O : Any> agentTransformer(agent: Agent): Action =
    agentTransformer<I, O>(agent, I::class.java, O::class.java)

fun <I, O : Any> agentTransformer(
    agent: Agent, inputClass: Class<I>,
    outputClass: Class<O>,
): Action {
    return TransformationAction(
        name = "@action-${agent.name}",
        description = "@action-${agent.name}",
        pre = emptyList(),
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        inputClass = inputClass,
        outputClass = outputClass,
        toolGroups = emptySet(),
    ) {
        val tf: Transformation<I, O> = asTransformation<I, O>(agent, outputClass)
        tf.transform(it)
    }
}


/**
 * Transformation function signature
 */
fun interface Transformation<I, O> {
    fun transform(context: TransformationActionContext<I, O>): O?
}
