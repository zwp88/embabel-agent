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
package com.embabel.agent.experimental.domain.mixin.support

import com.embabel.agent.experimental.domain.mixin.MixinEnabledEntity
import com.embabel.agent.experimental.domain.mixin.MixinRepository
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

abstract class AbstractMixinRepository<ID>(
    private val generateId: () -> ID,
) : MixinRepository<ID> {

    protected abstract fun store(entity: MixinEnabledEntity<ID>, id: ID)

    override fun <E : MixinEnabledEntity<ID>> save(entity: E): E {
        val id = entity.id ?: generateId()

        // Create a copy of the entity with the new ID using reflection
        val copiedEntity = if (entity.id != id) {
            val copyMethod = entity::class.memberFunctions.find { it.name == "copy" }
            val idParameter = copyMethod?.parameters?.find { it.name == "id" }

            if (copyMethod != null && idParameter != null) {
                @Suppress("UNCHECKED_CAST")
                copyMethod.callBy(
                    mapOf(
                        copyMethod.instanceParameter!! to entity,
                        idParameter to id
                    )
                ) as E
            } else {
                entity
            }
        } else {
            entity
        }

        store(copiedEntity, id)
        return copiedEntity
    }
}
