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

class EarlyTermination(
    agentProcess: AgentProcess,
    val reason: String,
    val policy: EarlyTerminationPolicy,
) : AbstractAgentProcessEvent(agentProcess) {

    override fun toString(): String = "Early termination by policy ${policy.name} - $reason"
}

/**
 * Enables early termination of an agent process.
 */
interface EarlyTerminationPolicy {

    val name: String get() = this::class.simpleName ?: "Unknown"

    /**
     * Checks if the agent process should be terminated early.
     */
    fun shouldTerminate(agentProcess: AgentProcess): EarlyTermination?

    companion object {

        const val DEFAULT_ACTION_LIMIT = 40

        @JvmStatic
        fun maxActions(maxActions: Int): EarlyTerminationPolicy =
            MaxActionsEarlyTerminationPolicy(maxActions)

        /**
         * Combines multiple early termination policies into one.
         */
        @JvmStatic
        fun firstOf(vararg earlyTerminationPolicies: EarlyTerminationPolicy): EarlyTerminationPolicy =
            FirstOfEarlyTerminationPolicy(earlyTerminationPolicies.toList())

        /**
         * Fallback budget limit for the agent process.
         * This is a last resort termination policy.
         */
        @JvmStatic
        fun hardBudgetLimit(budget: Double): EarlyTerminationPolicy =
            BudgetEarlyTerminationPolicy(budget)
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

private data class BudgetEarlyTerminationPolicy(
    private val budget: Double,
) : EarlyTerminationPolicy {
    override fun shouldTerminate(agentProcess: AgentProcess): EarlyTermination? =
        if (agentProcess.cost() >= budget) {
            EarlyTermination(
                agentProcess,
                "Exceeded budget of $${"%.4f".format(budget)}: cost=$${"%.4f".format(agentProcess.cost())}",
                this
            )
        } else null

}

private data class FirstOfEarlyTerminationPolicy(
    private val earlyTerminationPolicies: List<EarlyTerminationPolicy>,
) : EarlyTerminationPolicy {
    override fun shouldTerminate(agentProcess: AgentProcess): EarlyTermination? {
        for (earlyTerminationPolicy in earlyTerminationPolicies) {
            val termination = earlyTerminationPolicy.shouldTerminate(agentProcess)
            if (termination != null) {
                return termination
            }
        }
        return null
    }

}
