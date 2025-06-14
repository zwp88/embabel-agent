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
package com.embabel.agent.experimental.domain.mixin

import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DefaultIntroductionAdvisor
import org.springframework.aop.support.DelegatingIntroductionInterceptor

interface Proxied

/**
 * Hold a single mixin interface and its implementation.
 */
private data class MixinHolder<T>(
    val mixinInterface: Class<T>,
    val mixin: T,
)

/**
 * Manages mixins for a root entity
 */
class MixinManager<R : Any>(
    private var root: R,
) {
    private val mixins = mutableListOf<MixinHolder<*>>()

    fun updateRoot(
        root: R,
    ): MixinManager<R> {
        this.root = root
        return this
    }

    fun allInterfaces(): List<Class<*>> {
        return mixins.map { it.mixinInterface } + firstInterface(root)
    }

    fun <T> become(t: T, type: Class<T>): R {
        if (!type.isInterface) {
            throw IllegalArgumentException("Type $type is not an interface")
        }
        val mixinHolder = MixinHolder(
            mixinInterface = type,
            mixin = t,
        )
        // TODO check if the mixin interface is already present
        mixins += mixinHolder
        return root
    }

    /**
     * Implement the first interface of the object
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> becomeFirstInterface(t: T): R {
        return become(t, firstInterface(t) as Class<T>)
    }

    /**
     * Return the implementation of all interfaces, including
     * the root entity interface and all mixin interfaces.
     * @param plus additional interfaces to add
     */
    @Suppress("UNCHECKED_CAST")
    fun instance(plus: List<Class<*>> = emptyList()): R {
        val introductionInterceptor = EntityIntroductionInterceptor()
        // Adding Proxied tag interface to the list of interfaces seems to ensure that
        // CGLIB isn't used
        val allInterfaces = (allInterfaces() + plus + Proxied::class.java).toTypedArray()

        val factory = ProxyFactory(root)
        factory.setInterfaces(*allInterfaces)
        factory.addAdvisor(DefaultIntroductionAdvisor(introductionInterceptor))
        return factory.proxy as R
    }

    private inner class EntityIntroductionInterceptor : DelegatingIntroductionInterceptor(root) {
        override fun invoke(mi: MethodInvocation): Any? {
            // Try to find a mixin that can handle this method
            val method = mi.method
            val declaringClass = method.declaringClass

            // If the method is from our root entity, delegate to it
            if (root.javaClass.interfaces.contains(declaringClass) ||
                declaringClass == MixinEnabledEntity::class.java
            ) {
                return super.invoke(mi)
            }

            // Look for a mixin that implements this interface
            val mixinHolder = mixins.find {
                it.mixinInterface == declaringClass ||
                        it.mixinInterface.interfaces.contains(declaringClass)
            }

            return if (mixinHolder != null) {
                // Invoke the method on the mixin
                method.invoke(mixinHolder.mixin, *mi.arguments)
            } else {
                // Fallback to the default behavior
                super.invoke(mi)
            }
        }
    }
}

private fun firstInterface(o: Any): Class<*> {
    return o.javaClass.interfaces.firstOrNull()
        ?: throw IllegalArgumentException("Object implements no interface")
}
