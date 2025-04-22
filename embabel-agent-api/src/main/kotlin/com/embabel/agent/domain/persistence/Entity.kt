package com.embabel.agent.domain.persistence

interface Entity<ID> {
    val id: ID?

    fun persistent(): Boolean {
        return id != null
    }
}


interface MixinRepository<ID> {

    fun <E : Entity<ID>> findById(id: ID, type: Class<E>): E?

    fun <E1 : Entity<ID>, E2 : E1> findById(id: ID, type1: Class<E1>, type2: Class<E2>): E2?

    /**
     * Create a mixin of the two types.
     */
    fun <E : Entity<ID>, T> become(e: E, t: T, type: Class<T>): E

    fun <E : Entity<ID>> save(entity: E): E

}

inline fun <reified E1 : Entity<ID>, reified E2 : E1, ID> MixinRepository<ID>.findById(id: ID): E2? {
    return findById(id, E1::class.java, E2::class.java)
}

inline fun <reified E : Entity<ID>, reified T, ID> MixinRepository<ID>.become(e: E, t: T): E {
    return become(e, t, T::class.java)
}