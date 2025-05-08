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
import com.embabel.common.ai.model.PricingModel
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import com.embabel.common.util.loggerFor
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Ollama local models
 */
@ExcludeFromJacocoGeneratedReport(reason = "Ollama configuration can't be unit tested")
@Profile("!test")
@Configuration
class OllamaModels(
) {
    init {
        loggerFor<OllamaModels>().info("Ollama models are available")
    }

    @Bean
    fun gemma3_4b(): Llm = ollamaModelOf(GEMMA3_4B)
        .copy(
            pricingModel = PricingModel.ALL_YOU_CAN_EAT
        )

    @Bean
    fun llama3_2b(): Llm = ollamaModelOf(LLAMA3_2_3B)
        .copy(
            pricingModel = PricingModel.ALL_YOU_CAN_EAT
        )

    private fun ollamaModelOf(name: String): Llm {
        val chatModel = OllamaChatModel.builder()
            .ollamaApi(
                OllamaApi.builder()
                    .build()
            )
            .defaultOptions(
                OllamaOptions.builder()
                    .model(name)
                    .build()
            )
            .build()
        return Llm(name = name, chatModel)
    }

    companion object {

        const val GEMMA3_4B = "gemma3:4b"

        const val LLAMA3_2_3B = "llama3.2:3b"
    }
}
