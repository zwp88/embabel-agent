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
package com.embabel.agent.api.common

import com.embabel.agent.core.*
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.DynamicAgentCreationEvent
import com.embabel.agent.event.RankingChoiceRequestEvent
import com.embabel.agent.spi.Rankings
import com.embabel.agent.testing.FakeRanker
import com.embabel.agent.testing.RandomRanker
import com.embabel.common.util.kotlin.loggerFor

/**
 * Result of directly trying to execute a goal
 * Forcing client to discriminate
 */
sealed interface DynamicExecutionResult {
    val basis: Any

    data class Success(
        override val basis: Any,
        val output: Any,
        val agentProcessStatus: AgentProcessStatus,
    ) : DynamicExecutionResult

    data class NoGoalFound(
        override val basis: Any,
        val goalRankings: Rankings<Goal>,
    ) : DynamicExecutionResult

    data class NoAgentFound(
        override val basis: Any,
        val agentRankings: Rankings<Agent>,
    ) : DynamicExecutionResult
}

/**
 * Choose a goal based on the user input and try to achieve it.
 * Open execution model:
 * May bring in actions and conditions from multiple agents to help achieve the goal.
 * Doesn't need reified types because we don't know the type yet.
 */
fun AgentPlatform.chooseAndAccomplishGoal(
    intent: String,
    processOptions: ProcessOptions = ProcessOptions(),
): DynamicExecutionResult {
    val userInput = UserInput(intent)

    // Use a fake goal ranker if we are in test mode and don't already have a fake one
    // Enables running under integration tests and in test mode otherwise with production config
    val rankerToUse = if (processOptions.test && ranker !is FakeRanker) {
        RandomRanker()
    } else {
        ranker
    }

    val goalChoiceEvent = RankingChoiceRequestEvent<Goal>(
        agentPlatform = this,
        type = Goal::class.java,
        basis = userInput,
        choices = this.goals,
    )
    eventListener.onPlatformEvent(goalChoiceEvent)
    val goalRankings = rankerToUse
        .rankGoals(userInput = userInput, goals = this.goals)
    val credibleGoals = goalRankings
        .rankings
        .filter { it.confidence > properties.goalConfidenceCutOff }
    val goalChoice = credibleGoals.firstOrNull()
    if (goalChoice == null) {
        eventListener.onPlatformEvent(
            goalChoiceEvent.noDeterminationEvent(
                rankings = goalRankings,
                confidenceCutoff = properties.goalConfidenceCutOff
            )
        )
        return DynamicExecutionResult.NoGoalFound(goalRankings = goalRankings, basis = userInput)
    }

    loggerFor<AgentPlatform>().debug(
        "Goal choice {} with confidence {} for user intent {}: Choices were {}",
        goalChoice.ranked.name,
        goalChoice.confidence,
        intent,
        goals.joinToString("\n") { it.name },
    )
    eventListener.onPlatformEvent(
        goalChoiceEvent.determinationEvent(
            choice = goalChoice,
            rankings = goalRankings,
        )
    )

    val goalAgent = createAgent(
        name = "goal-${goalChoice.ranked.name}",
        description = goalChoice.ranked.description,
    )
        .withSingleGoal(goalChoice.ranked)
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
    return DynamicExecutionResult.Success(
        basis = userInput,
        output = processStatus.finalResult()!!,
        agentProcessStatus = processStatus,
    )
}

/**
 * Choose an agent based on the user input and run it.
 * Closed execution model:
 * Will never mix actions and goals from different agents.
 * Doesn't need reified types because we don't know the type yet.
 */
fun AgentPlatform.chooseAndRunAgent(
    intent: String,
    processOptions: ProcessOptions = ProcessOptions(),
): DynamicExecutionResult {
    val userInput = UserInput(intent)

    // Use a fake ranker if we are in test mode and don't already have a fake one
    // Enables running under integration tests and in test mode otherwise with production config
    val rankerToUse = if (processOptions.test && ranker !is FakeRanker) {
        RandomRanker()
    } else {
        ranker
    }

    val agentChoiceEvent = RankingChoiceRequestEvent<Agent>(
        agentPlatform = this,
        type = Agent::class.java,
        basis = userInput,
        choices = this.agents(),
    )
    eventListener.onPlatformEvent(agentChoiceEvent)
    val agentRankings = rankerToUse
        .rankAgents(userInput = userInput, agents = this.agents())
    val credibleAgents = agentRankings
        .rankings
        .filter { it.confidence > properties.agentConfidenceCutOff }
    val agentChoice = credibleAgents.firstOrNull()
    if (agentChoice == null) {
        eventListener.onPlatformEvent(
            agentChoiceEvent.noDeterminationEvent(
                rankings = agentRankings,
                confidenceCutoff = properties.agentConfidenceCutOff
            )
        )
        return DynamicExecutionResult.NoAgentFound(agentRankings = agentRankings, basis = userInput)
    }

    loggerFor<AgentPlatform>().debug(
        "Agent choice {} with confidence {} for user intent {}: Choices were {}",
        agentChoice.ranked.name,
        agentChoice.confidence,
        intent,
        agents().joinToString("\n") { it.name },
    )
    eventListener.onPlatformEvent(
        agentChoiceEvent.determinationEvent(
            choice = agentChoice,
            rankings = agentRankings,
        )
    )

    val agent = agentChoice.ranked
    eventListener.onPlatformEvent(
        DynamicAgentCreationEvent(
            agent = agentChoice.ranked,
            agentPlatform = this,
            basis = userInput,
        )
    )
    val processStatus = runAgentFrom(
        processOptions = processOptions,
        agent = agent,
        bindings = mapOf(
            "it" to userInput
        )
    )
    return DynamicExecutionResult.Success(
        basis = userInput,
        output = processStatus.finalResult()!!,
        agentProcessStatus = processStatus,
    )
}
