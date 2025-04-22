package com.embabel.agent.domain.persistence

import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DelegatingIntroductionInterceptor

interface Entity<ID> {
    val id: ID?

    fun persistent(): Boolean {
        return id != null
    }
}

data class Mixin2<E1 : Entity<ID>, E2, ID>(
    val e1: E1,
    val e2: E2,
) : Entity<ID> {

    override val id: ID?
        get() = e1.id

    override fun persistent(): Boolean {
        // TODO what about e2?
        return e1.persistent()
    }
}

fun <E1 : Any, E2 : E1> mixin(
    e1: E1,
    e2: E2
): E2 {
    // Create an introduction interceptor that delegates to e1
    val delegator = object : DelegatingIntroductionInterceptor(e1) {
        override fun invoke(mi: org.aopalliance.intercept.MethodInvocation): Any? {
            // Try to invoke the method on e2 first
            val method = mi.method
            val args = mi.arguments

            return try {
                // If method exists on e2, use it
                val e2Method = e2::class.java.getMethod(method.name, *method.parameterTypes)
                e2Method.invoke(e2, *args)
            } catch (ex: NoSuchMethodException) {
                // Fall back to e1 (via the superclass implementation)
                super.invoke(mi)
            } catch (ex: Exception) {
                // If there's some other error invoking on e2, fall back to e1
                super.invoke(mi)
            }
        }
    }

    // Create the proxy factory
    val proxyFactory = ProxyFactory()
    proxyFactory.setTarget(e1)
    proxyFactory.addAdvice(delegator)

    // Add all interfaces from E2
    e2::class.java.interfaces.forEach { proxyFactory.addInterface(it) }

    // If E2 is an interface itself, add it
    if (e2::class.java.isInterface) {
        proxyFactory.addInterface(e2::class.java)
    }

    // Create and return the proxy
    @Suppress("UNCHECKED_CAST")
    return proxyFactory.proxy as E2
}


interface MixinRepository<ID> {

    fun <E : Entity<ID>> findById(id: ID, type: Class<E>): E?

    fun <E1 : Entity<ID>, E2 : Entity<ID>> findById(id: ID, type1: Class<E1>, type2: Class<E2>): Mixin2<E1, E2, ID>

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