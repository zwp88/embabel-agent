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

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.event.logging.LoggingAgenticEventListener
import com.embabel.agent.shell.DefaultPromptProvider
import com.embabel.agent.spi.ToolGroupResolver
import com.embabel.agent.spi.support.RegistryToolGroupResolver
import com.embabel.common.ai.model.*
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.NameGenerator
import com.embabel.common.textio.template.JinjavaTemplateRenderer
import com.embabel.common.textio.template.TemplateRenderer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.shell.jline.PromptProvider
import org.springframework.web.client.RestTemplate


@Configuration
class AgentPlatformConfiguration(
) {

    /**
     * Used for process id generation
     */
    @Bean
    fun nameGenerator(): NameGenerator = MobyNameGenerator

    @Bean
    fun templateRenderer(): TemplateRenderer = JinjavaTemplateRenderer()

    @Bean
    @ConditionalOnMissingBean(LoggingAgenticEventListener::class)
    fun defaultLogger(): AgenticEventListener = LoggingAgenticEventListener()

    @Bean
    @ConditionalOnMissingBean(PromptProvider::class)
    fun defaultPromptProvider(): PromptProvider = DefaultPromptProvider()

    @Bean
    fun restTemplate() = RestTemplate()

    @Bean
    fun toolGroupResolver(toolGroups: List<ToolGroup>): ToolGroupResolver = RegistryToolGroupResolver(
        name = "RegistryToolGroupResolver",
        toolGroups
    )

    @Bean
    fun modelProvider(
        llms: List<Llm>,
        embeddingServices: List<EmbeddingService>,
        properties: ModelProperties,
    ): ModelProvider {

        return ApplicationPropertiesModelProvider(
            llms = llms,
            embeddingServices = embeddingServices,
            properties = properties,
        )

    }

}
