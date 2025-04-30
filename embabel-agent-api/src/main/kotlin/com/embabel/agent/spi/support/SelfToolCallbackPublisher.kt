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
package com.embabel.agent.spi.support

import com.embabel.agent.core.*
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks

/**
 * Convenient interface a class can implement to publish any @Tool
 * functions automatically.
 */
interface SelfToolCallbackPublisher : ToolCallbackPublisher {

    override val toolCallbacks: Collection<ToolCallback>
        get() = ToolCallbacks.from(this).toList()
}

interface SelfToolGroup : SelfToolCallbackPublisher, ToolGroup {

    val description: ToolGroupDescription

    val artifact: String get() = javaClass.name

    val provider: String get() = "Embabel"

    val version: String get() = DEFAULT_VERSION

    val permissions: Set<ToolGroupPermission>

    override val metadata: ToolGroupMetadata
        get() = ToolGroupMetadata(
            description = description,
            artifact = artifact,
            provider = provider,
            permissions = permissions,
            version = version,
        )
}
