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

import com.embabel.agent.api.common.*
import com.embabel.agent.api.common.support.*
import com.embabel.agent.core.*
import com.embabel.agent.core.support.Rerun
import com.embabel.agent.core.support.safelyGetToolCallbacks
import com.embabel.common.core.MobyNameGenerator
import org.springframework.ai.tool.ToolCallback

/**
 * Creates a chain from A to B via C. Emits actions.
 */
fun <A, B, C> chain(
    a: (context: InputActionContext<A>) -> B,
    b: (context: InputActionContext<B>) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder {
    val actions: List<Action> = listOf(
        TransformationAction(
            name = "chain-0",
            description = "chain element 0",
            cost = 0.0,
            value = 0.0,
            canRerun = false,
            inputClass = aClass,
            outputClass = bClass,
            toolGroups = emptySet(),
            toolCallbacks = emptyList(),
        ) {
            a.invoke(it)
        },
        TransformationAction(
            name = "chain-1",
            description = "chain element 0",
            cost = 0.0,
            value = 0.0,
            canRerun = false,
            inputClass = bClass,
            outputClass = cClass,
            toolGroups = emptySet(),
            toolCallbacks = emptyList(),
        ) {
            b.invoke(it)
        })
    return AgentScopeBuilder(
        name = MobyNameGenerator.generateName(),
        actions = actions,
    )
}


/**
 * Branch from A to B or C using Kotlin reified types.
 * Makes conditionals easy to express.
 */
fun <A, B, C> branch(
    a: (context: InputActionContext<A>) -> Branch<B, C>,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder {
    val branchAction =
        BranchingAction<A, B, C>(
            name = "chain-0",
            description = "chain element 0",
            cost = 0.0,
            value = 0.0,
            canRerun = false,
            inputClass = aClass,
            leftOutputClass = bClass,
            rightOutputClass = cClass,
            toolGroups = emptySet(),
            toolCallbacks = emptyList(),
        ) {
            a.invoke(it)
        }
    return AgentScopeBuilder(
        name = MobyNameGenerator.generateName(),
        actions = listOf(branchAction),
    )
}

/**
 * Convenience method to branch from A to B or C using Kotlin reified types.
 */
inline fun <reified A, reified B, reified C> branch(
    noinline a: (context: InputActionContext<A>) -> Branch<B, C>,
): AgentScopeBuilder {
    return branch(a, A::class.java, B::class.java, C::class.java)
}

/**
 * Convenience method to chain A to B via C using Kotlin reified types.
 */
inline fun <reified A, reified B, reified C> chain(
    noinline a: (context: InputActionContext<A>) -> B,
    noinline b: (context: InputActionContext<B>) -> C,
): AgentScopeBuilder {
    return chain(a, b, A::class.java, B::class.java, C::class.java)
}


fun <A, B, C> aggregate(
    transforms: List<(context: InputActionContext<A>) -> B>,
    merge: (list: List<B>, context: OperationContext) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder {
    val allCompletedCondition = ComputedBooleanCondition(
        name = "List<${bClass.name}>=>${cClass.name}",
        evaluator = { it, condition ->
            it.blackboard.all(bClass).size == transforms.size
        }
    )
    val actions = mutableListOf<Action>()

    val transformActions = transforms.mapIndexed { index, transform ->
        TransformationAction(
            name = "${aClass.name}=>${bClass.name}-$index",
            description = "Transform $aClass to $bClass",
            pre = emptyList(),
            post = listOf(allCompletedCondition.name),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputClass = aClass,
            outputClass = bClass,
            toolGroups = emptySet(),
            toolCallbacks = emptyList(),
        ) {
            transform.invoke(it)
        }
    }
    actions += transformActions
    val mergeAction = SupplierAction<C>(
        name = "List<${bClass.name}>=>${cClass.name}",
        description = "Aggregate list $bClass to $cClass",
        pre = transformActions.map { Rerun.hasRunCondition(it) } + allCompletedCondition.name,
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        outputClass = cClass,
        toolGroups = emptySet(),
        toolCallbacks = emptyList(),
    ) { context ->
        merge(context.objects.filterIsInstance(bClass), context)
    }
    actions += mergeAction
    return AgentScopeBuilder(
        name = MobyNameGenerator.generateName(),
        actions = actions,
        goals = emptySet(),
        conditions = setOf(allCompletedCondition),
    )
}

/**
 * Aggregate taking 2 inputs
 */
fun <A1, A2, B : Any, C> biAggregate(
    transforms: List<(context: BiInputActionContext<A1, A2>) -> B>,
    merge: (list: List<B>) -> C,
    a1Class: Class<A1>,
    a2Class: Class<A2>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder {
    val allCompletedCondition = ComputedBooleanCondition(
        name = "List<${bClass.name}>=>${cClass.name}",
        evaluator = { it, condition ->
            it.blackboard.all(bClass).size == transforms.size
        }
    )
    val actions = mutableListOf<Action>()

    val transformActions = transforms.mapIndexed { index, transform ->
        MultiTransformationAction<B>(
            name = "${a1Class.name}+${a2Class.name}=>${bClass.name}-$index",
            description = "Transform ${a1Class.name}+${a2Class.name} to $bClass",
            pre = emptyList(),
            post = listOf(allCompletedCondition.name),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputs = setOf(
                IoBinding(IoBinding.DEFAULT_BINDING, a1Class.name),
                IoBinding(IoBinding.DEFAULT_BINDING, a2Class.name),
            ),
            inputClasses = listOf(a1Class, a2Class),
            outputClass = bClass,
            toolGroups = emptySet(),
        ) {
            transform.invoke(BiInputActionContext(it.input[0] as A1, it.input[1] as A2, it))
        }
    }
    actions += transformActions
    val mergeAction = TransformationAction(
        name = "List<${bClass.name}>=>${cClass.name}",
        description = "Aggregate list $bClass to $cClass",
        pre = transformActions.map { Rerun.hasRunCondition(it) } + allCompletedCondition.name,
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        inputClass = bClass,
        outputClass = cClass,
        toolGroups = emptySet(),
        toolCallbacks = emptyList(),
    ) {
        val cList = it.objects.filterIsInstance<B>(bClass)
        merge(cList)
    }
    actions += mergeAction
    return AgentScopeBuilder(
        name = MobyNameGenerator.generateName(),
        actions = actions,
        goals = emptySet(),
        conditions = setOf(allCompletedCondition),
    )
}

/**
 * Run all the transforms and merge the results.
 */
inline fun <reified A, reified B, reified C> aggregate(
    transforms: List<(context: InputActionContext<A>) -> B>,
    noinline merge: (list: List<B>, context: OperationContext) -> C,
): AgentScopeBuilder {
    return aggregate(
        transforms, merge, A::class.java, B::class.java, C::class.java
    )
}

data class BiInputActionContext<A1, A2>(
    val input1: A1,
    val input2: A2,
    val actionContext: ActionContext,
) : InputsActionContext, ActionContext by actionContext {

    override val inputs: List<Any> get() = listOfNotNull(input1, input2)

    override fun toolCallbacksOnDomainObjects(): List<ToolCallback> =
        safelyGetToolCallbacks(setOfNotNull(input1, input2))
}

inline fun <reified A1, reified A2, reified B : Any, reified C> biAggregate(
    transforms: List<(context: BiInputActionContext<A1, A2>) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeBuilder {
    return biAggregate<A1, A2, B, C>(
        transforms,
        merge,
        a1Class = A1::class.java,
        a2Class = A2::class.java,
        bClass = B::class.java,
        cClass = C::class.java,
    )
}

/**
 * Special aggregate that works like an accumulator. If the condition is not
 * satisfied, your transformer methods will be called again with the latest updated context.
 */
inline fun <reified A, reified B : Any, reified C> repeatableAggregate(
    startWith: C,
    transforms: List<(context: BiInputActionContext<A, C>) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeBuilder {
    val asb = biAggregate<A, C, B, C>(
        transforms,
        merge,
        a1Class = A::class.java,
        a2Class = C::class.java,
        bClass = B::class.java,
        cClass = C::class.java,
    )
    val populator: Action = SupplierAction<C>(
        name = "repeatable-aggregate",
        description = "Repeatable aggregate",
        pre = emptyList(),
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        outputClass = C::class.java,
        toolGroups = emptySet(),
        toolCallbacks = emptyList(),
    ) {
        startWith
    }
    return asb.copy(
        actions = asb.actions + populator,
    )
}


fun <C> repeat(
    what: () -> AgentScopeBuilder,
    // TODO gather this
    until: (c: C, context: ProcessContext) -> Boolean,
    cClass: Class<C>,
): AgentScopeBuilder {
    val conditionName = "repeat-until-${cClass.name}"
    val untilCondition = ComputedBooleanCondition(
        name = conditionName,
        evaluator = { it, condition ->
            val input = it.blackboard.last(cClass)
            if (input == null) {
                return@ComputedBooleanCondition false
            }
            until(input, it)
        })
    val doerScope = what.invoke().build()
    val completionAction = TransformationAction(
        name = "repeat",
        description = "Repeat until condition is met",
        pre = listOf(untilCondition.name),
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        inputClass = cClass,
        outputClass = cClass,
        toolGroups = emptySet(),
        toolCallbacks = emptyList(),
    ) {
        TODO()
    }
    return AgentScopeBuilder(
        name = MobyNameGenerator.generateName(),
        actions = doerScope.actions + completionAction,
        conditions = setOf(untilCondition),
    )
}

inline fun <reified C> repeat(
    noinline what: () -> AgentScopeBuilder,
    noinline until: (c: C, processContext: ProcessContext) -> Boolean,
): AgentScopeBuilder {
    return repeat(what, until, C::class.java)
}

//inline fun <reified A, reified B> withAll(
//    vararg transforms: (a: A) -> B,
//): Aggregated<A, B> {
//    return Aggregated(
//        transforms.map { transform -> { a: A, context: OperationContext -> transform(a) } },
//        A::class.java, B::class.java,
//    )
//}

data class AgentScopeBuilder(
    val name: String,
    val provider: String = "embabel",
    val actions: List<Action> = emptyList(),
    val goals: Set<Goal> = emptySet(),
    val conditions: Set<Condition> = emptySet(),
) {
    fun parallelize(): AgentScopeBuilder {
        return this
    }

    fun errorHandling(): AgentScopeBuilder {
        return this
    }

    fun build(): AgentScope {
        return AgentScope(
            name = name,
            actions = actions,
            goals = goals,
            conditions = conditions,
        )
    }

    inline fun <reified O : Any> run(context: TransformationActionContext<*, O>): O {
        val withExtraGoal = AgentScope(
            name = name,
            actions = actions,
            goals = goals + Goal(
                name = name,
                description = "description",
                inputs = setOf(
                    IoBinding(
                        type = context.outputClass,
                        name = IoBinding.DEFAULT_BINDING,
                    ),
                ),
            ),
            conditions = conditions,
        )
        val agent = withExtraGoal.createAgent(
            name = name,
            provider = provider,
            description = name,
        )
        val singleAction = agent.asAction<Any, O>()
        singleAction.execute(
            processContext = context.processContext,
            outputTypes = emptyMap(),
            action = context.action,
        )
        return context.last<O>() ?: throw IllegalStateException(
            "No output of type ${O::class.java} found in context"
        )
    }
}

inline fun <reified I, reified O : Any> runAgent(
    agent: Agent,
    context: TransformationActionContext<I, O>,
): O {
    val singleAction = agent.asAction<Any, O>()
    singleAction.execute(
        processContext = context.processContext,
        outputTypes = emptyMap(),
        action = context.action,
    )
    return context.last<O>() ?: throw IllegalStateException(
        "No output of type ${O::class.java} found in context"
    )
}
