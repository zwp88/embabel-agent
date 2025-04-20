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

import com.embabel.agent.spi.ToolGroupResolver
import com.embabel.common.util.kotlin.loggerFor
import org.springframework.ai.tool.ToolCallback

/**
 * Metadata about a tool group
 * @param role role of the tool group. Many tool groups can provide this
 * @param artifact name of the tool group
 * @param provider group provider
 * @param version version of the tool
 */
data class ToolGroupMetadata(
    val role: String,
    val artifact: String,
    val provider: String,
    val version: String = "0.1.0-SNAPSHOT",
)

/**
 * Allows consuming tools and exposing them to LLMs.
 * Interface allowing abstraction between tool concept
 * and specific tools.
 */
interface ToolConsumer {

    val name: String

    /**
     * ToolCallbacks exposed. This will include directly registered tools
     * and tools resolved from ToolGroups.
     */
    val toolCallbacks: Collection<ToolCallback>

    /**
     * List of ToolGroup roles this consumer needs
     */
    val toolGroups: Collection<String>

    fun resolveToolCallbacks(toolGroupResolver: ToolGroupResolver): Collection<ToolCallback> {
        val tools = mutableListOf<ToolCallback>()
        tools += toolCallbacks
        for (role in toolGroups) {
            val resolution = toolGroupResolver.resolveToolGroup(role)
            if (resolution.resolvedToolGroup == null) {
                loggerFor<ToolConsumer>().warn(
                    "Could not resolve tool group with role='{}': {}", role, resolution.failureMessage
                )
            } else {
                tools += resolution.resolvedToolGroup.toolCallbacks
            }
        }
        loggerFor<ToolConsumer>().debug(
            "{} resolved {} tools from {} tools and {} tool groups: {}",
            name,
            tools.size,
            toolCallbacks.size,
            toolGroups.size,
            tools.map { it.toolDefinition.name() },
        )
        return tools.distinctBy { it.toolDefinition.name() }
    }
}

data class ToolGroup(
    val metadata: ToolGroupMetadata,
    val toolCallbacks: Collection<ToolCallback>,
) {

    /**
     * Define well known tool groups
     */
    companion object {
        const val WEB = "web"
    }
}

/**
 * Resolution of a tool group request
 * @param failureMessage Failure message in case we could not resolve this group.
 */
data class ToolGroupResolution(
    val resolvedToolGroup: ToolGroup?,
    val failureMessage: String? = null,
)
