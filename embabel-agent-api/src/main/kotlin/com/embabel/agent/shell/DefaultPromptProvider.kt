package com.embabel.agent.shell

import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.springframework.shell.jline.PromptProvider

/**
 * Vanilla prompt provider
 */
class DefaultPromptProvider : PromptProvider {
    override fun getPrompt() = AttributedString(
        "embabel> ",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
    )
}