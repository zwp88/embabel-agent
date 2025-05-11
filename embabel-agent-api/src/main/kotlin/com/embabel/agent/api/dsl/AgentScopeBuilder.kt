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

import com.embabel.agent.api.common.InputActionContext
import com.embabel.agent.api.common.InputsActionContext
import com.embabel.agent.api.dsl.support.Transformer
import com.embabel.agent.core.*
import com.embabel.agent.core.support.Rerun
import com.embabel.common.core.MobyNameGenerator

/**
 * This interface is used to build actions in a DSL-like manner.
 * It allows chaining of functions with different input and output types.
 *
 * @see [AgentScopeBuilder]
 */

fun <A, B, C> chain(
    a: (a: A) -> B,
    b: (b: B) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder = TODO()


inline fun <reified A, reified B, reified C> chain(
    noinline a: (a: A) -> B,
    noinline b: (b: B) -> C,
): AgentScopeBuilder {
    return chain(a, b, A::class.java, B::class.java, C::class.java)
}


fun <A, B, C> aggregate(
    transforms: List<(context: InputActionContext<A>) -> B>,
    merge: (list: List<B>) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder {
    val allCompletedCondition = ComputedBooleanCondition(
        name = "List<${bClass.name}>=>${cClass.name}",
        evaluator = {
            it.blackboard.all(bClass).size == transforms.size
        }
    )
    val actions = mutableListOf<Action>()

    val transformActions = transforms.mapIndexed { index, transform ->
        Transformer(
            name = "${aClass.name}=>${bClass.name}-$index",
            description = "Transform $aClass to $bClass",
            pre = emptyList(),
            post = listOf(allCompletedCondition.name),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputClass = aClass,
            outputClass = bClass,
            toolGroups = emptyList(),
            toolCallbacks = emptyList(),
        ) {
            transform.invoke(it)
        }
    }
    actions += transformActions
    val mergeAction = Transformer(
        name = "List<${bClass.name}>=>${cClass.name}",
        description = "Aggregate list $bClass to $cClass",
        pre = transformActions.map { Rerun.hasRunCondition(it) } + allCompletedCondition.name,
        post = emptyList(),
        cost = 0.0,
        value = 0.0,
        canRerun = true,
        inputClass = bClass,
        outputClass = cClass,
        toolGroups = emptyList(),
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

interface OutputAwareActionContext<I, O> : InputsActionContext {
    val input: I
    val output: O

    override val inputs: List<Any> get() = listOfNotNull(input, output)
}

fun <A, B, C> repeatableAggregate(
    transforms: List<(context: OutputAwareActionContext<A, C>) -> B>,
    merge: (list: List<B>) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeBuilder = TODO()

/**
 * Run all the transforms and merge the results.
 */
inline fun <reified A, reified B, reified C> aggregate(
    transforms: List<(context: InputActionContext<A>) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeBuilder {
    return aggregate(
        transforms, merge, A::class.java, B::class.java, C::class.java
    )
}

inline fun <reified A, reified B, reified C> repeatableAggregate(
    transforms: List<(context: OutputAwareActionContext<A, C>) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeBuilder {
    return repeatableAggregate(
        transforms, merge, A::class.java, B::class.java, C::class.java
    )
}

fun <C> repeat(
    what: () -> AgentScopeBuilder,
    until: (context: InputActionContext<C>) -> Boolean,
    cClass: Class<*>,
): AgentScopeBuilder = TODO()

inline fun <reified C> repeat(
    noinline what: () -> AgentScopeBuilder,
    noinline until: (context: InputActionContext<C>) -> Boolean,
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
}
