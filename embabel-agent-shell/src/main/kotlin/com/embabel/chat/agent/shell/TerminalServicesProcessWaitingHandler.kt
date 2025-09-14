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
package com.embabel.chat.agent.shell

import com.embabel.agent.api.common.autonomy.AgentProcessExecution
import com.embabel.agent.api.common.autonomy.ProcessWaitingException
import com.embabel.agent.shell.TerminalServices
import com.embabel.chat.AgenticResultAssistantMessage
import com.embabel.chat.AssistantMessage
import com.embabel.chat.agent.ProcessWaitingHandler

/**
 * Shell-specific implementation of agent platform chat session.
 */
class TerminalServicesProcessWaitingHandler(
    private val terminalServices: TerminalServices,
) : ProcessWaitingHandler {

    override fun handleProcessWaitingException(
        pwe: ProcessWaitingException,
        basis: Any,
    ): AssistantMessage {
        val awaitableResponse =
            terminalServices.handleProcessWaitingException(pwe) ?: return AssistantMessage("Operation cancelled.")
        pwe.awaitable.onResponse(
            response = awaitableResponse,
            agentProcess = pwe.agentProcess,
        )
        try {
            val agentProcess = pwe.agentProcess
            agentProcess.run()
            val ape = AgentProcessExecution.fromProcessStatus(
                basis = basis,
                agentProcess = agentProcess
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
