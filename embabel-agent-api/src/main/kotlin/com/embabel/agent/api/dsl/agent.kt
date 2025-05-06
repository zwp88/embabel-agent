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
package com.embabel.agent.api.dsl

import com.embabel.agent.core.Agent
import com.embabel.agent.core.DEFAULT_VERSION
import com.embabel.common.ai.prompt.PromptContributor
import org.springframework.ai.tool.ToolCallback

/**
 * Surface area of DSL for creating an agent.
 */
fun agent(
    name: String,
    version: String = DEFAULT_VERSION,
    description: String,
    toolGroups: List<String> = emptyList(),
    toolCallbacks: Collection<ToolCallback> = emptyList(),
    promptContributors: List<PromptContributor> = emptyList(),
    block: AgentBuilder.() -> Unit,
): Agent {
    return AgentBuilder(
        name = name,
        version = version,
        description = description,
        toolGroups = toolGroups,
        toolCallbacks = toolCallbacks,
        promptContributors = promptContributors,
    )
        .apply(block)
        .build()
}
