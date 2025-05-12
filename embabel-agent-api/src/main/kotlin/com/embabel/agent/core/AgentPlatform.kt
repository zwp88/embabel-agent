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

import com.embabel.agent.spi.PlatformServices
import com.embabel.agent.spi.ToolGroupResolver

/**
 * An AgentPlatform can run agents. It can also act as an agent itself,
 * drawing on all of its agents as its own actions, goals, and conditions.
 * An AgentPlatform is stateful, as agents can be deployed to it.
 * See TypedOps for a higher level API with typed I/O.
 */
interface AgentPlatform : AgentScope {

    val platformServices: PlatformServices

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
            )
        )
        return this
    }

    /**
     * Run the agent from the given ProcessOptions.
     * We might create a new blackboard or have one
     * @param agent the agent to run. Does not need to be deployed to the platform
     * @param processOptions the options for the process
     * @param bindings the bindings for the process: Objects that are pre-bound
     * to the blackboard.
     */
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
        get() = agents().flatMap { it.schemaTypes }.distinctBy { it.name }

    override val domainTypes: Collection<Class<*>>
        get() = agents().flatMap { it.domainTypes }.distinct()

    override val actions: List<Action>
        get() = agents().flatMap { it.actions }.distinctBy { it.name }

    override val goals: Set<Goal>
        get() = agents().flatMap { it.goals }.distinctBy { it.name }.toSet()

    override val conditions: Set<Condition>
        get() = agents().flatMap { it.conditions }.distinctBy { it.name }.toSet()

}
