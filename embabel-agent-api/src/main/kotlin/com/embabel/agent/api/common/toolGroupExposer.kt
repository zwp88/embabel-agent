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
package com.embabel.agent.api.common

import com.embabel.agent.core.ToolCallbackPublisher
import com.embabel.agent.core.ToolGroup
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.aopalliance.intercept.MethodInvocation
import org.slf4j.LoggerFactory
import org.springframework.aop.IntroductionInterceptor
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DefaultIntroductionAdvisor

/**
 * Expose the given ToolGroup as an interface.
 * This allows code actions to call the tool group as an interface,
 * benefiting from MCP infrastructure without exposing too much power
 * to models.
 * Methods on the relevant interface must match the tool names
 * and the parameters must match the tool parameters.
 * Not all tools need to be supported.
 */
fun <T> exposeAsInterface(
    toolCallbackPublisher: ToolCallbackPublisher,
    intf: Class<T>,
): T {
    val introductionInterceptor = ToolGroupIntroductionInterceptor(toolCallbackPublisher, intf)
    val factory = ProxyFactory()
    factory.setInterfaces(intf)
    factory.addAdvisor(DefaultIntroductionAdvisor(introductionInterceptor))
    return factory.proxy as T
}

private class ToolGroupIntroductionInterceptor(
    private val toolCallbackPublisher: ToolCallbackPublisher,
    private val intf: Class<*>,
) : IntroductionInterceptor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val objectMapper = jacksonObjectMapper()

    init {
        if (!intf.isInterface) {
            throw IllegalArgumentException("Type $intf is not an interface")
        }
        // Validate methods
        intf.methods.forEach { method ->
            if (method.returnType == Void.TYPE) {
                throw IllegalArgumentException("Method ${method.name} cannot return void")
            }

            if (!toolCallbackPublisher.toolCallbacks.any { it.toolDefinition.name() == method.name }) {
                throw IllegalArgumentException(
                    "Method ${method.name} is not backed by the tool group: Known tools are\n${
                        toolCallbackPublisher.toolCallbacks.joinToString(
                            "\n"
                        ) { it.toolDefinition.name() }
                    }"
                )
            }
        }
    }

    override fun invoke(invocation: MethodInvocation): Any? {
        logger.debug(
            "Invoking {} on {}, looking for {}",
            invocation.method,
            toolCallbackPublisher,
            intf.name,
        )
        val argMap = invocation.method.parameters.map { it.name }.zip(invocation.arguments).toMap()
        val payload =
            if (invocation.arguments.isEmpty()) "{}" else objectMapper.writeValueAsString(argMap)
        val tool = toolCallbackPublisher.toolCallbacks.first { it.toolDefinition.name() == invocation.method.name }
        logger.info("Calling tool ${tool.toolDefinition.name()} with payload $payload")
        return tool.call(payload)
    }

    override fun implementsInterface(intf: Class<*>): Boolean {
        return intf.isAssignableFrom(this.intf)
    }
}

/**
 * Expose the ToolGroup as an interface
 */
fun <T> ToolCallbackPublisher.asInterface(
    intf: Class<T>,
): T = exposeAsInterface(this, intf)

inline fun <reified T> ToolGroup.asInterface(): T {
    return asInterface(T::class.java)
}
