package com.embabel.agent.domain.persistence.support

import com.embabel.agent.domain.persistence.MixinEnabledEntity
import java.util.*

class InMemoryMixinRepository : AbstractMixinRepository<String>(
    generateId = { UUID.randomUUID().toString() },
) {

    private val map: MutableMap<String, MixinEnabledEntityHolder> = mutableMapOf()

    override fun store(entity: MixinEnabledEntity<String>, id: String) {
        var found = map[id]
        val entityHolder = found?.copy(root = entity)
            ?: MixinEnabledEntityHolder(
                root = entity,
                mixins = emptyList(),
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
        val mixins = foundEntityHolder.mixins + mixin
        map[e.id!!] = foundEntityHolder.copy(mixins = mixins)
        return e
    }

}
