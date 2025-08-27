package com.embabel.agent.spi.support

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Properties for the ChatClientLlmOperations operations
 * @param maybePromptTemplate template to use for the "maybe" prompt, which
 *  * can enable a failure result if the LLM does not have enough information to
 *  * create the desired output structure.
 */
@ConfigurationProperties(prefix = "embabel.llm-operations.prompts")
data class LlmOperationsPromptsProperties(
    val maybePromptTemplate: String = "maybe_prompt_contribution",
    val generateExamplesByDefault: Boolean = true,
    val defaultTimeout: Duration = Duration.ofSeconds(60),
)