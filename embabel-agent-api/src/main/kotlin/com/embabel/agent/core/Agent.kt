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

import com.embabel.agent.spi.StuckHandler
import com.embabel.common.core.types.Described
import com.embabel.plan.goap.GoapSystem
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback

const val DEFAULT_VERSION = "0.1.0-SNAPSHOT"

/**
 * An agent defines a set of actions and conditions
 * that enable planning.
 * @param name The name of the agent
 * @param version The version of the agent
 * @param description A description of the agent
 * @param goals The goals the agent can achieve
 * @param stuckHandler The handler to call when the agent is stuck, provided
 * @param toolCallbacks The callbacks for tools the agent can use. Enables them to be
 * associated with agent definition.
 * @param toolGroups The tool groups the agent can use
 * @param conditions Well known conditions that can be referenced by actions
 * @param actions The actions the agent can use
 * @param schemaTypes Data types used in this agent
 */
data class Agent(
    override val name: String,
    val version: String = DEFAULT_VERSION,
    override val description: String,
    override val toolCallbacks: Collection<ToolCallback> = emptyList(),
    override val toolGroups: Collection<String> = emptyList(),
    override val conditions: Set<Condition> = emptySet(),
    override val actions: List<Action>,
    override val goals: Set<Goal>,
    val stuckHandler: StuckHandler? = null,
    override val schemaTypes: Collection<SchemaType> = inferDataTypes(
        agentName = name,
        defaultDataTypes = emptyList(),
        actions = actions,
    ),
) : Described, AgentScope, ToolConsumer {

    /**
     * Return a version of the agent with the single goal
     */
    fun withSingleGoal(goal: Goal): Agent =
        copy(goals = setOf(goal))

    fun findDataType(name: String): SchemaType {
        return schemaTypes.find { it.name == name }
            ?: error("Data type $name not found in agent $name")
    }

    @JsonIgnore
    val goapSystem: GoapSystem = run {
        val actions = actions.toSet()
        logger.debug(infoString())
        GoapSystem(actions, goals)
    }

    override fun infoString(verbose: Boolean?): String {
        return "Agent " + super.infoString(verbose)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Agent::class.java)

        fun inferDataTypes(
            agentName: String,
            defaultDataTypes: List<SchemaType>,
            actions: List<Action>
        ): List<SchemaType> {
            // Merge properties from multiple type references
            for (action in actions) {
                logger.debug(
                    "Action {} has types {}, inputBinding={}",
                    action.name,
                    action.schemaTypes,
                    action.inputs,
                )
            }
            val types = (defaultDataTypes + actions
                .flatMap { it.schemaTypes }
                .groupBy { it.name }
                .mapValues { (_, types) ->
                    types.reduce { acc, type ->
                        acc.copy(properties = acc.properties + type.properties)
                    }
                }
                .values)
                .map { it.copy(properties = it.properties.distinctBy { it.name }) }
                .distinctBy { it.name }
                .sortedBy { it.name }

            logger.debug(
                "Agent {} inferred types:\n\t{}",
                agentName,
                types.joinToString("\n\t") { "${it.name} - ${it.properties.map { p -> p.name }}" })
            return types
        }
    }

}
