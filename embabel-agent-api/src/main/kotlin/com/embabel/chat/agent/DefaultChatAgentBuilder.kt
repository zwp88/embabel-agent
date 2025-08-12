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

import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.api.common.workflow.control.SimpleAgentBuilder
import com.embabel.agent.core.Agent
import com.embabel.agent.core.last
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona
import com.embabel.agent.tools.agent.ToolGroupFactory
import com.embabel.chat.AssistantMessage
import com.embabel.chat.Conversation
import com.embabel.chat.WindowingConversationFormatter
import com.embabel.common.ai.model.LlmOptions

class DefaultChatAgentBuilder(
    autonomy: Autonomy,
    private val llm: LlmOptions,
    private val persona: Persona = K9,
    private val promptTemplate: String = "chat/default_chat",
) {

    private val toolGroupFactory = ToolGroupFactory(autonomy)

    fun build(): Agent =
        SimpleAgentBuilder
            .returning(AssistantMessage::class.java)
            .running { context ->
                // TODO should arguably use messages
                val conversation = context.last<Conversation>()
                    ?: throw IllegalStateException("No conversation found in context")
                val formattedConversation =
                    conversation.promptContributor(WindowingConversationFormatter(windowSize = 100))
                val assistantMessageContext = context.ai()
                    .withLlm(llm)
                    .withPromptElements(persona)
                    .withToolGroup(
                        toolGroupFactory.achievableGoalsToolGroup(
                            context = context,
                            bindings = mapOf("it" to UserInput("doesn't matter"))
                        ),
                    )
                    .withTemplate(promptTemplate)
                    .generateText(
                        mapOf(
                            "persona" to persona,
                            "formattedConversation" to formattedConversation,
                        )
                    )
                AssistantMessage(
                    name = persona.name,
                    content = assistantMessageContext,
                )
            }
            .buildAgent(
                name = "Default chat agent",
                description = "Default conversation agent with persona ${persona.name}",
            )
}
