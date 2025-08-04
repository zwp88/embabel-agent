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
package com.embabel.agent.core

import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.Named
import com.embabel.common.util.indent
import com.embabel.common.util.indentLines

/**
 * Type known to the Embabel agent platform.
 * May be backed by a domain object or by a map.
 */
sealed interface EmbabelType : HasInfoString, Named

/**
 * Simple data type
 */
data class SchemaType(
    override val name: String,
    val properties: List<PropertyDefinition> = emptyList(),
) : EmbabelType {

    fun withProperty(
        property: PropertyDefinition,
    ): SchemaType {
        return copy(properties = properties + property)
    }

    override fun infoString(
        verbose: Boolean?,
        indent: Int,
    ): String {
        return """
                |name: $name
                |properties:
                |${properties.map { it }.joinToString("\n") { it.toString().indent(1) }}
                |"""
            .trimMargin()
            .indentLines(indent)
    }

}

data class PropertyDefinition(
    val name: String,
    val type: String = "string",
    val description: String? = name,
)

/**
 * Typed backed by a JVM object
 */
data class DomainType(
    val clazz: Class<*>,
) : EmbabelType {

    override val name: String
        get() = clazz.name

    override fun infoString(
        verbose: Boolean?,
        indent: Int,
    ): String {
        return """
                |class: ${clazz.name}
                |"""
            .trimMargin()
            .indentLines(indent)
    }

}
