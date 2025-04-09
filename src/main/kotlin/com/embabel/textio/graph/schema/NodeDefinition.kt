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
package com.embabel.textio.graph.schema

data class NodeDefinition(
    val name: String,
    val properties: List<PropertyDefinition> = emptyList(),
) {

    fun withProperty(
        propertyDefinition: PropertyDefinition,
    ): NodeDefinition {
        return copy(properties = properties + properties)
    }
}

class PropertyDefinition(
    val name: String,
    val type: String = "string",
    val description: String? = name,
) {
    override fun toString(): String {
        return "PropertyDefinition(name='$name', type='$type', description=$description)"
    }
}
