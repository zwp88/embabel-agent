package com.embabel.agent.shell

import com.embabel.agent.event.logging.personality.LumonColors
import com.embabel.agent.event.logging.personality.character
import com.embabel.common.util.MessageGenerator
import com.embabel.common.util.color
import org.jline.utils.AttributedString
import org.springframework.shell.jline.PromptProvider

/**
 * Draw prompts from random messages from the given file
 */
open class MessageGeneratorPromptProvider(
    private val color: Int,
    private val messageGenerator: MessageGenerator,
) : PromptProvider {

    override fun getPrompt(): AttributedString {
        val msg = messageGenerator.generate()
        val (character, text) = if (":" in msg) {
            msg.split(":", limit = 2).map { it.trim() }
        } else {
            listOf("", msg.trim())
        }
        return AttributedString(
            character(character, text).color(color) + "\nLumon> ".color(LumonColors.Membrane),
        )
    }

}