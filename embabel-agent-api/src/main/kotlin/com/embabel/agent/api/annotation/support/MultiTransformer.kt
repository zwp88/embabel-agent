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
package com.embabel.agent.api.annotation.support

import com.embabel.agent.api.common.Transformation
import com.embabel.agent.api.common.TransformationPayload
import com.embabel.agent.core.*
import com.embabel.agent.core.support.AbstractAction
import org.springframework.ai.tool.ToolCallback

/**
 * Transformer that can take multiple inputs.
 */
internal class MultiTransformer<O : Any>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    canRerun: Boolean = false,
    qos: ActionQos = ActionQos(),
    inputs: Set<IoBinding>,
    private val inputClasses: List<Class<*>>,
    private val outputClass: Class<O>,
    private val outputVarName: String? = "it",
    private val referencedInputProperties: Set<String>? = null,
    override val toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String>,
    private val block: Transformation<List<Any>, O>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs,
    outputs = if (outputVarName == null) emptySet() else setOf(IoBinding(outputVarName, outputClass.name)),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    canRerun = canRerun,
    qos = qos,
) {

    override val domainTypes
        get() = inputClasses + outputClass

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, SchemaType>,
        action: Action
    ): ActionStatus = ActionRunner.execute(processContext) {
        val inputValues: List<Any> = inputs.map {
            processContext.getValue(variable = it.name, type = it.type)
                ?: throw IllegalArgumentException("Input ${it.name} of type ${it.type} not found in process context")
        }
        logger.debug("Resolved action {} inputs {}", name, inputValues)
        val output = block.transform(
            TransformationPayload(
                input = inputValues,
                processContext = processContext,
                inputClass = List::class.java as Class<List<Any>>,
                outputClass = outputClass,
                action = this,
            )
        )
        if (output == null) {
            null
        } else {
            if (!outputClass.isInstance(output)) {
                throw IllegalArgumentException(
                    """
                Output of action $name is not of type ${outputClass.name}.
                Did you incorrectly obtain a PromptRunner via 'using' before the end of an action method?
                Take a payload object as the last signature of your method signature.
                Return was $output
                """.trimIndent()
                )
            }
            if (outputVarName != null) {
                logger.debug("Binding output of action {}: {} to {}", name, outputVarName, output)
                processContext.agentProcess[outputVarName] = output
            } else {
                logger.debug("Adding output of action {}: {}", name, output)
                processContext.agentProcess += output
            }
        }
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClasses.map { it.declaredFields.map { it.name } }.flatten().toSet()
            fields
        }
    }
}
