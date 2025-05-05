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
import com.embabel.common.ai.model.OptionsConverter
import com.embabel.common.ai.model.PerTokenPricingModel
import com.embabel.common.ai.model.config.OpenAiConfiguration
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import com.embabel.common.util.loggerFor
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
class OpenAiModels(
    @Value("\${OPENAI_API_KEY}")
    private val apiKey: String,
) {
    init {
        loggerFor<OpenAiConfiguration>().info("OpenAI AI models are available")
    }

    private val openAiApi = OpenAiApi.builder().apiKey(apiKey).build()

    @Bean
    fun gpt41mini(): Llm {
        val model = GPT_41_MINI
        return Llm(
            name = model,
            model = chatModelOf(model),
            knowledgeCutoffDate = LocalDate.of(2024, 7, 18),
        ).copy(
            pricingModel = PerTokenPricingModel(
                usdPer1mInputTokens = .40,
                usdPer1mOutputTokens = 1.6,
            )
        )
    }

    @Bean
    fun gpt41(): Llm {
        val model = GPT_41
        return Llm(
            name = model,
            model = chatModelOf(model),
            optionsConverter = optionsConverter,
            knowledgeCutoffDate = LocalDate.of(2024, 8, 6),
        )
            .copy(
                pricingModel = PerTokenPricingModel(
                    usdPer1mInputTokens = 2.0,
                    usdPer1mOutputTokens = 8.0,
                )
            )
    }

    @Bean
    fun gpt41nano(): Llm {
        val model = GPT_41_NANO
        return Llm(
            name = model,
            model = chatModelOf(model),
            optionsConverter = optionsConverter,
            knowledgeCutoffDate = LocalDate.of(2024, 8, 6),
        )
            .copy(
                pricingModel = PerTokenPricingModel(
                    usdPer1mInputTokens = .1,
                    usdPer1mOutputTokens = .4,
                )
            )
    }

    private fun chatModelOf(model: String): ChatModel {
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder().model(model).build()
            ).build()
    }

    private val optionsConverter: OptionsConverter = { options ->
        OpenAiChatOptions.builder()
            .temperature(options.temperature)
            .build()
    }

    companion object {

        const val GPT_41_MINI = "gpt-4.1-mini"

        const val GPT_41 = "gpt-4.1"

        const val GPT_41_NANO = "gpt-4.1-nano"
    }
}
