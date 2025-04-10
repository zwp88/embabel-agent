/*
                                * Copyright 2025 Embabel Software, Inc.
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
