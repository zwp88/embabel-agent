package com.embabel.agent.domain.persistence.support

import com.embabel.agent.domain.persistence.MixinEnabledEntity
import com.embabel.agent.domain.persistence.MixinRepository
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