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
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.OptionsConverter
import com.embabel.common.ai.model.PerTokenPricingModel
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.ai.retry.TransientAiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.support.RetryTemplate
import java.time.Duration
import java.time.LocalDate

@ConfigurationProperties(prefix = "anthropic")
data class AnthropicProperties(
    val maxAttempts: Int = 2,
)

/**
 * Anthropic models are often overloaded, so we fall back to OpenAI if it's available.
 */
@Configuration
@ConditionalOnProperty("ANTHROPIC_API_KEY")
@Profile("!test")
@ExcludeFromJacocoGeneratedReport(reason = "Anthropic configuration can't be unit tested")
class AnthropicModels(
    llms: List<Llm>,
    private val properties: AnthropicProperties,
) {
    private val logger = LoggerFactory.getLogger(AnthropicModels::class.java)

    // Don't try too hard
    private val retryTemplate = RetryTemplate.builder()
        .maxAttempts(properties.maxAttempts)
        .retryOn(TransientAiException::class.java)
        .exponentialBackoff(Duration.ofMillis(2000L), 5.0, Duration.ofMillis(180000L))
        .withListener(object : RetryListener {
            override fun <T : Any?, E : Throwable?> onError(
                context: RetryContext?,
                callback: RetryCallback<T?, E?>?,
                throwable: Throwable?
            ) {
                logger.debug("Retry error. Retry count:" + context?.retryCount, throwable);
            }
        })
        .build()

    val gpt41 = llms.find { it.name == OpenAiModels.GPT_41 }
    val gpt41mini = llms.find { it.name == OpenAiModels.GPT_41_MINI }

    init {
        logger.info("Anthropic models are available: $properties")
        if (gpt41 != null) {
            logger.info("✅ Using ${gpt41!!.name} fallback")
        } else {
            logger.info("❌ ${gpt41!!.name} fallback not available")
        }
        if (gpt41mini != null) {
            logger.info("✅ Using ${gpt41mini!!.name} fallback")
        } else {
            logger.info("❌ ${gpt41mini!!.name} fallback not available")
        }
    }

    private val keywords = listOf(
        "overloaded",
        "busy",
        "rate_limit",
        "throttled",
        "quota",
        "organization",
    )

    private val flipTrigger: ((Throwable) -> Boolean) = { t ->
        when (t) {
            is NonTransientAiException -> true
            is TransientAiException -> {
                val msg = t.message?.lowercase() ?: ""
                keywords.any { msg.contains(it) }
            }

            else -> true
        }
    }

    @Bean
    fun claudeOpus4(
        @Value("\${ANTHROPIC_API_KEY}") apiKey: String,
    ): Llm {
        return anthropicModelOf(
            CLAUDE_40_OPUS, apiKey,
            knowledgeCutoffDate = LocalDate.of(2025, 3, 31),
            maxTokens = 200000,
        )
            .withFallback(fallbackTo = gpt41, whenError = flipTrigger)
            .copy(
                pricingModel = PerTokenPricingModel(
                    usdPer1mInputTokens = 15.0,
                    usdPer1mOutputTokens = 75.0,
                )
            )
    }

    @Bean
    fun claudeSonnet(
        @Value("\${ANTHROPIC_API_KEY}") apiKey: String,
    ): Llm {
        return anthropicModelOf(
            CLAUDE_37_SONNET, apiKey, knowledgeCutoffDate = LocalDate.of(2024, 10, 31),
            maxTokens = 20000,
        )
            .withFallback(fallbackTo = gpt41, whenError = flipTrigger)
            .copy(
                pricingModel = PerTokenPricingModel(
                    usdPer1mInputTokens = 3.0,
                    usdPer1mOutputTokens = 15.0,
                )
            )
    }

    @Bean
    fun claudeHaiku(
        @Value("\${ANTHROPIC_API_KEY}") apiKey: String,
    ): Llm = anthropicModelOf(
        CLAUDE_35_HAIKU, apiKey,
        knowledgeCutoffDate = LocalDate.of(2024, 10, 22),
        maxTokens = 8192,
    )
        .withFallback(fallbackTo = gpt41mini, whenError = flipTrigger)
        .copy(
            pricingModel = PerTokenPricingModel(
                usdPer1mInputTokens = .80,
                usdPer1mOutputTokens = 4.0,
            )
        )

    private fun anthropicModelOf(
        name: String,
        apiKey: String,
        knowledgeCutoffDate: LocalDate?,
        maxTokens: Int,
    ): Llm {
        // Claude seems to need max tokens
        val chatModel = AnthropicChatModel
            .builder()
            .anthropicApi(
                AnthropicApi.builder()
                    .apiKey(apiKey)
                    .build()
            )
            .retryTemplate(retryTemplate)
            .build()
        return Llm(
            name = name,
            model = chatModel,
            provider = PROVIDER,
            optionsConverter = AnthropicOptionsConverter,
            knowledgeCutoffDate = knowledgeCutoffDate,
        )
    }


    companion object {

        const val CLAUDE_37_SONNET = "claude-3-7-sonnet-latest"

        const val CLAUDE_35_HAIKU = "claude-3-5-haiku-latest"

        const val CLAUDE_40_OPUS = "claude-opus-4-20250514"

        const val PROVIDER = "Anthropic"
    }

}

object AnthropicOptionsConverter : OptionsConverter<AnthropicChatOptions> {
    override fun convertOptions(options: LlmOptions): AnthropicChatOptions =
        AnthropicChatOptions.builder()
            .temperature(options.temperature)
            .topP(options.topP)
            .maxTokens(options.maxTokens)
            .thinking(
                if (options.thinking != null) AnthropicApi.ChatCompletionRequest.ThinkingConfig(
                    AnthropicApi.ThinkingType.ENABLED,
                    options.thinking!!.tokenBudget,
                ) else null
            )
//            .presencePenalty(options.presencePenalty)
//            .frequencyPenalty(options.frequencyPenalty)
            .topP(options.topP)
            .build()
}
