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

import com.embabel.agent.core.DEFAULT_VERSION
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.core.ToolGroupMetadata.Companion.invoke
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks

/**
 * Convenient interface a class can implement to be a tool group
 */
interface SelfToolGroup : ToolGroup {

    val description: ToolGroupDescription

    val artifact: String get() = javaClass.name

    val provider: String get() = "Embabel"

    val version: String get() = DEFAULT_VERSION

    override val metadata: ToolGroupMetadata
        get() = ToolGroupMetadata(
            description = description,
            artifact = artifact,
            provider = provider,
            version = version,
        )

    override val toolCallbacks: Collection<ToolCallback>
        get() = ToolCallbacks.from(this).toList()
}
