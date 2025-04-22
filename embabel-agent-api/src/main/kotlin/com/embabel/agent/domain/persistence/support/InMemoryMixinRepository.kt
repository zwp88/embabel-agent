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
package com.embabel.agent.domain.persistence.support

import com.embabel.agent.domain.mixin.MixinHolder
import com.embabel.agent.domain.mixin.MixinManager
import com.embabel.agent.domain.persistence.MixinEnabledEntity
import java.util.*

typealias MixinEnabledEntityHolder = MixinManager<MixinEnabledEntity<String>>

class InMemoryMixinRepository : AbstractMixinRepository<String>(
    generateId = { UUID.randomUUID().toString() },
) {

    private val map: MutableMap<String, MixinEnabledEntityHolder> = mutableMapOf()

    override fun store(entity: MixinEnabledEntity<String>, id: String) {
        var found = map[id]
        val entityHolder = found?.updateRoot(root = entity)
            ?: MixinEnabledEntityHolder(
                root = entity,
            )
        map[id] = entityHolder
    }

    override fun <E : MixinEnabledEntity<String>> findById(
        id: String,
        type: Class<E>
    ): E? {
        return map[id]?.implementationOfAllInterfaces() as? E
    }

    override fun <E1 : MixinEnabledEntity<String>, E2 : E1> findById(
        id: String,
        type1: Class<E1>,
        type2: Class<E2>
    ): E2? {
        return return map[id]?.implementationOfAllInterfaces(listOf(type2)) as? E2
    }

    override fun <E : MixinEnabledEntity<String>, T> become(e: E, t: T, type: Class<T>): E {
        require(e.id != null) { "Entity must be persistent to use become" }
        val foundEntityHolder = map[e.id]
            ?: throw IllegalStateException("Entity with id ${e.id} not found")
        val mixin = MixinHolder(
            mixinInterface = type,
            mixin = t,
        )
        // TODO change the mixin if it already exists
        val mixins = foundEntityHolder + mixin
        map[e.id!!] = mixins
        return e
    }

}
