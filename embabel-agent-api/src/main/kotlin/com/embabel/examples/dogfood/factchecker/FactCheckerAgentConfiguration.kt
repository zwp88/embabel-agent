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
package com.embabel.examples.dogfood.factchecker

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.Agent
import com.embabel.common.ai.model.LlmOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class FactCheckerAgentConfiguration {
    @Bean
    fun factChecker(): Agent {
        return factCheckerAgent(
            llms = listOf(
                LlmOptions.Companion(OpenAiModels.Companion.GPT_41_MINI).withTemperature(.3),
                LlmOptions.Companion(AnthropicModels.Companion.CLAUDE_35_HAIKU).withTemperature(.0),
            )
        )
    }
}
