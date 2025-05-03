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
package com.embabel.agent.config

import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.event.logging.LoggingAgenticEventListener
import com.embabel.agent.event.logging.personality.ColorPalette
import com.embabel.agent.event.logging.personality.DefaultColorPalette
import com.embabel.agent.shell.DefaultPromptProvider
import com.embabel.agent.spi.*
import com.embabel.agent.spi.support.*
import com.embabel.common.ai.model.*
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.NameGenerator
import com.embabel.common.textio.template.JinjavaTemplateRenderer
import com.embabel.common.textio.template.TemplateRenderer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.shell.jline.PromptProvider
import org.springframework.web.client.RestTemplate

/**
 * Core configuration for AgentPlatform
 */
@Configuration
@EnableConfigurationProperties(ModelProperties::class)
class AgentPlatformConfiguration(
) {

    /**
     * Used for process id generation
     */
    @Bean
    fun nameGenerator(): NameGenerator = MobyNameGenerator

    @Bean
    fun toolDecorator(toolGroupResolver: ToolGroupResolver): ToolDecorator = DefaultToolDecorator(
        toolGroupResolver,
    )

    @Bean
    fun templateRenderer(): TemplateRenderer = JinjavaTemplateRenderer()

    /**
     * Fallback if we don't have a more interesting logger
     */
    @Bean
    @ConditionalOnMissingBean(LoggingAgenticEventListener::class)
    fun defaultLogger(): AgenticEventListener = LoggingAgenticEventListener()

    /**
     * Fallback if we don't have a more interesting prompt provider
     */
    @Bean
    @ConditionalOnMissingBean(PromptProvider::class)
    fun defaultPromptProvider(): PromptProvider = DefaultPromptProvider()

    @Bean
    @ConditionalOnMissingBean(ColorPalette::class)
    fun defaultColorPalette(): ColorPalette = DefaultColorPalette()

    @Bean
    fun restTemplate() = RestTemplate()

    @Bean
    fun ranker(llmOperations: LlmOperations): Ranker = LlmRanker(
        llmOperations = llmOperations,
        llm = LlmOptions(OpenAiModels.GPT_4o),
        maxAttempts = 3,
    )

    @Bean
    fun agentProcessRepository(): AgentProcessRepository = InMemoryAgentProcessRepository()

    @Bean
    fun toolGroupResolver(toolGroups: List<ToolGroup>): ToolGroupResolver = RegistryToolGroupResolver(
        name = "RegistryToolGroupResolver",
        toolGroups
    )

    @Bean
    fun actionScheduler(): OperationScheduler =
        ProcessOptionsOperationScheduler()

    @Bean
    fun modelProvider(
        llms: List<Llm>,
        embeddingServices: List<EmbeddingService>,
        properties: ModelProperties,
    ): ModelProvider = ApplicationPropertiesModelProvider(
        llms = llms,
        embeddingServices = embeddingServices,
        properties = properties,
    )

}
