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
package com.embabel.agent.core

import com.embabel.agent.event.AbstractAgentProcessEvent

/**
 * Controls log output.
 */
data class Verbosity(
    val showPrompts: Boolean = false,
    val showLlmResponses: Boolean = false,
    val debug: Boolean = false,
    val showPlanning: Boolean = false,
) {
    val showLongPlans: Boolean get() = showPlanning || debug || showLlmResponses || showPrompts
}

enum class Delay {
    NONE, MEDIUM, LONG
}

class EarlyTermination(
    agentProcess: AgentProcess,
    val reason: String,
    val policy: EarlyTerminationPolicy,
) : AbstractAgentProcessEvent(agentProcess)

interface EarlyTerminationPolicy {
    fun shouldTerminate(agentProcess: AgentProcess): EarlyTermination?

    companion object {
        fun maxActions(maxActions: Int = 40): EarlyTerminationPolicy = MaxActionsEarlyTerminationPolicy(maxActions)
    }
}

private data class MaxActionsEarlyTerminationPolicy(
    private val maxActions: Int,
) : EarlyTerminationPolicy {
    override fun shouldTerminate(agentProcess: AgentProcess): EarlyTermination? =
        if (agentProcess.history.size >= maxActions) {
            EarlyTermination(agentProcess, "Max actions reached", this)
        } else null

}

/**
 *  @param maxActions maximum number of actions to run.
 *  Prevents infinite loops
 */
data class ProcessControl(
    val toolDelay: Delay = Delay.NONE,
    val operationDelay: Delay = Delay.NONE,
    val earlyTerminationPolicy: EarlyTerminationPolicy,
)

/**
 * How to run an AgentProcess
 * @param contextId context id to use for this process. Can be null.
 * If set it can enable connection to external resources and persistence
 * from previous runs.
 * @param blackboard an existing blackboard to use for this process.
 * By default, it will be modified as the process runs.
 * Whether this is an independent copy is up to the caller, who can call spawn()
 * before passing this argument.
 * @param test whether to run in test mode. In test mode, the agent platform
 * will not use any external resources such as LLMs, and will not persist any state.
 * @param verbosity detailed verbosity settings for logging etc.
 */
data class ProcessOptions(
    val contextId: ContextId? = null,
    val blackboard: Blackboard? = null,
    val test: Boolean = false,
    val verbosity: Verbosity = Verbosity(),
    val allowGoalChange: Boolean = true,
    val control: ProcessControl = ProcessControl(
        toolDelay = Delay.NONE,
        operationDelay = Delay.NONE,
        earlyTerminationPolicy = EarlyTerminationPolicy.maxActions(40),
    ),
)
