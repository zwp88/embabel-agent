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
package com.embabel.agent.mcpserver

import com.embabel.agent.common.LoggingConstants
import com.embabel.agent.core.ToolCallbackPublisher
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Tag interface extending Spring AI ToolCallbackProvider
 * that identifies tool callbacks that our MCP server exposes.
 */
interface McpToolExportCallbackPublisher : ToolCallbackPublisher

/**
 * Configures MCP server. Exposes a limited number of tools.
 */
@Configuration
@Profile("!test")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
class McpServerConfiguration(
    private val mcpToolExportCallbackPublishers: List<McpToolExportCallbackPublisher>,
) {

    /**
     * Used by Spring MCP server
     */
    @Bean
    fun callbacks(): ToolCallbackProvider {
        val allToolCallbacks = mcpToolExportCallbackPublishers.flatMap { it.toolCallbacks }
        val separator = "~".repeat(LoggingConstants.BANNER_WIDTH)
        loggerFor<McpServerConfiguration>().info(
            "\n${separator}\n{} MCP tool exporters: {}\nExposing a total of {} MCP server tools:\n\t{}\n${separator}",
            mcpToolExportCallbackPublishers.size,
            mcpToolExportCallbackPublishers,
            allToolCallbacks.size,
            allToolCallbacks.joinToString(
                "\n\t"
            ) { "${it.toolDefinition.name()}: ${it.toolDefinition.description()}" }
        )
        return object : ToolCallbackProvider {
            override fun getToolCallbacks(): Array<out ToolCallback?> {
                return allToolCallbacks.toTypedArray()
            }
        }
    }
}
