package com.embabel.agent.domain.persistence.support

import com.embabel.agent.domain.persistence.Entity
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DefaultIntroductionAdvisor
import org.springframework.aop.support.DelegatingIntroductionInterceptor

data class MixinHolder<T>(
    val mixinInterface: Class<T>,
    val mixin: T,
)

data class EntityHolder(
    val root: Entity<String>,
    val mixins: List<MixinHolder<*>>,
) : Entity<String> {

    override val id: String?
        get() = root.id

    val implementationOfAllInterfaces: Any by lazy {
        val introductionInterceptor = EntityIntroductionInterceptor()
        // Create the proxy with all interfaces
        val allInterfaces = mutableListOf<Class<*>>().apply {
            // Add the root entity interface
            add(Entity::class.java)
            // Add all mixin interfaces
            mixins.forEach { mixinHolder ->
                add(mixinHolder.mixinInterface)
            }
        }.toTypedArray()

        // Create the proxy using Spring's ProxyFactory
        val factory = ProxyFactory()
        factory.setTarget(root)
        factory.setInterfaces(*allInterfaces)
        val advisor = DefaultIntroductionAdvisor(introductionInterceptor)
        factory.addAdvisor(advisor)
        factory.proxy
    }

    private inner class EntityIntroductionInterceptor : DelegatingIntroductionInterceptor(root) {
        // Return an object made with Spring AOP IntroductionAdvisor that implements all interfaces
        override fun invoke(mi: MethodInvocation): Any? {
            // Try to find a mixin that can handle this method
            val method = mi.method
            val declaringClass = method.declaringClass

            // If the method is from our root entity, delegate to it
            if (root.javaClass.interfaces.contains(declaringClass) ||
                declaringClass == Entity::class.java
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

