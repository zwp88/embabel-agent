/*
 * Copyright 2025 Embabel Software, Inc.
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
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.primitive.LlmOptions
import com.embabel.agent.support.AbstractAction
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.EffectSpec
import com.embabel.textio.graph.schema.NodeDefinition
import org.springframework.ai.tool.ToolCallback
import java.lang.reflect.Modifier

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

/**
 * Payload for any operation
 * @param processContext the process context
 * @param action the action being executed, if one is executing.
 * This is useful for getting tools etc.
 */
interface OperationPayload : Blackboard {
    val processContext: ProcessContext
    val action: Action?
}

interface InputPayload<I> : OperationPayload {
    val input: I

    fun agentPlatform() = processContext.platformServices.agentPlatform

}


data class TransformationPayload<I, O>(
    override val input: I,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputPayload<I>, Blackboard by processContext.blackboard,
    AgenticEventListener by processContext.platformServices.eventListener {

    /**
     * Simple prompt transformation
     */
    fun <I, O> transform(
        input: I,
        prompt: (input: I) -> String,
        llmOptions: LlmOptions = LlmOptions.Companion(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): O = processContext.transform(
        input, prompt, llmOptions, toolCallbacks, outputClass,
        agentProcess = processContext.agentProcess,
        action = this.action,
    )
}

/**
 * Transformation function signature
 */
fun interface Transformation<I, O> {
    fun transform(payload: TransformationPayload<I, O>): O
}

data class AggregationPayload<I, O>(
    val input: List<I>,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : OperationPayload, Blackboard by processContext.blackboard

typealias Aggregation<I, O> = (
    payload: AggregationPayload<I, O>,
) -> O

data class SupplyPayload<O>(
    override val processContext: ProcessContext,
    override val action: Action?,
    val outputClass: Class<O>,
) : OperationPayload, Blackboard by processContext.blackboard, LlmTransformer by processContext

typealias Supply<O> = (
    payload: SupplyPayload<O>,
) -> O

data class ConsumerPayload<I>(
    override val input: I,
    override val action: Action?,
    override val processContext: ProcessContext,
) : InputPayload<I>, Blackboard by processContext.blackboard, LlmTransformer by processContext

typealias ConsumerFunction<I> = (
    payload: ConsumerPayload<I>,
) -> Unit

inline fun <reified O> supplier(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    outputVarName: String = "it",
    cost: Double = 0.0,
    toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String> = emptySet(),
    transitions: List<Transition> = emptyList(),
    qos: Qos = Qos(),
    noinline block: Supply<O>,
): Action {
    return Supplier(
        name = name,
        description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        cost = cost,
        outputVarName = outputVarName,
        transitions = transitions,
        toolCallbacks = toolCallbacks,
        toolGroups = toolGroups,
        qos = qos,
        outputClass = O::class.java,
        block = block,
    )
}

/**
 * Transformer to convert input to output
 */
inline fun <reified I> consumer(
    name: String = "${I::class.simpleName}->Unit",
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = "it",
    cost: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolCallbacks: Collection<ToolCallback> = emptyList(),
    toolGroups: Collection<String> = emptyList(),
    qos: Qos = Qos(),
    referencedInputProperties: Set<String>? = null,
    noinline block: ConsumerFunction<I>,
): Action {
    return Consumer(
        name = name,
        description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        cost = cost,
        transitions = transitions,
        qos = qos,
        inputVarName = inputVarName,
        inputClass = I::class.java,
        toolCallbacks = toolCallbacks,
        toolGroups = toolGroups,
        referencedInputProperties = referencedInputProperties,
        block = block,
    )
}

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

inline fun <reified I, reified O : Any> forkJoin(
    name: String = "s${I::class.simpleName}*->${O::class.simpleName}",
    description: String = name,
    transformation: Transformation<I, O>,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    inputVarName: String = "it",
    canRerun: Boolean = false,
    noinline joinWith: (l: List<I>) -> O,
): Action {
    return ForkJoin<I, O>(
        name = name, description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        inputClass = I::class.java,
        outputClass = O::class.java,
        inputVarName = inputVarName,
        transformation = transformation,
        canRerun = canRerun,
        joinWith = joinWith,
        toolCallbacks = TODO(),
    )
}

inline fun <reified I, reified O> aggregator(
    name: String,
    description: String = name,
    pre: List<Condition> = emptyList(),
    post: List<Condition> = emptyList(),
    outputVarName: String = "it",
    cost: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolCallbacks: Collection<ToolCallback> = emptyList(),
    toolGroups: Collection<String> = emptySet(),
    qos: Qos = Qos(),
    referencedInputProperties: Set<String>? = null,
    noinline block: Aggregation<I, O>,
): Action {
    return Aggregator<I, O>(
        name = name,
        description = description,
        pre = pre.map { it.name },
        post = post.map { it.name },
        cost = cost,
        transitions = transitions,
        qos = qos,
        outputVarName = outputVarName,
        inputClass = I::class.java,
        outputClass = O::class.java,
        toolCallbacks = toolCallbacks,
        toolGroups = toolGroups,
        referencedInputProperties = referencedInputProperties,
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
    if (com.embabel.agent.Aggregation::class.java.isAssignableFrom(inputClass)) {
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
    return setOf(IoBinding(inputVarName, inputClass.simpleName))
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
    outputs = if (outputVarName == null) emptySet() else setOf(IoBinding(outputVarName, outputClass.simpleName)),
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
        outputTypes: Map<String, NodeDefinition>,
        action: Action
    ): ActionStatus = ActionRunner.execute {
        val input = processContext.getValue(inputVarName, inputClass.simpleName) as I
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

// DOES each split get its own process? how do you join?
// Otherwise how does downstream triggering work from each "it"?
// Numbered it naming it_2/20
// Maybe downstream triggering is not possible, although state triggering is?
// Or do we get a second it at each time and it does fire?, even in parallel
class ForkJoin<I, O : Any>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    qos: Qos = Qos(),
    private val inputClass: Class<I>,
    private val outputClass: Class<O>,
    private val inputVarName: String,
    private val outputVarName: String = "it",
    private val referencedInputProperties: Set<String>? = null,
    toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String> = emptySet(),
    private val transformation: Transformation<I, O>,
    private val joinWith: (l: List<I>) -> O,
    canRerun: Boolean = false,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs = setOf(IoBinding(inputVarName, "List")),
    outputs = setOf(IoBinding(outputVarName, outputClass.simpleName)),
    transitions = emptyList(),
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    canRerun = canRerun,
    qos = qos,
) {

    override val domainTypes
        get() = setOf(inputClass, outputClass)

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action
    ): ActionStatus = ActionRunner.execute {
        // TODO get variable?
        val inputs = processContext.blackboard.entries.filterIsInstance(inputClass)
        // TODO runner modes for parallelization etc.
        val outputs = mutableListOf<O>()

        inputs.forEach { input ->
            val output = transformation.transform(
                // TODO do we spawn here?
                TransformationPayload(
                    input = input,
                    processContext = processContext,
                    inputClass = inputClass,
                    outputClass = outputClass,
                    action = this,
                )
            )
            // TODO put entries
            outputs.add(output)
        }
        val joined = joinWith(inputs)
        processContext.blackboard[outputVarName] = joined
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClass.declaredFields.map { it.name }.toSet()
            fields
        }
    }
}

class Aggregator<I, O>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String> = emptySet(),
    qos: Qos = Qos(),
    private val inputClass: Class<I>,
    private val outputClass: Class<O>,
    private val outputVarName: String = "it",
    private val referencedInputProperties: Set<String>? = null,
    private val block: Aggregation<I, O>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs = emptySet(),
    outputs = setOf(IoBinding(outputVarName, outputClass.simpleName)),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    qos = qos,
    canRerun = true,
) {

    override val domainTypes
        get() = setOf(inputClass, outputClass)

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action
    ): ActionStatus = ActionRunner.execute {
        val input = processContext.blackboard.entries.filterIsInstance(inputClass)
        val output = block(
            AggregationPayload(
                input = input,
                processContext = processContext,
                inputClass = inputClass,
                outputClass = outputClass,
                action = this,
            )
        )
        processContext.blackboard[outputVarName] = output as Any
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClass.declaredFields.map { it.name }.toSet()
            fields
        }
    }
}

