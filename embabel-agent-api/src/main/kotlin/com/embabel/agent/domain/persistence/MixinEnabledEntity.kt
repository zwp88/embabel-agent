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
package com.embabel.agent.domain.persistence

/**
 * An entity that has its own id and state but supports mixins.
 */
interface MixinEnabledEntity<ID> {
    val id: ID?

    fun persistent(): Boolean {
        return id != null
    }
}


/**
 * An interface that allows to mixin other interfaces to an entity.
 * Each entity must have a root, but other interfaces can be mixed in to it.
 */
interface MixinRepository<ID> {

    fun <E : MixinEnabledEntity<ID>> findById(id: ID, type: Class<E>): E?

    fun <E1 : MixinEnabledEntity<ID>, E2 : E1> findById(id: ID, type1: Class<E1>, type2: Class<E2>): E2?

    /**
     * Create a mixin of the two types.
     */
    fun <E : MixinEnabledEntity<ID>, T> become(e: E, t: T, type: Class<T>): E

    fun <E : MixinEnabledEntity<ID>> save(entity: E): E

}

inline fun <reified E1 : MixinEnabledEntity<ID>, reified E2 : E1, ID> MixinRepository<ID>.findById(id: ID): E2? {
    return findById(id, E1::class.java, E2::class.java)
}

inline fun <reified E : MixinEnabledEntity<ID>, reified T, ID> MixinRepository<ID>.become(e: E, t: T): E {
    return become(e, t, T::class.java)
}
