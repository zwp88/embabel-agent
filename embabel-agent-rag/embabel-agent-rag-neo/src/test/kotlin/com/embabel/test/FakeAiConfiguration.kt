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
package com.embabel.test

import com.embabel.common.ai.model.DefaultOptionsConverter
import com.embabel.common.ai.model.EmbeddingService
import com.embabel.common.ai.model.Llm
import io.mockk.mockk
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.TestPropertySource

/**
 * Parallels the AiConfiguration class in src/main/java/com/embabel/server/AiConfiguration.kt.
 * Enables tests to run without OPENAI_API_KEY.
 */
@Configuration
@Profile("test")
internal class FakeAiConfiguration {

    private val logger = LoggerFactory.getLogger(FakeAiConfiguration::class.java)

    init {
        logger.info("Using fake AI configuration")
    }

    @Bean
    fun cheapest(): Llm {
        return Llm(
            name = "cheapest",
            model = mockk<ChatModel>(),
            provider = "hello",
            optionsConverter = DefaultOptionsConverter
        )
    }

    @Bean
    fun best(): Llm {
        return Llm(
            name = "test-llm",
            model = mockk<ChatModel>(),
            provider = "hello",
            optionsConverter = DefaultOptionsConverter
        )
    }

    @Bean
    fun embedding(): EmbeddingService {
        return EmbeddingService(
            name = "test",
            model = FakeEmbeddingModel(),
            provider = "hello",
        )
    }

    @Bean(name = ["bedrockModels"]) fun bedrockModels() = Any()
}