class Consumer<I>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    qos: Qos = Qos(),
    canRerun: Boolean = false,
    private val inputClass: Class<I>,
    private val inputVarName: String = "it",
    private val referencedInputProperties: Set<String>? = null,
    toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String> = emptySet(),
    private val block: ConsumerFunction<I>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs = expandInputBindings(inputVarName, inputClass),
    outputs = emptySet(),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    canRerun = canRerun,
    qos = qos,
) {

    override val domainTypes
        get() = setOf(inputClass)

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action
    ): ActionStatus = ActionRunner.execute {
        val input = processContext.getValue(inputVarName, inputClass.simpleName) as I
        block(ConsumerPayload(input = input, action = this, processContext = processContext))
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClass.declaredFields.map { it.name }.toSet()
            fields
        }
    }
}

class Supplier<O>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String> = emptySet(),
    canRerun: Boolean = false,
    qos: Qos = Qos(),
    private val outputClass: Class<O>,
    private val outputVarName: String = "it",
    private val block: Supply<O>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs = emptySet(),
    outputs = setOf(IoBinding(outputVarName, outputClass.simpleName)),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    qos = qos,
    canRerun = canRerun,
) {

    // No input preconditions
    override val preconditions: EffectSpec = pre.associate { it to ConditionDetermination(true) }.toMutableMap()

    override val domainTypes
        get() = setOf(outputClass)

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action
    ): ActionStatus = ActionRunner.execute {
        val output = block(SupplyPayload(processContext = processContext, action = this, outputClass = outputClass))
        processContext.blackboard[outputVarName] = output as Any
    }

    override fun referencedInputProperties(variable: String): Set<String> = emptySet()
}
