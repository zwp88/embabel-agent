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

fun interface MessageListener {
    fun onMessage(message: Message)
}

class MessageSavingMessageListener(
    private val messageList: MutableList<Message> = mutableListOf()
) : MessageListener {
    override fun onMessage(message: Message) {
        messageList.add(message)
    }

    fun messages(): List<Message> = messageList.toList()

}

/**
 * Simplest possible conversation session implementation
 */
interface ChatSession {

    val conversation: Conversation

    val messageListener: MessageListener

    /**
     * Any response messages will be sent to the messageListener
     */
    fun send(message: UserMessage, additionalListener: MessageListener)
}
