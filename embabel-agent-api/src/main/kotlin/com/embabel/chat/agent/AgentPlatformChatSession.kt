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

import com.embabel.agent.api.common.autonomy.*
import com.embabel.agent.core.Blackboard
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.support.InMemoryBlackboard
import com.embabel.agent.domain.io.UserInput
import com.embabel.chat.*


/**
 * Configuration for the chat session
 * @param confirmGoals Whether to confirm goals with the user before proceeding
 * @param bindConversation Whether to bind the conversation to the chat session
 */
data class ChatConfig(
    val confirmGoals: Boolean = true,
    val bindConversation: Boolean = false,
    val multiGoal: Boolean = false,
)

/**
 * Base class for agent platform chat sessions.
 * Uses last message as intent and delegates handling to agent platform.
 */
abstract class AgentPlatformChatSession(
    private val autonomy: Autonomy,
    private val goalChoiceApprover: GoalChoiceApprover,
    override val messageListener: MessageListener,
    val processOptions: ProcessOptions = ProcessOptions(),
    val chatConfig: ChatConfig = ChatConfig(),
) : ChatSession {

    private var internalConversation: Conversation = InMemoryConversation()

    private val blackboard: Blackboard = processOptions.blackboard ?: InMemoryBlackboard()

    override val conversation: Conversation
        get() = internalConversation

    override fun respond(
        message: UserMessage,
        additionalListener: MessageListener?,
    ) {
        internalConversation = conversation.withMessage(message)
        val assistantMessage = generateResponse(message)
        internalConversation = conversation.withMessage(assistantMessage)
        messageListener.onMessage(assistantMessage)
        additionalListener?.onMessage(assistantMessage)
    }

    protected fun generateResponse(message: UserMessage): AssistantMessage {

        val handledCommand = handleAsCommand(message)
        if (handledCommand != null) {
            return handledCommand
        }

        val bindings = buildMap {
            put("userInput", UserInput(message.content))
            if (shouldBindConversation())
                put("conversation", conversation)
        }
        try {
            val dynamicExecutionResult = autonomy.chooseAndAccomplishGoal(
                // We continue to use the same blackboard.
                processOptions = processOptions.copy(
                    blackboard = blackboard,
                ),
                goalChoiceApprover = goalChoiceApprover,
                agentScope = autonomy.agentPlatform,
                bindings = bindings,
                goalSelectionOptions = GoalSelectionOptions(
                    multiGoal = chatConfig.multiGoal,
                ),
            )
            val result = dynamicExecutionResult.output
            // Bind the result to the blackboard.
            blackboard += result
            return AgenticResultAssistantMessage(
                agentProcessExecution = dynamicExecutionResult,
                content = result.toString(),
            )
        } catch (pwe: ProcessWaitingException) {
            return handleProcessWaitingException(pwe, message.content)
        } catch (_: NoGoalFound) {
            return AssistantMessage(
                content = """|
                    |I'm sorry Dave. I'm afraid I can't do that.
                    |
                    |Things I CAN do:
                    |${autonomy.agentPlatform.goals.joinToString("\n") { "- ${it.description}" }}
                """.trimMargin(),
            )
        } catch (_: GoalNotApproved) {
            return AssistantMessage(
                content = "I obey. That action will not be executed.",
            )
        }
    }

    /**
     * Determines whether conversation should be bound to the blackboard
     */
    protected abstract fun shouldBindConversation(): Boolean

    /**
     * Handles process waiting exceptions in a platform-specific way
     */
    protected abstract fun handleProcessWaitingException(
        pwe: ProcessWaitingException,
        basis: Any,
    ): AssistantMessage

    private fun handleAsCommand(message: UserMessage): AssistantMessage? {
        return parseSlashCommand(message.content)?.let { (command, args) ->
            when (command) {
                "help" -> AssistantMessage(
                    content = """
                        |Available commands:
                        |/help - Show this help message
                        |/bb, blackboard - Show the blackboard
                    """.trimMargin(),
                )

                "bb", "blackboard" -> {
                    AssistantMessage(blackboard.infoString(verbose = true, indent = 2))
                }

                else -> AssistantMessage("Unrecognized / command $command")
            }
        }
    }
}

val SLASH_COMMAND_REGEX = Regex("^/([a-zA-Z0-9_-]+)(?:\\s+(.*))?$")

fun parseSlashCommand(input: String): Pair<String, List<String>>? {
    val match = SLASH_COMMAND_REGEX.matchEntire(input)
    return if (match != null) {
        val command = match.groupValues[1]
        val argsString = match.groupValues[2]
        val args = if (argsString.isNotEmpty()) {
            argsString.trim().split("\\s+".toRegex())
        } else {
            emptyList()
        }
        Pair(command, args)
    } else {
        null
    }
}
