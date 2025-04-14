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
package com.embabel.agent.testing

import com.embabel.agent.core.Action
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.LlmTransformer
import com.embabel.agent.core.primitive.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import java.lang.reflect.ParameterizedType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Fake LLM transformer that generates valid classes with random strings.
 */
class DummyObjectCreatingLlmTransformer(
    private val stringsToUse: List<String>,
) : LlmTransformer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val random = Random()

    override fun <I, O> doTransform(
        input: I,
        literalPrompt: String,
        llmOptions: LlmOptions,
        allToolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
    ): O {
        logger.debug("Creating fake response for class: ${outputClass.name}")

        // Create a mock instance based on the output class structure
        @Suppress("UNCHECKED_CAST")
        return createMockInstance(outputClass) as O
    }

    override fun <I, O> maybeTransform(
        input: I,
        prompt: (I) -> String,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?
    ): Result<O> {
        logger.debug("Creating fake response for class: ${outputClass.name}")

        // Create a mock instance based on the output class structure
        @Suppress("UNCHECKED_CAST")
        val o = createMockInstance(outputClass) as O

        // TODO simulate occasional failures
        return Result.success(o)
    }

    override fun <I, O> transform(
        input: I,
        prompt: (I) -> String,
        llmOptions: LlmOptions,
        toolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
        agentProcess: AgentProcess,
        action: Action?
    ): O = doTransform(input, prompt(input), llmOptions, toolCallbacks, outputClass)

    @Suppress("UNCHECKED_CAST")
    private fun <T> createMockInstance(clazz: Class<T>): Any {
        // Handle primitive types and common Java types
        when {
            clazz == String::class.java -> return getRandomLoremIpsum()
            clazz == Int::class.java || clazz == Integer::class.java -> return random.nextInt(1000)
            clazz == Long::class.java -> return random.nextLong()
            clazz == Double::class.java -> return random.nextDouble() * 100
            clazz == Float::class.java -> return random.nextFloat() * 100
            clazz == Boolean::class.java -> return random.nextBoolean()
            clazz == List::class.java -> return listOf(getRandomLoremIpsum())
            clazz.isEnum -> return clazz.enumConstants?.let { it[random.nextInt(it.size)] } ?: ""
            clazz == LocalDate::class.java -> return LocalDate.now()
            clazz == LocalDateTime::class.java -> return LocalDateTime.now()
            clazz == Date::class.java -> return Date()
        }

        // Handle List<T> with reflection
        if (List::class.java.isAssignableFrom(clazz)) {
            val genericType = findListGenericType(clazz)
            genericType?.let { componentType ->
                return createMockList(componentType, 3)
            }
        }

        // For data classes and other complex types, attempt to find a constructor
        val constructors = clazz.declaredConstructors
        if (constructors.isEmpty()) {
            throw IllegalArgumentException("No constructor found for class: ${clazz.name}")
        }

        // Choose the constructor with the most parameters
        val constructor = constructors.maxByOrNull { it.parameterCount } ?: constructors[0]
        constructor.isAccessible = true

        // Create arguments for the constructor
        val parameterTypes = constructor.genericParameterTypes
        val arguments = parameterTypes.map { paramType ->
            when {
                paramType is ParameterizedType -> {
                    val rawType = paramType.rawType as Class<*>
                    if (List::class.java.isAssignableFrom(rawType)) {
                        val componentType = paramType.actualTypeArguments[0]
                        if (componentType is Class<*>) {
                            createMockList(componentType, 3)
                        } else {
                            emptyList<Any>()
                        }
                    } else {
                        // For other generic types, try to create a mock instance
                        createMockInstance(rawType)
                    }
                }

                paramType is Class<*> -> createMockInstance(paramType)
                else -> null // Default for unknown types
            }
        }.toTypedArray()

        // Create a new instance with the constructor
        return constructor.newInstance(*arguments)
    }

    private fun findListGenericType(clazz: Class<*>): Class<*>? {
        val genericInterfaces = clazz.genericInterfaces
        for (genericInterface in genericInterfaces) {
            if (genericInterface is ParameterizedType) {
                val rawType = genericInterface.rawType as Class<*>
                if (List::class.java.isAssignableFrom(rawType)) {
                    val typeArg = genericInterface.actualTypeArguments[0]
                    if (typeArg is Class<*>) {
                        return typeArg
                    }
                }
            }
        }
        return null
    }

    private fun <T> createMockList(componentType: Class<T>, size: Int): List<Any> {
        return (0 until size).map { createMockInstance(componentType) }
    }

    private fun getRandomLoremIpsum(): String {
        val wordCount = random.nextInt(5) + 3 // Between 3 and 7 words
        return (0 until wordCount).joinToString(" ") { stringsToUse[random.nextInt(stringsToUse.size)] }
    }

    companion object {

        /**
         * A fake LLM transformer that generates Lorem Ipsum
         * style fake test
         */
        val LoremIpsum: LlmTransformer = DummyObjectCreatingLlmTransformer(
            listOf(
                "Lorem ipsum dolor sit amet", "consectetur adipiscing elit", "sed do eiusmod tempor",
                "incididunt ut labore", "et dolore magna aliqua", "Ut enim ad minim veniam",
                "quis nostrud exercitation", "ullamco laboris nisi", "ut aliquip ex ea commodo",
                "consequat Duis aute", "irure dolor in reprehenderit", "in voluptate velit esse",
                "cillum dolore eu fugiat", "nulla pariatur Excepteur", "sint occaecat cupidatat",
                "non proident sunt", "in culpa qui officia", "deserunt mollit anim", "id est laborum"
            )
        )
    }
}
