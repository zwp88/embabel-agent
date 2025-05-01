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
package com.embabel.agent.core.support

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks

/**
 * ToolCallbacks.from complains if no tools.
 * Also conceal varargs
 */
fun safelyGetToolCallbacks(instances: Collection<Any>): List<ToolCallback> {
    val callbacks = mutableListOf<ToolCallback>()
    instances.forEach {
        if (it is ToolCallback) {
            callbacks.add(it)
        } else try {
            callbacks.addAll(ToolCallbacks.from(it).toList())
        } catch (_: IllegalStateException) {
        }
    }

    return callbacks
}

fun safelyGetToolCallbacksFrom(instance: Any): List<ToolCallback> =
    safelyGetToolCallbacks(listOf(instance))
