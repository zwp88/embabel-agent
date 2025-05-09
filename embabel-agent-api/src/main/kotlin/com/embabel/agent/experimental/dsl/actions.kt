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
package com.embabel.agent.experimental.dsl

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.core.AgentScope

/**
 * This interface is used to build actions in a DSL-like manner.
 * It allows chaining of functions with different input and output types.
 *
 * @see [AgentScopeFactory]
 */

fun <A, B, C> chain(
    a: (a: A) -> B,
    b: (b: B) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeFactory = TODO()


inline fun <reified A, reified B, reified C> chain(
    noinline a: (a: A) -> B,
    noinline b: (b: B) -> C,
): AgentScopeFactory {
    return chain(a, b, A::class.java, B::class.java, C::class.java)
}

fun <A, B, C> aggregate1(
    transforms: List<(a: A, context: OperationContext) -> B>,
    merge: (list: List<B>) -> C,
    aClass: Class<A>,
    bClass: Class<B>,
    cClass: Class<C>,
): AgentScopeFactory = TODO()

inline fun <reified A, reified B, reified C> aggregate1(
    transforms: List<(a: A, context: OperationContext) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeFactory {
    return aggregate1(
        transforms,
        merge,
        A::class.java, B::class.java, C::class.java
    )
}

/**
 * Run all the transforms and merge the results.
 */
inline fun <reified A, reified B, reified C> aggregate(
    transforms: List<(a: A) -> B>,
    noinline merge: (list: List<B>) -> C,
): AgentScopeFactory {
    return aggregate1(
        transforms.map { transform -> { a: A, context: OperationContext -> transform(a) } },
        merge,
        A::class.java, B::class.java, C::class.java
    )
}

inline fun <reified A, reified B> withAll(
    vararg transforms: (a: A) -> B,
): Aggregated<A, B> {
    return Aggregated(
        transforms.map { transform -> { a: A, context: OperationContext -> transform(a) } },
        A::class.java, B::class.java,
    )
}

class Aggregated<A, B>(
    private val transforms: List<(a: A, context: OperationContext) -> B>,
    private val aClass: Class<A>,
    private val bClass: Class<B>,
) {
    fun <C> merge(merger: (b: B) -> C): AgentScopeFactory {
        TODO()
    }

}

class AgentScopeFactory(
//    val block: (T) -> Unit,
) {
    fun parallelize(): AgentScopeFactory {
        return this
    }

    fun build(): AgentScope {
        TODO()
    }
}
