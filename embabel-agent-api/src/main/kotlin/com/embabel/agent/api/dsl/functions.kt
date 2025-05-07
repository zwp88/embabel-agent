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

import com.embabel.agent.core.Action

interface AgentFactory {

    fun <A, B, C> chain(
        a: (a: A) -> B,
        b: (b: B) -> C,
        aClass: Class<A>,
        bClass: Class<B>,
        cClass: Class<C>,
    ): Actions
}

inline fun <reified A, reified B, reified C> AgentFactory.chain(
    noinline a: (a: A) -> B,
    noinline b: (b: B) -> C,
): Actions {
    return chain(a, b, A::class.java, B::class.java, C::class.java)
}

class Actions(
//    val block: (T) -> Unit,
) {
    fun parallelize(): Actions {
        return this
    }

    fun toActions(): Collection<Action> {
        TODO()
    }
}
