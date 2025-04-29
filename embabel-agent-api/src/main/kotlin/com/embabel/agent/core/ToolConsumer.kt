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

interface ToolGroupDescription {

    /**
     * Natural language description of the tool group.
     * May be used by an LLM to choose tool groups so should be informative.
     * Tool groups with the same role should have similar descriptions,
     * although they should call out any unique features.
     */
    val description: String

    /**
     * Role of the tool group. Many tool groups can provide this
     * Multiple tool groups can provide the same role,
     * for example with different QoS.
     */
    val role: String

    companion object {
        operator fun invoke(
            description: String,
            role: String,
        ): ToolGroupDescription = ToolGroupDescriptionImpl(
            description = description,
            role = role,
        )
    }

}

private data class ToolGroupDescriptionImpl(
    override val description: String,
    override val role: String,
) : ToolGroupDescription

enum class ToolGroupPermission {
    /**
     * Tool group can be used to modify local resources.
     * This is a strong permission and should be used with caution.
     */
    HOST_ACCESS,

    /**
     * Tool group accesses the internet.
     */
    INTERNET_ACCESS,
}

/**
 * Metadata about a tool group. Interface as platforms
 * may extend it
 */
interface ToolGroupMetadata : ToolGroupDescription {

    /**
     * Name of the tool group
     */
    val artifact: String

    /**
     * Provider of the tool group
     */
    val provider: String

    /**
     * Version of the tool group
     */
    val version: String

    /**
     * What this tool group's tools can do.
     */
    val permissions: Set<ToolGroupPermission>

    companion object {
        operator fun invoke(
            description: String,
            role: String,
            artifact: String,
            provider: String,
            permissions: Set<ToolGroupPermission>,
            version: String = DEFAULT_VERSION,
        ): ToolGroupMetadata = MinimalToolGroupMetadata(
            description = description,
            role = role,
            artifact = artifact,
            provider = provider,
            permissions = permissions,
            version = version,
        )

        operator fun invoke(
            description: ToolGroupDescription,
            artifact: String,
            provider: String,
            permissions: Set<ToolGroupPermission>,
            version: String = DEFAULT_VERSION,
        ): ToolGroupMetadata = MinimalToolGroupMetadata(
            description = description.description,
            role = description.role,
            artifact = artifact,
            provider = provider,
            permissions = permissions,
            version = version,
        )
    }

}

private data class MinimalToolGroupMetadata(
    override val description: String,
    override val role: String,
    override val artifact: String,
    override val provider: String,
    override val permissions: Set<ToolGroupPermission>,
    override val version: String = DEFAULT_VERSION,
) : ToolGroupMetadata


interface ToolCallbackSpec {

    /**
     * Tool callbacks referenced or exposed.
     */
    val toolCallbacks: Collection<ToolCallback>

}

interface ToolCallbackConsumer : ToolCallbackSpec

interface ToolGroupConsumer {

    /**
     * Tool groups exposed. This will include directly registered tool groups
     * and tool groups resolved from ToolGroups.
     */
    val toolGroups: Collection<String>
}

/**
 * Allows consuming tools and exposing them to LLMs.
 * Interface allowing abstraction between tool concept
 * and specific tools.
 */
interface ToolConsumer : ToolCallbackConsumer, ToolGroupConsumer {

    val name: String

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

interface ToolCallbackPublisher : ToolCallbackSpec

interface ToolGroup : ToolCallbackPublisher {

    val metadata: ToolGroupMetadata

    /**
     * Define well known tool groups
     */
    companion object {

        operator fun invoke(
            metadata: ToolGroupMetadata,
            toolCallbacks: Collection<ToolCallback>,
        ): ToolGroup = ToolGroupImpl(
            metadata = metadata,
            toolCallbacks = toolCallbacks,
        )

        const val WEB = "web"

        val WEB_DESCRIPTION = ToolGroupDescription(
            description = "Tools for web search and scraping",
            role = WEB,
        )

        const val FILE = "file"
        val FILE_DESCRIPTION = ToolGroupDescription(
            description = "Tools for file and directory operations",
            role = FILE,
        )

        const val CI = "code"
        val CI_DESCRIPTION = ToolGroupDescription(
            description = "Tools for running CI on a project",
            role = CI,
        )
    }
}

private data class ToolGroupImpl(
    override val metadata: ToolGroupMetadata,
    override val toolCallbacks: Collection<ToolCallback>,
) : ToolGroup

/**
 * Resolution of a tool group request
 * @param failureMessage Failure message in case we could not resolve this group.
 */
data class ToolGroupResolution(
    val resolvedToolGroup: ToolGroup?,
    val failureMessage: String? = null,
)
