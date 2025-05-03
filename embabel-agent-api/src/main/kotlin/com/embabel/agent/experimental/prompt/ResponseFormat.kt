package com.embabel.agent.experimental.prompt

import com.embabel.common.ai.prompt.PromptContributor

data class ResponseFormat(
    val format: String,
) : PromptContributor {

    override fun contribution(): String =
        """
            # RESPONSE FORMAT #
            $format
        """.trimIndent()

    override val role: String = "response_format"

    companion object {
        val MARKDOWN = ResponseFormat("Markdown")

    }
}