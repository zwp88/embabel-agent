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