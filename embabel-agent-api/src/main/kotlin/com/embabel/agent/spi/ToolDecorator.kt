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

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.core.AgentProcess
import org.springframework.ai.tool.ToolCallback

/**
 * Decorate tools for use on the platform
 */
fun interface ToolDecorator {

    /**
     * Decorate the tool with some extra information.
     * @param tool The tool to decorate.
     * @return The decorated tool.
     */
    fun decorate(
        tool: ToolCallback,
        agentProcess: AgentProcess,
        llmOptions: LlmOptions,
    ): ToolCallback
}
