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
package com.embabel.common.core.util

import com.embabel.common.util.loggerFor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.util.ClassUtils
import java.lang.reflect.ParameterizedType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


/**
 * Create mock instances of objects.
 * This is useful for testing purposes and for creating
 * instances we may want to introspect.
 * @param stringsToUse array of string choices for String fields.
 * Defaults to lorem ipsum values
 */
open class DummyInstanceCreator(
    protected val stringsToUse: List<String> = LoremIpsums,
) {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val random = Random()

    /**
     * Create a dummy instance of this class
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> createDummyInstance(clazz: Class<T>): Any {
        logger.debug("Creating mock instance for class {}", clazz.name)
        // Handle primitive types and common Java types
        when {
            clazz == String::class.java -> return getRandomString()
            clazz == Int::class.java || clazz == Integer::class.java -> return random.nextInt(1000)
            clazz == Long::class.java -> return random.nextLong()
            clazz == Double::class.java -> return random.nextDouble() * 100
            clazz == Float::class.java -> return random.nextFloat() * 100
            clazz == Boolean::class.java -> return random.nextBoolean()
            clazz == List::class.java -> return listOf(getRandomString())
            clazz.isEnum -> return clazz.enumConstants?.let { it[random.nextInt(it.size)] } ?: ""
            clazz == LocalDate::class.java -> return LocalDate.now()
            clazz == LocalDateTime::class.java -> return LocalDateTime.now()
            clazz == Instant::class.java -> return Instant.now()
            clazz == Date::class.java -> return Date()
            clazz == Duration::class.java -> return Duration.ofMillis(random.nextLong())
        }

        // Handle List<T> with reflection
        if (List::class.java.isAssignableFrom(clazz)) {
            val genericType = findListGenericType(clazz)
            genericType?.let { componentType ->
                return createDummyList(componentType, 3)
            }
        }

        // For data classes and other complex types, attempt to find a constructor
        val constructors = clazz.declaredConstructors
        if (constructors.isEmpty()) {
            if (clazz.isInterface) {
                // Try to find an implementation
                val implementations = findImplementationsOnClasspath(clazz)
                val impl = implementations.random()
                logger.info("Using implementation {} of interface {}", impl.name, clazz.name)
                return createDummyInstance(impl)
            }
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
                            createDummyList(componentType, 3)
                        } else {
                            emptyList<Any>()
                        }
                    } else {
                        // For other generic types, try to create a mock instance
                        createDummyInstance(rawType)
                    }
                }

                paramType is Class<*> -> createDummyInstance(paramType)
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

    private fun <T> createDummyList(componentType: Class<T>, size: Int): List<Any> {
        return (0 until size).map { createDummyInstance(componentType) }
    }

    private fun getRandomString(): String {
        val wordCount = random.nextInt(5) + 3 // Between 3 and 7 words
        return (0 until wordCount).joinToString(" ") { stringsToUse[random.nextInt(stringsToUse.size)] }
    }

    companion object {

        val LoremIpsums = listOf(
            "Lorem ipsum dolor sit amet", "consectetur adipiscing elit", "sed do eiusmod tempor",
            "incididunt ut labore", "et dolore magna aliqua", "Ut enim ad minim veniam",
            "quis nostrud exercitation", "ullamco laboris nisi", "ut aliquip ex ea commodo",
            "consequat Duis aute", "irure dolor in reprehenderit", "in voluptate velit esse",
            "cillum dolore eu fugiat", "nulla pariatur Excepteur", "sint occaecat cupidatat",
            "non proident sunt", "in culpa qui officia", "deserunt mollit anim", "id est laborum"
        )

        /**
         * A dummy instance creator that uses Lorem Ipsum strings
         */
        val LoremIpsum = DummyInstanceCreator(
            stringsToUse = LoremIpsums,
        )
    }
}


/**
 * Finds implementations of a given interface on the classpath using Spring utilities
 * @param interfaceClass The interface to find implementations for
 * @param basePackage The base package to scan (e.g. "com.yourcompany")
 * @return List of classes that implement the given interface
 */
fun <T> findImplementationsOnClasspath(
    interfaceClass: Class<T>,
    basePackage: String? = null,
): List<Class<out T>> {
    require(interfaceClass.isInterface) { "${interfaceClass.simpleName} is not an interface" }

    val resolver = PathMatchingResourcePatternResolver()
    val metadataReaderFactory = CachingMetadataReaderFactory(resolver)
    val implementations = mutableListOf<Class<out T>>()

    val packageToScan = basePackage ?: run {
        val packageParts = interfaceClass.name.split(".")
        if (packageParts.size >= 2) {
            // Take first 2 parts of the package
            "${packageParts[0]}.${packageParts[1]}"
        } else {
            // If the package doesn't have 2 parts, use the full package
            TODO()
        }
    }
    loggerFor<DummyInstanceCreator>().info(
        "Looking under package [{}] for interface {}",
        packageToScan,
        interfaceClass.name,
    )

    // Convert package path to resource path
    val packageSearchPath = "classpath*:" +
            ClassUtils.convertClassNameToResourcePath(packageToScan) +
            "/**/*.class"

    try {
        // Find all class resources in the specified package
        val resources = resolver.getResources(packageSearchPath)

        for (resource in resources) {
            if (resource.isReadable) {
                val metadataReader: MetadataReader = metadataReaderFactory.getMetadataReader(resource)
                val className = metadataReader.classMetadata.className

                try {
                    val candidateClass = Class.forName(className)

                    // Check if class is a non-interface, non-abstract class that implements our interface
                    if (!candidateClass.isInterface &&
                        !java.lang.reflect.Modifier.isAbstract(candidateClass.modifiers) &&
                        interfaceClass.isAssignableFrom(candidateClass)
                    ) {

                        @Suppress("UNCHECKED_CAST")
                        implementations.add(candidateClass as Class<out T>)
                    }
                } catch (e: Exception) {
                    // Skip classes that can't be loaded
                    println("Failed to load class: $className, reason: ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        println("Error scanning classpath: ${e.message}")
    }

    return implementations
}
