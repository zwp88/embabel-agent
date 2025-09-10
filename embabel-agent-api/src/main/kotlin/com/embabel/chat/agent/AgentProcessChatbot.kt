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
package com.embabel.chat.agent

import com.embabel.agent.channel.DiagnosticOutputChannelEvent
import com.embabel.agent.channel.OutputChannel
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.identity.User
import com.embabel.chat.ChatSession
import com.embabel.chat.Chatbot
import com.embabel.chat.Conversation
import com.embabel.chat.UserMessage
import com.embabel.chat.support.InMemoryConversation

/**
 * Chatbot implementation backed by an AgentProcess
 * The AgentProcess must react to messages and respond on its output channel
 */
class AgentProcessChatbot(
    private val agentPlatform: AgentPlatform,
    private val agent: Agent,
) : Chatbot {

    override fun createSession(
        user: User?,
        outputChannel: OutputChannel,
        systemMessage: String?,
    ): ChatSession {
        val agentProcess = agentPlatform.createAgentProcess(
            agent = agent,
            processOptions = ProcessOptions(
                outputChannel = outputChannel,
            ),
            bindings = emptyMap(),
        )
        // Should end up waiting
        agentPlatform.start(agentProcess)
        return AgentProcessChatSession(agentProcess)
    }

    override fun findSession(conversationId: String): ChatSession? {
        return agentPlatform.getAgentProcess(conversationId)?.let { agentProcess ->
            AgentProcessChatSession(agentProcess)
        }
    }

}

/**
 * Many instances for one AgentProcess
 */
class AgentProcessChatSession(
    private val agentProcess: AgentProcess,
) : ChatSession {

    override val processId: String = agentProcess.id

    override val outputChannel: OutputChannel
        get() = agentProcess.processContext.outputChannel

    override val conversation = run {
        agentProcess[KEY] as? Conversation
            ?: run {
                val conversation = InMemoryConversation(id = agentProcess.id)
                agentProcess[KEY] = conversation
                conversation.also {
                    agentProcess.processContext.outputChannel.send(
                        DiagnosticOutputChannelEvent(
                            processId = agentProcess.id,
                            message = "Chat session started with conversationId=${conversation.id}",
                        )
                    )
                }
            }
    }

    override val user: User?
        get() = agentProcess.processContext.processOptions.identities.forUser

    override fun respond(
        userMessage: UserMessage,
    ) {
        agentProcess.addObject(userMessage)
        TODO()
    }

    companion object {
        const val KEY = "conversation"
    }
}
