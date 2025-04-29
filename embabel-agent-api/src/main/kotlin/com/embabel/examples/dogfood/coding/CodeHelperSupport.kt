package com.embabel.examples.dogfood.coding

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Superclass for code helpers
 */
abstract class CodeHelperSupport(
    val projectRepository: ProjectRepository,
    val defaultLocation: String,
) {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected val claudeSonnet = LlmOptions(
        AnthropicModels.CLAUDE_37_SONNET
    )

}