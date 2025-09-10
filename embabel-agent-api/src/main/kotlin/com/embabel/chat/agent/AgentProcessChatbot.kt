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

fun interface AgentSource {

    fun resolveAgent(user: User?): Agent
}

/**
 * Chatbot implementation backed by an AgentProcess
 * The AgentProcess must react to messages and respond on its output channel
 * @param agentPlatform the agent platform to create and manage agent processes
 * @param agentSource factory for agents. The factory is called for each new session.
 * This allows lazy loading and more flexible usage patterns
 */
class AgentProcessChatbot(
    private val agentPlatform: AgentPlatform,
    private val agentSource: AgentSource,
) : Chatbot {

    override fun createSession(
        user: User?,
        outputChannel: OutputChannel,
        systemMessage: String?,
    ): ChatSession {
        val agentProcess = agentPlatform.createAgentProcess(
            agent = agentSource.resolveAgent(user),
            processOptions = ProcessOptions(
                outputChannel = outputChannel,
            ),
            bindings = emptyMap(),
        )
        // We don't yet start the process, it will be started when the first message is received
        return AgentProcessChatSession(agentProcess)
    }

    override fun findSession(conversationId: String): ChatSession? {
        return agentPlatform.getAgentProcess(conversationId)?.let { agentProcess ->
            AgentProcessChatSession(agentProcess)
        }
    }

    companion object {

        /**
         * Create a chatbot with the given agent. The agent is looked up by name from the agent platform.
         * @param agentPlatform the agent platform to create and manage agent processes
         * @param agentName the name of the agent to
         */
        @JvmStatic
        fun withAgentByName(
            agentPlatform: AgentPlatform,
            agentName: String,
        ): Chatbot = AgentProcessChatbot(agentPlatform, {
            agentPlatform.agents().find { it.name == agentName }
                ?: throw IllegalArgumentException("No agent found with name $agentName")
        })
    }

}

/**
 * Many instances for one AgentProcess
 */
private class AgentProcessChatSession(
    private val agentProcess: AgentProcess,
) : ChatSession {

    override val processId: String = agentProcess.id

    override fun isFinished(): Boolean = agentProcess.finished

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

    override fun onUserMessage(
        userMessage: UserMessage,
    ) {
        agentProcess.addObject(userMessage)
        agentProcess.run()
    }

    companion object {
        const val KEY = "conversation"
    }
}
