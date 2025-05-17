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
package com.embabel.agent.spi

import com.embabel.agent.event.AgentProcessToolCallResponseEvent

data class ToolStats(
    val name: String,
    val calls: Int
)

data class ToolsStats(
    val stats: Map<String, ToolStats> = emptyMap()
) {

    fun record(e: AgentProcessToolCallResponseEvent) {
        stats[e.function]?.let {
            it.copy(calls = it.calls + 1)
        } ?: ToolStats(name = e.function, 1)
    }
}
