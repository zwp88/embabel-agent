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
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.shell.ShellConfig
import com.embabel.agent.shell.TerminalServices
import com.embabel.chat.*

/**
 * Uses last message as intent.
 * Wholly delegates handling to agent platform.
 * Can bind conversation to the blackboard if so configured.
 */
class LastMessageIntentAgentPlatformChatSession(
    private val autonomy: Autonomy,
    private val goalChoiceApprover: GoalChoiceApprover,
    override val messageListener: MessageListener,
    val processOptions: ProcessOptions = ProcessOptions(),
    private val terminalServices: TerminalServices,
    private val config: ShellConfig.ChatConfig,
) : ChatSession {

    private var _conversation: Conversation = InMemoryConversation()

    override val conversation: Conversation
        get() = _conversation

    override fun respond(message: UserMessage, additionalListener: MessageListener?) {
        _conversation = conversation.withMessage(message)
        val asssistantMessage = generateResponse(message)
        _conversation = conversation.withMessage(asssistantMessage)
        messageListener.onMessage(asssistantMessage)
        additionalListener?.onMessage(asssistantMessage)
    }

    private fun generateResponse(message: UserMessage): AssistantMessage {
        try {
            val dynamicExecutionResult = autonomy.chooseAndAccomplishGoal(
                intent = message.content,
                processOptions = processOptions,
                goalChoiceApprover = goalChoiceApprover,
                agentScope = autonomy.agentPlatform,
                additionalBindings = if (config.bindConversation)
                    mapOf("conversation" to conversation)
                else emptyMap()
            )
            val result = dynamicExecutionResult.output
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

    private fun handleProcessWaitingException(pwe: ProcessWaitingException, basis: Any): AssistantMessage {
        val awaitableResponse = terminalServices.handleProcessWaitingException(pwe)
        if (awaitableResponse == null) {
            return AssistantMessage("Operation cancelled.")
        }
        pwe.awaitable.onResponse(
            response = awaitableResponse,
            processContext = pwe.agentProcess!!.processContext
        )
        try {
            pwe.agentProcess.run()
            val ape = AgentProcessExecution.fromProcessStatus(
                basis = basis,
                agentProcess = pwe.agentProcess
            )
            return AgenticResultAssistantMessage(
                agentProcessExecution = ape,
                content = ape.output.toString(),
            )
        } catch (e: ProcessWaitingException) {
            return handleProcessWaitingException(e, basis)
        }
    }
}
