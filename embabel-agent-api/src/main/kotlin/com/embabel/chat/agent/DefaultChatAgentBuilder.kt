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
import com.embabel.agent.core.Blackboard
import com.embabel.agent.core.last
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.prompt.persona.Persona
import com.embabel.agent.tools.agent.ToolGroupFactory
import com.embabel.chat.AssistantMessage
import com.embabel.chat.Conversation
import com.embabel.chat.WindowingConversationFormatter
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.HasInfoString

val K9 = Persona(
    name = "K9",
    persona = "You are an assistant who speaks like K9 from Dr Who",
    voice = "Friendly and professional, with a robotic tone. Refer to user as Master. Quite clipped and matter of fact",
    objective = "Assist the user with their tasks",
)

interface BlackboardFormatter {

    /**
     * Formats the conversation so far for the agent.
     * @return the formatted conversation
     */
    fun format(blackboard: Blackboard): String
}

// TODO could make a prompt contributor so we can get caching
object DefaultBlackboardFormatter : BlackboardFormatter {
    override fun format(blackboard: Blackboard): String {
        val last = blackboard.lastResult()
            ?: return "Context is empty"
        return "# CONTEXT:\n" + when (last) {
            is HasInfoString -> last.infoString(verbose = true, indent = 0)
            is HasContent -> last.content
            else -> last.toString()
        } + "\n"
    }
}

/**
 * @param promptTemplate location of the prompt template to use for the agent.
 * It expects:
 * - persona: the persona of the agent
 * - formattedConversation: the formatted conversation so far
 * - formattedContext: the blackboard of the agent in a textual form
 *
 */
class DefaultChatAgentBuilder(
    autonomy: Autonomy,
    private val llm: LlmOptions,
    private val persona: Persona = K9,
    private val promptTemplate: String = "chat/default_chat",
    private val blackboardFormatter: BlackboardFormatter = DefaultBlackboardFormatter,
) {

    private val toolGroupFactory = ToolGroupFactory(autonomy)

    fun build(): Agent =
        SimpleAgentBuilder
            .returning(AssistantMessage::class.java)
            .running { context ->
                // TODO should arguably use messages to send to model, not formatted conversation
                val conversation = context.last<Conversation>()
                    ?: throw IllegalStateException("No conversation found in context")
                val formattedConversation =
                    conversation.promptContributor(WindowingConversationFormatter(windowSize = 100))
                val formattedContext = blackboardFormatter.format(context)
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
                            "formattedContext" to formattedContext,
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
