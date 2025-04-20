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

import com.embabel.agent.api.common.Transformation
import com.embabel.agent.api.common.TransformationPayload
import com.embabel.agent.core.*
import com.embabel.agent.core.support.AbstractAction
import org.springframework.ai.tool.ToolCallback
import java.lang.reflect.Modifier


inline fun <reified I, reified O : Any> transformer(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = "it",
    outputVarName: String? = "it",
    cost: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolCallbacks: List<ToolCallback> = emptyList(),
    toolGroups: Collection<String> = emptySet(),
    qos: Qos = Qos(),
    referencedInputProperties: Set<String>? = null,
    block: Transformation<I, O>,
): Action {
    return Transformer(
        name = name,
        description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        cost = cost,
        transitions = transitions,
        qos = qos,
        inputVarName = inputVarName,
        outputVarName = outputVarName,
        inputClass = I::class.java,
        outputClass = O::class.java,
        referencedInputProperties = referencedInputProperties,
        toolCallbacks = toolCallbacks,
        toolGroups = toolGroups,
        block = block,
    )
}


/**
 * Create input binding(s) for the given variable name and type.
 * Allow for megazords (Aggregations) and decompose them into their individual fields.
 */
fun expandInputBindings(
    inputVarName: String,
    inputClass: Class<*>
): Set<IoBinding> {
    if (com.embabel.agent.domain.special.Aggregation::class.java.isAssignableFrom(inputClass)) {
        return inputClass.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
            .map { field ->
                // Make field accessible if it's private
                field.isAccessible = true
                IoBinding(
                    type = field.type
                )
            }
            .toSet()
    }

    // Default case: just return the input itself
    return setOf(IoBinding(inputVarName, inputClass.name))
}

class Transformer<I, O>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    canRerun: Boolean = false,
    qos: Qos = Qos(),
    private val inputClass: Class<I>,
    private val outputClass: Class<O>,
    private val inputVarName: String = "it",
    private val outputVarName: String? = "it",
    private val referencedInputProperties: Set<String>? = null,
    toolCallbacks: Collection<ToolCallback> = emptyList(),
    toolGroups: Collection<String>,
    private val block: Transformation<I, O>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs = expandInputBindings(inputVarName, inputClass),
    outputs = if (outputVarName == null) emptySet() else setOf(IoBinding(outputVarName, outputClass.name)),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    canRerun = canRerun,
    qos = qos,
) {

    override val domainTypes
        get() = setOf(inputClass, outputClass)

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, SchemaType>,
        action: Action,
    ): ActionStatus = ActionRunner.execute(processContext) {
        val input = processContext.getValue(inputVarName, inputClass.name) as I
        val output = block.transform(
            TransformationPayload(
                input = input,
                processContext = processContext,
                action = this,
                inputClass = inputClass as Class<I>,
                outputClass = outputClass,
            )
        )
        if (output != null) {
            if (outputVarName != null) {
                processContext.blackboard[outputVarName] = output
            } else {
                processContext.blackboard += output
            }
        }
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClass.declaredFields.map { it.name }.toSet()
            fields
        }
    }
}
