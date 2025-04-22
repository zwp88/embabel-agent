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
package com.embabel.agent.config.openai

import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.config.OpenAiConfiguration
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import com.embabel.common.util.kotlin.loggerFor
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate


@ExcludeFromJacocoGeneratedReport(reason = "Open AI configuration can't be unit tested")
@Configuration
@ConditionalOnProperty("OPENAI_API_KEY")
class OpenAiConfiguration(
    @Value("\${OPENAI_API_KEY}")
    private val apiKey: String,
) {
    init {
        loggerFor<OpenAiConfiguration>().info("OpenAI AI models are available")
    }

    private val openAiApi = OpenAiApi.builder().apiKey(apiKey).build()

    @Bean
    fun gpr4omini(): Llm {
        val model = "gpt-4o-mini"
        return Llm.withKnowledgeCutoff(
            name = model,
            model = chatModelOf(model),
            knowledgeCutoffDate = LocalDate.of(2024, 7, 18),
        )
    }

    @Bean
    fun gpt4o(): Llm {
        val model = "gpt-4o"
        return Llm.withKnowledgeCutoff(
            name = "gpt-4o",
            model = chatModelOf(model),
            knowledgeCutoffDate = LocalDate.of(2024, 8, 6),
        )
    }

    private fun chatModelOf(model: String): ChatModel {
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder().model(model).build()
            ).build()
    }
}
