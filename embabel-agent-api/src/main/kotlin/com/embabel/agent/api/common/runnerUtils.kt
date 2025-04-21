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
import com.embabel.agent.core.hitl.Awaitable
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.event.DynamicAgentCreationEvent
import com.embabel.agent.event.RankingChoiceRequestEvent
import com.embabel.agent.spi.Rankings
import com.embabel.agent.testing.FakeRanker
import com.embabel.agent.testing.RandomRanker
import com.embabel.common.util.kotlin.loggerFor
import kotlin.Throws

/**
 * Result of directly trying to execute a goal
 * Forcing client to discriminate
 */
class DynamicExecutionResult private constructor(

    /**
     * What triggered this result. Process input.
     */
    val basis: Any,

    val output: Any,
    val agentProcess: AgentProcess,
) {

    companion object {

        fun fromProcessStatus(
            basis: Any,
            agentProcess: AgentProcess,
        ): DynamicExecutionResult =
            when (agentProcess.status) {
                AgentProcessStatusCode.COMPLETED -> {
                    DynamicExecutionResult(
                        basis = basis,
                        output = agentProcess.finalResult()!!,
                        agentProcess = agentProcess,
                    )
                }

                AgentProcessStatusCode.WAITING -> {
                    throw ProcessWaitingException(
                        agentProcess = agentProcess,
                        // TODO this is dirty
                        awaitable = agentProcess.finalResult() as Awaitable<*, *>
                    )
                }

                AgentProcessStatusCode.FAILED -> {
                    throw ProcessExecutionFailedException(
                        agentProcess = agentProcess,
                        detail = "Process ${agentProcess.id} failed"
                    )
                }

                else -> {
                    TODO("Handle other statuses: ${agentProcess.status}")
                }
            }
    }
}

/**
 * Used for control flow
 */
sealed class ProcessExecutionException(
    val agentProcess: AgentProcess?,
    message: String,
) : Exception(message)

class NoGoalFound(
    val basis: Any,
    val goalRankings: Rankings<Goal>,
) : ProcessExecutionException(null, "Goal not found: ${goalRankings.rankings.joinToString(",")}")

class NoAgentFound(
    val basis: Any,
    val agentRankings: Rankings<Agent>,
) : ProcessExecutionException(null, "Agent not found: ${agentRankings.rankings.joinToString(",")}")

class ProcessExecutionFailedException(
    agentProcess: AgentProcess,
    val detail: String,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} failed: $detail")


class ProcessWaitingException(
    agentProcess: AgentProcess,
    val awaitable: Awaitable<*, *>,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} is waiting for ${awaitable.infoString()}")

/**
 * Choose a goal based on the user input and try to achieve it.
 * Open execution model:
 * May bring in actions and conditions from multiple agents to help achieve the goal.
 * Doesn't need reified types because we don't know the type yet.
 */
@Throws(ProcessExecutionException::class)
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
        .filter { it.score > properties.goalConfidenceCutOff }
    val goalChoice = credibleGoals.firstOrNull()
    if (goalChoice == null) {
        eventListener.onPlatformEvent(
            goalChoiceEvent.noDeterminationEvent(
                rankings = goalRankings,
                confidenceCutoff = properties.goalConfidenceCutOff
            )
        )
        throw NoGoalFound(goalRankings = goalRankings, basis = userInput)
    }

    loggerFor<AgentPlatform>().debug(
        "Goal choice {} with confidence {} for user intent {}: Choices were {}",
        goalChoice.match.name,
        goalChoice.score,
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
        name = "goal-${goalChoice.match.name}",
        description = goalChoice.match.description,
    )
        .withSingleGoal(goalChoice.match)
    eventListener.onPlatformEvent(
        DynamicAgentCreationEvent(
            agent = goalAgent,
            agentPlatform = this,
            basis = userInput,
        )
    )
    val agentProcess = runAgentFrom(
        processOptions = processOptions,
        agent = goalAgent,
        bindings = mapOf(
            "it" to userInput
        )
    )

    return DynamicExecutionResult.fromProcessStatus(basis = userInput, agentProcess = agentProcess)
}


/**
 * Choose an agent based on the user input and run it.
 * Closed execution model:
 * Will never mix actions and goals from different agents.
 * Doesn't need reified types because we don't know the type yet.
 */
@Throws(ProcessExecutionException::class)
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
        .filter { it.score > properties.agentConfidenceCutOff }
    val agentChoice = credibleAgents.firstOrNull()
    if (agentChoice == null) {
        eventListener.onPlatformEvent(
            agentChoiceEvent.noDeterminationEvent(
                rankings = agentRankings,
                confidenceCutoff = properties.agentConfidenceCutOff
            )
        )
        throw NoAgentFound(agentRankings = agentRankings, basis = userInput)
    }

    loggerFor<AgentPlatform>().debug(
        "Agent choice {} with confidence {} for user intent {}: Choices were {}",
        agentChoice.match.name,
        agentChoice.score,
        intent,
        agents().joinToString("\n") { it.name },
    )
    eventListener.onPlatformEvent(
        agentChoiceEvent.determinationEvent(
            choice = agentChoice,
            rankings = agentRankings,
        )
    )

    val agent = agentChoice.match
    eventListener.onPlatformEvent(
        DynamicAgentCreationEvent(
            agent = agentChoice.match,
            agentPlatform = this,
            basis = userInput,
        )
    )
    val agentProcess = runAgentFrom(
        processOptions = processOptions,
        agent = agent,
        bindings = mapOf(
            "it" to userInput
        )
    )
    return DynamicExecutionResult.fromProcessStatus(
        basis = userInput,
        agentProcess = agentProcess,
    )
}
