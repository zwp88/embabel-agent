package com.embabel.agent.domain.persistence.support

import com.embabel.agent.domain.persistence.Entity
import java.util.*

class InMemoryMixinRepository : AbstractMixinRepository<String>(
    generateId = { UUID.randomUUID().toString() },
) {

    private val map: MutableMap<String, EntityHolder> = mutableMapOf()

    override fun store(entity: Entity<String>, id: String) {
        var found = map[id]
        val entityHolder = found?.copy(root = entity)
            ?: EntityHolder(
                root = entity,
                mixins = emptyList(),
            )
        map[id] = entityHolder
    }

    override fun <E : Entity<String>> findById(
        id: String,
        type: Class<E>
    ): E? {
        return map[id]?.implementationOfAllInterfaces as? E?
    }

    override fun <E1 : Entity<String>, E2 : E1> findById(
        id: String,
        type1: Class<E1>,
        type2: Class<E2>
    ): E2? {
        TODO("Not yet implemented")
    }

    override fun <E : Entity<String>, T> become(e: E, t: T, type: Class<T>): E {
        require(e.id != null) { "Entity must be persistent to use become" }
        val foundEntityHolder = map[e.id]
            ?: throw IllegalStateException("Entity with id ${e.id} not found")
        val mixin = MixinHolder(
            mixinInterface = type,
            mixin = t,
        )
        // TODO change the mixin if it already exists
        val mixins = foundEntityHolder.mixins + mixin
        map[e.id!!] = foundEntityHolder.copy(mixins = mixins)
        return e
    }

}
