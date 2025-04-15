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

import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.Named
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.ai.tool.ToolCallback

interface ConditionSource {

    val conditions: Set<Condition>
}

interface GoalSource {

    val goals: Set<Goal>
}

interface ActionSource {

    val actions: List<Action>
}

/**
 * Defines the scope of an agent: What goals, conditions and actions it knows about.
 */
interface AgentScope : Named, GoalSource, ConditionSource, ActionSource, DataDictionary, HasInfoString {

    @get:JsonIgnore
    override val domainTypes: Collection<Class<*>>
        get() = actions.flatMap { it.domainTypes }.distinct()

    override fun infoString(verbose: Boolean?): String =
        "%s:\n\tgoals:\n\t\t%s\n\tactions:\n\t\t%s\n\tconditions: %s\n\tdata types: %s".format(
            name,
            goals.sortedBy { it.name }
                .joinToString("\n\t\t") { "${it.name} - pre=${it.preconditions} value=${it.value}" },
            actions.sortedBy { it.name }
                .joinToString("\n\t\t") { "${it.name} - pre=${it.preconditions} post=${it.effects}" },
            conditions.map { it.name }.sorted(),
            domainTypes.map { it.simpleName }.distinct().sorted(),
        )


    /**
     * Create a new agent from the given scope
     * @param name Name of the agent to create
     * @param description Description of the agent to create
     * @param extraToolCallbacks Extra tool callbacks to add to the agent
     * @param extraToolGroups Extra tool groups to add to the agent
     */
    fun createAgent(
        name: String,
        description: String,
        extraToolCallbacks: Collection<ToolCallback> = emptyList(),
        extraToolGroups: Collection<String> = emptyList(),
    ): Agent {
        val toolCallbacks =
            (actions.flatMap { it.toolCallbacks } + extraToolCallbacks).distinct()
        val toolGroups =
            (actions.flatMap { it.toolGroups } + extraToolGroups).distinct()
        val newAgent = Agent(
            name = name,
            description = name,
            toolCallbacks = toolCallbacks,
            toolGroups = toolGroups,
            actions = actions,
            goals = goals,
            conditions = conditions,
        )
        return newAgent
    }

    companion object {

        operator fun invoke(
            name: String,
            actions: List<Action> = emptyList(),
            goals: Set<Goal> = emptySet(),
            conditions: Set<Condition> = emptySet(),
        ): AgentScope {
            return AgentScopeImpl(
                name = name,
                actions = actions,
                goals = goals,
                conditions = conditions,
            )
        }

    }
}

private data class AgentScopeImpl(
    override val name: String,
    override val actions: List<Action>,
    override val goals: Set<Goal>,
    override val conditions: Set<Condition>,
    override val schemaTypes: Collection<SchemaType> = emptyList(),
) : AgentScope
