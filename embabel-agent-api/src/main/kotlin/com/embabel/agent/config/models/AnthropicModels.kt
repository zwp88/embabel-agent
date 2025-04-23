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
package com.embabel.agent.config.models

import com.embabel.common.ai.model.Llm
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import com.embabel.common.util.kotlin.loggerFor
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate

@Configuration
@ConditionalOnProperty("ANTHROPIC_API_KEY")
@ExcludeFromJacocoGeneratedReport(reason = "Anthropic configuration can't be unit tested")
class AnthropicModels {

    init {
        loggerFor<AnthropicModels>().info("Anthropic models are available")
    }

    @Bean
    fun claudeSonnet(
        @Value("\${ANTHROPIC_API_KEY}") apiKey: String,
    ): Llm = anthropicModelOf(CLAUDE_37_SONNET, apiKey, .0, knowledgeCutoffDate = LocalDate.of(2024, 10, 31))

    @Bean
    fun claudeHaiku(
        @Value("\${ANTHROPIC_API_KEY}") apiKey: String,
    ): Llm = anthropicModelOf(CLAUDE_35_HAIKU, apiKey, .0, knowledgeCutoffDate = LocalDate.of(2024, 10, 22))

    private fun anthropicModelOf(
        name: String,
        apiKey: String,
        temperature: Double,
        knowledgeCutoffDate: LocalDate?,
    ): Llm {
        val chatModel = AnthropicChatModel.builder()
            .anthropicApi(AnthropicApi(apiKey))
            .defaultOptions(
                AnthropicChatOptions.builder()
                    .model(name)
                    .temperature(temperature)
                    .maxTokens(1000)
                    .build()
            )
            .build()
        return Llm(
            name = name,
            model = chatModel,
            knowledgeCutoffDate = knowledgeCutoffDate,
        )
    }

    companion object {

        const val CLAUDE_37_SONNET = "claude-3-7-sonnet-latest"

        const val CLAUDE_35_HAIKU = "claude-3-5-haiku-latest"
    }

}
