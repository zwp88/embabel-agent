package com.embabel.agent.support

import com.embabel.agent.AgentPlatform
import com.embabel.agent.AgentPlatformProperties
import com.embabel.agent.annotation.support.AgentMetadataReader
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

interface AutoRegisteringAgentPlatformProperties : AgentPlatformProperties {
    val autoRegister: Boolean
}

/**
 * Autoregister beans with @Agentic annotation
 */
@Service
@Order(Ordered.LOWEST_PRECEDENCE)
class AutoRegisteringBeanPostProcessor(
    private val agentMetadataReader: AgentMetadataReader,
    private val agentPlatform: AgentPlatform,
    private val properties: AutoRegisteringAgentPlatformProperties,
) : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        if (properties.autoRegister) {
            val agentMetadata = agentMetadataReader.createAgentMetadata(bean)
            if (agentMetadata != null) {
                agentPlatform.deploy(agentMetadata)
            }
        }
        return bean
    }
}