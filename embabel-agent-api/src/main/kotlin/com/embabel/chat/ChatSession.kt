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
package com.embabel.chat

import com.embabel.agent.channel.AssistantMessageOutputChannelEvent
import com.embabel.agent.channel.OutputChannel
import com.embabel.agent.identity.User

/**
 * Simplest possible conversation session implementation
 * Responsible for keeping its conversation up to date
 */
interface ChatSession {

    val outputChannel: OutputChannel

    /**
     * The Embabel User if known, null if not.
     */
    val user: User?

    /**
     * Conversation history. Kept up to date.
     */
    val conversation: Conversation

    /**
     * Subclasses should override this to provide a process ID if available.
     */
    val processId: String? get() = null

    /**
     * Update the conversation with a new message
     * and respond to it.
     * Any response messages will be sent to the messageListener,
     * but also should be added to the conversation.
     * @param userMessage message to send
     */
    fun respond(
        userMessage: UserMessage,
    )

    /**
     * Convenience method to add a message to the conversation
     */
    fun saveAndSend(message: AssistantMessage) {
        conversation.addMessage(message)
        outputChannel.send(
            AssistantMessageOutputChannelEvent(
                processId = processId ?: "anonymous",
                AssistantMessage(
                    content = message.content,
                    name = null,
                ),
            )
        )
    }
}
