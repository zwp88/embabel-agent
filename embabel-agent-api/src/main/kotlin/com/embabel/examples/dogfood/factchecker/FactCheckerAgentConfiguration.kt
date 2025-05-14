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