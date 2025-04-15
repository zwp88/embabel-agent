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

interface Conditions {

    val conditions: List<Condition>
}

interface GoalsContributor {

    val goals: Set<Goal>
}

/**
 * Metadata of an agent, along with ability to instantiate an agent.
 */
interface AgentMetadata : Named, GoalsContributor, Conditions, DataDictionary, HasInfoString {

    val actions: List<Action>

    @get:JsonIgnore
    override val domainTypes: Collection<Class<*>>
        get() = actions.flatMap { it.domainTypes }.distinct()

    override fun infoString(verbose: Boolean?): String =
        "%s:\n\tgoals:\n\t\t%s\n\tactions:\n\t\t%s\n\tconditions: %s\n\tdata types: %s".format(
            name,
            goals.joinToString("\n\t\t") { "${it.name} - pre=${it.preconditions} value=${it.value}" },
            actions.joinToString("\n\t\t") { "${it.name} - pre=${it.preconditions} post=${it.effects}" },
            conditions.map { it.name },
            domainTypes.map { it.simpleName }.distinct().sorted(),
        )

    companion object {

        operator fun invoke(
            name: String,
            actions: List<Action> = emptyList(),
            goals: Set<Goal> = emptySet(),
            conditions: List<Condition> = emptyList(),
        ): AgentMetadata {
            return AgentMetadataImpl(
                name = name,
                actions = actions,
                goals = goals,
                conditions = conditions,
            )
        }

    }
}

private data class AgentMetadataImpl(
    override val name: String,
    override val actions: List<Action>,
    override val goals: Set<Goal>,
    override val conditions: List<Condition>,
    override val schemaTypes: Collection<SchemaType> = emptyList(),
) : AgentMetadata

interface AgentFactory {
    fun createAgent(name: String, description: String): Agent
}
