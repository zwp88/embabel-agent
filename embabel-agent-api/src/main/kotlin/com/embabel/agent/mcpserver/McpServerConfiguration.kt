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

import com.embabel.agent.api.common.Autonomy
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.spi.support.SelfToolCallbackPublisher
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Configuration
@Profile("!test")
class McpServerConfiguration {

    @Bean
    fun callbacks(autonomyTools: AutonomyTools): ToolCallbackProvider {
        loggerFor<McpServerConfiguration>().info(
            "Exposing MCP server tools:\n\t${
                autonomyTools.toolCallbacks.joinToString(
                    "\n\t"
                ) { "${it.toolDefinition.name()}: ${it.toolDefinition.description()}" }

            }"
        )
        return autonomyTools
    }
}

@Service
class AutonomyTools(private val autonomy: Autonomy) : SelfToolCallbackPublisher, ToolCallbackProvider {

    override fun getToolCallbacks(): Array<out ToolCallback?> {
        return this.toolCallbacks.toTypedArray()
    }

    // TODO fix this
    @Tool(description = "call this tool if you are asked to perform research or answer a question")
    fun chooseAgent(@ToolParam(description = "the user's intended research or question to answer") intent: String): String {
        val verbosity = Verbosity(
            showPrompts = true,
        )
        val processOptions = ProcessOptions(verbosity = verbosity)
        val dynamicExecutionResult = autonomy.chooseAndRunAgent(
            intent,
            processOptions
        )
        return when (val output = dynamicExecutionResult.output) {
            is String -> output
            is HasInfoString -> dynamicExecutionResult.output.infoString(verbose = true)
            is HasContent -> output.text
            else -> output.toString()
        }
    }
}
