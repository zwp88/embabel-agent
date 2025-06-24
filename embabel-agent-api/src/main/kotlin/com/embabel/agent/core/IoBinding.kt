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

import kotlin.reflect.KClass

/**
 * Binding definition of form name:Type
 * If name is omitted, it is assumed to be 'it'
 * Used to build preconditions from input and output bindings.
 * Default name ("it") has a special meaning. It will be satisfied
 * by an instance of the correct type being bound to "it", but also by
 * the final result of the action having the correct type.
 */
@JvmInline
value class IoBinding(val value: String) {
    init {
        require(value.isNotBlank()) { "Type definition must not be blank" }
    }

    constructor(name: String, type: String) : this("$name:$type")

    val type: String
        get() = if (value.contains(":")) {
            value.split(":")[1]
        } else {
            value
        }

    val name: String
        get() =
            if (value.contains(":")) {
                value.split(":")[0]
            } else {
                DEFAULT_BINDING
            }

    companion object {
        /**
         * The default binding, when it is not otherwise specified.
         * Consistent with Groovy and Kotlin behavior.
         */
        const val DEFAULT_BINDING = "it"

        operator fun invoke(name: String? = DEFAULT_BINDING, type: Class<*>): IoBinding {
            return IoBinding(value = "$name:${type.name}")
        }

        operator fun invoke(name: String? = DEFAULT_BINDING, type: KClass<*>) = invoke(name = name, type = type.java)

    }

}
