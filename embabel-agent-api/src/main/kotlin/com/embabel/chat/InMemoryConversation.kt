package com.embabel.chat

import com.embabel.common.core.MobyNameGenerator

data class InMemoryConversation(
    override val id: String = MobyNameGenerator.generateName(),
    override val messages: List<Message> = emptyList(),
    private val persistent: Boolean = false,
) : Conversation {

    override fun withMessage(message: Message): Conversation {
        return copy(
            messages = messages + message,
        )
    }

    override fun persistent(): Boolean = persistent
}