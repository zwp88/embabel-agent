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
package com.embabel.agent.config

import com.embabel.agent.api.common.ExecutingOperationContext
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.ProcessOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

/**
 * Support injection of [ExecutingOperationContext] into beans.
 */
@Configuration
class OperationContextInjectionConfiguration {

    @Bean
    @Scope("prototype")
    fun executingOperationContextFactory(agentPlatform: AgentPlatform): ExecutingOperationContext {
        val processOptions = ProcessOptions()
        val callingClassName = findFirstUserClass()
        val agentForIdOnly = agent(
            name = callingClassName,
            description = "Empty agent for operation context injection into $callingClassName",
        ) {
            // No actions, just a placeholder
        }
        return ExecutingOperationContext(
            name = callingClassName,
            agentProcess = agentPlatform.createAgentProcess(
                agentForIdOnly,
                processOptions = processOptions,
                bindings = emptyMap(),
            ),
        )
    }
}

private fun findFirstUserClass(): String {
    val stackTrace = Thread.currentThread().stackTrace

    return stackTrace
        .firstOrNull { element ->
            element.className.startsWith("com.embabel.agent") &&
                    !element.className.contains("$") && // Avoid inner classes
                    !element.methodName.contains("<init>") // Avoid constructor calls from Spring
        }
        ?.className
        ?.substringAfterLast(".")
        ?: "Unknown"
}
