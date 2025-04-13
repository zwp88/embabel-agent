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
package com.embabel.agent

import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.event.DynamicAgentCreationEvent
import com.embabel.agent.event.GoalChoiceRequestEvent
import com.embabel.agent.spi.GoalRanker
import com.embabel.agent.spi.GoalRankings
import com.embabel.agent.spi.ToolGroupResolver
import com.embabel.agent.testing.FakeGoalRanker
import com.embabel.agent.testing.RandomGoalRanker
import com.embabel.common.util.kotlin.loggerFor

/**
 * Controls log output.
 */
data class Verbosity(
    val showPrompts: Boolean = false,
    val showLlmResponses: Boolean = false,
    val debug: Boolean = false,
)

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
    val maxActions: Int = 20,
)

/**
 * Result of directly trying to execute a goal
 * Forcing client to discriminate
 */
sealed interface GoalResult {
    val basis: Any

    data class Success(
        override val basis: Any,
        val output: Any,
        val processStatus: AgentProcessStatus,
    ) : GoalResult

    data class NoGoalFound(
        override val basis: Any,
        val goalRankings: GoalRankings,
    ) : GoalResult
}

/**
 * Properties common to all AgentPlatform implementations
 */
interface AgentPlatformProperties {

    /**
     * Goal confidence cut-off, between 0 and 1, which is required
     * to have confidence in executing the most promising goal.
     */
    val goalConfidenceCutOff: ZeroToOne
}

/**
 * An AgentPlatform can run agents. It can also act as an agent itself,
 * drawing on all of its agents as its own actions, goals, and conditions.
 * An AgentPlatform is stateful, as agents can be deployed to it.
 * See TypedOps for a higher level API with typed I/O.
 */
interface AgentPlatform : AgentMetadata, AgentFactory {

    val properties: AgentPlatformProperties

    val goalRanker: GoalRanker

    val eventListener: AgenticEventListener

    val toolGroupResolver: ToolGroupResolver

    fun agentByName(agentName: String): Agent = agents().firstOrNull { it.name == agentName }
        ?: throw IllegalArgumentException("Unknown agent: $agentName")

    fun agents(): List<Agent>

    fun deploy(agent: Agent): AgentPlatform

    fun deploy(agentMetadata: AgentMetadata): AgentPlatform {
        if (agentMetadata is Agent) {
            return deploy(agentMetadata)
        }
        deploy(
            Agent(
                name = agentMetadata.name,
                description = agentMetadata.name,
                actions = agentMetadata.actions,
                goals = agentMetadata.goals,
                conditions = agentMetadata.conditions,
                toolGroups = emptyList(),
            )
        )
        return this
    }

    fun deploy(vararg agents: Agent): AgentPlatform =
        agents.fold(this) { acc, agent ->
            acc.deploy(agent)
        }

    fun deploy(action: Action): AgentPlatform

    fun deploy(goal: Goal): AgentPlatform

    fun runAgentFrom(
        agent: Agent,
        processOptions: ProcessOptions = ProcessOptions(),
        bindings: Map<String, Any>,
    ): AgentProcessStatus

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

    override val conditions: List<Condition>
        get() = agents().flatMap { it.conditions }.distinct()

    override fun createAgent(name: String, description: String): Agent {
        // TODO this isn't great, as we may not want all of them
        val toolCallbacks =
            (actions.flatMap { it.toolCallbacks } + agents().flatMap { it.toolCallbacks }).distinct()
        val toolGroups =
            (actions.flatMap { it.toolGroups } + agents().flatMap { it.toolGroups }).distinct()
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

    /**
     * Choose a goal based on the user input.
     * Doesn't need reified types because we don't know the type yet.
     */
    fun chooseAndAccomplishGoal(
        intent: String,
        processOptions: ProcessOptions = ProcessOptions(),
    ): GoalResult {
        val userInput = UserInput(intent)

        // Use a fake goal ranker if we are in test mode and don't already have a fake one
        // Enables running under integration tests and in test mode otherwise with production config
        val goalRanker = if (processOptions.test && goalRanker !is FakeGoalRanker) {
            RandomGoalRanker()
        } else {
            goalRanker
        }

        val goalChoiceEvent = GoalChoiceRequestEvent(
            agentPlatform = this,
            basis = userInput,
        )
        eventListener.onPlatformEvent(goalChoiceEvent)
        val goalRankings = goalRanker
            .rankGoals(userInput = userInput, agentMetadata = this)
        val credibleGoals = goalRankings
            .rankings
            .filter { it.confidence > properties.goalConfidenceCutOff }
        val goalChoice = credibleGoals.firstOrNull()
        if (goalChoice == null) {
            eventListener.onPlatformEvent(
                goalChoiceEvent.noDeterminationEvent(
                    goalRankings = goalRankings,
                    goalConfidenceCutOff = properties.goalConfidenceCutOff
                )
            )
            return GoalResult.NoGoalFound(goalRankings = goalRankings, basis = userInput)
        }

        loggerFor<AgentPlatform>().debug(
            "Goal choice {} with confidence {} for user intent {}: Choices were {}",
            goalChoice.goal.name,
            goalChoice.confidence,
            intent,
            goals.joinToString("\n") { it.name },
        )
        eventListener.onPlatformEvent(
            goalChoiceEvent.determinationEvent(
                goalChoice = goalChoice,
                goalRankings = goalRankings,
            )
        )

        val goalAgent = createAgent(
            name = "goal-${goalChoice.goal.name}",
            description = goalChoice.goal.description,
        )
            .withSingleGoal(goalChoice.goal)
        eventListener.onPlatformEvent(
            DynamicAgentCreationEvent(
                agent = goalAgent,
                agentPlatform = this,
                basis = userInput,
            )
        )
        val processStatus = runAgentFrom(
            processOptions = processOptions,
            agent = goalAgent,
            bindings = mapOf(
                "it" to userInput
            )
        )
        return GoalResult.Success(
            basis = userInput,
            output = processStatus.finalResult()!!,
            processStatus = processStatus,
        )
    }
}
