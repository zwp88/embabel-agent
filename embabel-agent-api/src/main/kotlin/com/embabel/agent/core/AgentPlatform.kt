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

import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.spi.Ranker
import com.embabel.agent.spi.ToolGroupResolver
import com.embabel.common.core.types.ZeroToOne

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

/**
 * How to run an AgentProcess
 * @param contextId context id to use for this process. Can be null.
 * If set it can enable connection to external resources and persistence
 * from previous runs.
 * @param test whether to run in test mode. In test mode, the agent platform
 * will not use any external resources such as LLMs, and will not persist any state.
 * @param verbosity detailed verbosity settings for logging etc.
 * @param maxActions max number of actions after which to stop even if a goal has not been achieved.
 * Prevents infinite loops.
 */
data class ProcessOptions(
    val contextId: ContextId? = null,
    val test: Boolean = false,
    val verbosity: Verbosity = Verbosity(),
    val allowGoalChange: Boolean = true,
    val maxActions: Int = 40,
)

/**
 * Properties common to all AgentPlatform implementations
 */
interface AgentPlatformProperties {

    /**
     * Goal confidence cut-off, between 0 and 1, which is required
     * to have confidence in executing the most promising goal.
     */
    val goalConfidenceCutOff: ZeroToOne

    val agentConfidenceCutOff: ZeroToOne
}

class NoSuchAgentException(
    agentName: String,
) : IllegalArgumentException("No such agent: $agentName")

/**
 * An AgentPlatform can run agents. It can also act as an agent itself,
 * drawing on all of its agents as its own actions, goals, and conditions.
 * An AgentPlatform is stateful, as agents can be deployed to it.
 * See TypedOps for a higher level API with typed I/O.
 */
interface AgentPlatform : AgentScope {

    val properties: AgentPlatformProperties

    val ranker: Ranker

    val eventListener: AgenticEventListener

    val toolGroupResolver: ToolGroupResolver

    /**
     * Find an agent process by id. Implementations are only obliged to
     * resolve running processes, although they may choose to return older processes.
     */
    fun getAgentProcess(id: String): AgentProcess?

    fun agents(): Set<Agent>

    fun deploy(agent: Agent): AgentPlatform

    fun deploy(agentScope: AgentScope): AgentPlatform {
        if (agentScope is Agent) {
            return deploy(agentScope)
        }
        deploy(
            Agent(
                name = agentScope.name,
                description = agentScope.name,
                actions = agentScope.actions,
                goals = agentScope.goals,
                conditions = agentScope.conditions,
                toolGroups = emptyList(),
            )
        )
        return this
    }

    fun deploy(action: Action): AgentPlatform

    fun deploy(goal: Goal): AgentPlatform

    fun runAgentFrom(
        agent: Agent,
        processOptions: ProcessOptions = ProcessOptions(),
        bindings: Map<String, Any>,
    ): AgentProcess

    fun createChildProcess(
        agent: Agent,
        parentAgentProcess: AgentProcess,
    ): AgentProcess

    override val schemaTypes: Collection<SchemaType>
        get() = agents().flatMap { it.schemaTypes }.distinct()

    override val domainTypes: Collection<Class<*>>
        get() = agents().flatMap { it.domainTypes }.distinct()

    override val actions: List<Action>
        get() = agents().flatMap { it.actions }.distinct()

    override val goals: Set<Goal>
        get() = agents().flatMap { it.goals }.toSet()

    override val conditions: Set<Condition>
        get() = agents().flatMap { it.conditions }.distinctBy { it.name }.toSet()

}
