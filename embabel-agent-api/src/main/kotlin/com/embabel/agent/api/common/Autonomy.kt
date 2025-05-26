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

import com.embabel.agent.common.Constants
import com.embabel.agent.core.*
import com.embabel.agent.core.hitl.Awaitable
import com.embabel.agent.core.hitl.AwaitableResponse
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.event.AgentPlatformEvent
import com.embabel.agent.event.DynamicAgentCreationEvent
import com.embabel.agent.event.RankingChoiceRequestEvent
import com.embabel.agent.spi.Ranker
import com.embabel.agent.spi.Rankings
import com.embabel.agent.testing.FakeRanker
import com.embabel.agent.testing.RandomRanker
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.ZeroToOne
import com.embabel.common.util.loggerFor
import com.embabel.plan.goap.AStarGoapPlanner
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.WorldStateDeterminer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Successful result of directly trying to execute a goal
 * and forcing client to discriminate between possible goals.
 * Failure results in an exception being thrown.
 */
class DynamicExecutionResult private constructor(

    /**
     * What triggered this result. Process input.
     */
    val basis: Any,

    /**
     * Output object
     */
    val output: Any,

    /**
     * Process that executed and is now complete
     */
    val agentProcess: AgentProcess,
) : HasInfoString {

    override fun infoString(verbose: Boolean?): String {
        return "DynamicExecutionResult(basis=$basis, output=$output, agentProcess=${agentProcess.infoString(verbose)})"
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromProcessStatus(
            basis: Any,
            agentProcess: AgentProcess,
        ): DynamicExecutionResult =
            when (agentProcess.status) {
                AgentProcessStatusCode.COMPLETED -> {
                    DynamicExecutionResult(
                        basis = basis,
                        output = agentProcess.lastResult()!!,
                        agentProcess = agentProcess,
                    )
                }

                AgentProcessStatusCode.WAITING -> {
                    throw ProcessWaitingException(
                        agentProcess = agentProcess,
                        // TODO this is dirty
                        awaitable = agentProcess.lastResult() as Awaitable<*, AwaitableResponse>,
                    )
                }

                AgentProcessStatusCode.FAILED -> {
                    throw ProcessExecutionFailedException(
                        agentProcess = agentProcess,
                        detail = "Process ${agentProcess.id} failed: ${agentProcess.failureInfo}",
                    )
                }

                AgentProcessStatusCode.STUCK -> {
                    throw ProcessExecutionStuckException(
                        agentProcess = agentProcess,
                        detail = "Process ${agentProcess.id} stuck"
                    )
                }

                AgentProcessStatusCode.TERMINATED -> {
                    throw ProcessExecutionTerminatedException(
                        agentProcess = agentProcess,
                        detail = "Process ${agentProcess.id} was terminated: ${agentProcess.failureInfo}",
                    )
                }

                else -> {
                    error("Unexpected process status: ${agentProcess.status}")
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

/**
 * The Ranker chose a goal, but it was rejected by the GoalApprover
 */
class GoalNotApproved(
    val basis: Any,
    val goalRankings: Rankings<Goal>,
    val reason: String,
    override val agentPlatform: AgentPlatform,
) : AgentPlatformEvent,
    ProcessExecutionException(null, "Goal not approved because $reason: ${goalRankings.rankings.joinToString(",")}") {

    override val timestamp: Instant = Instant.now()

}

class NoAgentFound(
    val basis: Any,
    val agentRankings: Rankings<Agent>,
) : ProcessExecutionException(null, "Agent not found: ${agentRankings.rankings.joinToString(",")}")

class ProcessExecutionFailedException(
    agentProcess: AgentProcess,
    val detail: String,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} failed: $detail")

class ProcessExecutionTerminatedException(
    agentProcess: AgentProcess,
    val detail: String,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} terminated: $detail")

class ProcessExecutionStuckException(
    agentProcess: AgentProcess,
    val detail: String,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} stuck: $detail")

class ProcessWaitingException(
    agentProcess: AgentProcess,
    val awaitable: Awaitable<*, AwaitableResponse>,
) : ProcessExecutionException(agentProcess, "Process ${agentProcess.id} is waiting for ${awaitable.infoString()}")


/**
 * Autonomy properties
 * @param goalConfidenceCutOff Goal confidence cut-off, between 0 and 1, which is required
 * to have confidence in executing the most promising goal.
 * @param agentConfidenceCutOff Agent confidence cut-off, between 0 and 1, which is required
 * to have confidence in executing the most promising agent.
 */
@ConfigurationProperties("embabel.autonomy")
data class AutonomyProperties(
    val goalConfidenceCutOff: ZeroToOne = 0.6,
    val agentConfidenceCutOff: ZeroToOne = 0.6,
)

data class GoalChoiceApprovalRequest(
    val goal: Goal,
    val intent: String,
    val rankings: Rankings<Goal>,
)

sealed interface GoalChoiceApprovalResponse {
    val request: GoalChoiceApprovalRequest
    val approved: Boolean
}

data class GoalChoiceApproved(
    override val request: GoalChoiceApprovalRequest,
) : GoalChoiceApprovalResponse {
    override val approved: Boolean = true
}

data class GoalChoiceNotApproved(
    override val request: GoalChoiceApprovalRequest,
    val reason: String,
) : GoalChoiceApprovalResponse {
    override val approved: Boolean = false
}

/**
 * Implemented by objects that can veto goal choice
 */
fun interface GoalChoiceApprover {

    /**
     * Respond to a goal choice.
     */
    fun approve(
        goalChoiceApprovalRequest: GoalChoiceApprovalRequest
    ): GoalChoiceApprovalResponse

    companion object {
        val APPROVE_ALL = GoalChoiceApprover { GoalChoiceApproved(it) }

        /**
         * Approve if the score is greater than this value
         */
        infix fun approveWithScoreOver(score: ZeroToOne) = GoalChoiceApprover { request ->
            if ((request.rankings.rankings.firstOrNull()?.score ?: 0.0) > score) {
                GoalChoiceApproved(request)
            } else {
                GoalChoiceNotApproved(
                    request = request,
                    reason = "Score ${request.rankings.rankings.firstOrNull()?.score} is not over $score",
                )
            }
        }
    }

}

/**
 * Adds autonomy to an AgentPlatform, with the ability to choose
 * goals and agents dynamically, given user input.
 * Then calls the AgentPlatform to execute.
 */
@Service
class Autonomy(
    val agentPlatform: AgentPlatform,
    private val ranker: Ranker,
    val properties: AutonomyProperties,
) {

    private val eventListener = agentPlatform.platformServices.eventListener

    /**
     * Choose a goal based on the user input and try to achieve it.
     * Open execution model.
     * May bring in actions and conditions from multiple agents to help achieve the goal.
     * Doesn't need reified types because we don't know the type yet.
     * @param intent user intent
     * @param processOptions process options
     * @param goalChoiceApprover goal choice approver allowing goal choice to be rejected
     * @param agentScope scope to look for the agent
     */
    @Throws(ProcessExecutionException::class)
    fun chooseAndAccomplishGoal(
        intent: String,
        processOptions: ProcessOptions = ProcessOptions(),
        goalChoiceApprover: GoalChoiceApprover,
        agentScope: AgentScope,
    ): DynamicExecutionResult {
        val userInput = UserInput(intent)
        val goalRun = createGoalSeeker(
            userInput = userInput,
            processOptions = processOptions,
            goalChoiceApprover = goalChoiceApprover,
            agentScope = agentScope,
            emitEvents = true,
        )
        val agentProcess = agentPlatform.runAgentFrom(
            processOptions = processOptions,
            agent = goalRun.agent,
            bindings = mapOf(
                IoBinding.DEFAULT_BINDING to userInput
            )
        )

        return DynamicExecutionResult.fromProcessStatus(
            basis = userInput,
            agentProcess = agentProcess,
        )
    }

    /**
     * Agent to seek a goal and the goal rankings that we used to choose the goal.
     */
    data class GoalSeeker(
        val agent: Agent,
        val rankings: Rankings<Goal>,
    )

    /**
     * Choose an agent based on the user input and run it.
     * Closed execution model:
     * Will never mix actions and goals from different agents.
     * Doesn't need reified types because we don't know the type yet.
     */
    @Throws(ProcessExecutionException::class)
    fun chooseAndRunAgent(
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
            agentPlatform = agentPlatform,
            type = Agent::class.java,
            basis = userInput,
            choices = agentPlatform.agents(),
        )
        eventListener.onPlatformEvent(agentChoiceEvent)
        val agentRankings = rankerToUse
            .rank(
                description = "agent",
                userInput = userInput.content,
                rankables = agentPlatform.agents()
            )
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
            agentPlatform.agents().joinToString("\n") { it.name },
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
                agentPlatform = agentPlatform,
                basis = userInput,
            )
        )
        return runAgent(userInput, processOptions, agent)
    }

    fun runAgent(
        userInput: UserInput,
        processOptions: ProcessOptions,
        agent: Agent
    ): DynamicExecutionResult {
        val agentProcess = agentPlatform.runAgentFrom(
            processOptions = processOptions,
            agent = agent,
            bindings = mapOf(
                IoBinding.DEFAULT_BINDING to userInput
            )
        )
        return DynamicExecutionResult.fromProcessStatus(
            basis = userInput,
            agentProcess = agentProcess,
        )
    }

    /**
     * Indicate which goal we'd use for this intent, and what agent we'd create.
     * Dry run capability available externally.
     */
    fun createGoalSeeker(
        intent: String,
        goalChoiceApprover: GoalChoiceApprover,
        agentScope: AgentScope,
    ): GoalSeeker = createGoalSeeker(
        userInput = UserInput(intent),
        processOptions = ProcessOptions(),
        goalChoiceApprover = goalChoiceApprover,
        emitEvents = false,
        agentScope = agentScope,
    )

    /**
     * Choose a goal, showing workings and create an agent.
     * @param emitEvents whether to emit events. If we're just
     * doing a dry run, we don't want to emit events
     */
    private fun createGoalSeeker(
        userInput: UserInput,
        processOptions: ProcessOptions,
        goalChoiceApprover: GoalChoiceApprover,
        emitEvents: Boolean,
        agentScope: AgentScope,
    ): GoalSeeker {
        // Use a fake goal ranker if we are in test mode and don't already have a fake one
        // Enables running under integration tests and in test mode otherwise with production config
        val rankerToUse = if (processOptions.test && ranker !is FakeRanker) {
            RandomRanker()
        } else {
            ranker
        }

        val goalChoiceEvent = RankingChoiceRequestEvent(
            agentPlatform = agentPlatform,
            type = Goal::class.java,
            basis = userInput,
            choices = agentScope.goals,
        )
        if (emitEvents) eventListener.onPlatformEvent(goalChoiceEvent)
        val goalRankings = rankerToUse
            .rank(
                description = "goal",
                userInput = userInput.content,
                rankables = agentScope.goals
            )
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
            userInput.content,
            agentScope.goals.joinToString("\n") { it.name },
        )
        if (emitEvents) eventListener.onPlatformEvent(
            goalChoiceEvent.determinationEvent(
                choice = goalChoice,
                rankings = goalRankings,
            )
        )

        // Check if the goal is approved
        val approval =
            goalChoiceApprover.approve(
                GoalChoiceApprovalRequest(
                    goal = goalChoice.match,
                    intent = userInput.content,
                    rankings = goalRankings,
                )
            )
        if (approval is GoalChoiceNotApproved) {
            val goalNotApproved = GoalNotApproved(
                basis = userInput,
                goalRankings = goalRankings,
                reason = approval.reason,
                agentPlatform = agentPlatform,
            )
            if (emitEvents) eventListener.onPlatformEvent(goalNotApproved)
            throw goalNotApproved
        }

        val goalAgent = createGoalAgent(userInput = userInput, agentScope = agentScope, goal = goalChoice.match)
        if (emitEvents) eventListener.onPlatformEvent(
            DynamicAgentCreationEvent(
                agent = goalAgent,
                agentPlatform = agentPlatform,
                basis = userInput,
            )
        )
        return GoalSeeker(agent = goalAgent, rankings = goalRankings)
    }

    fun createGoalAgent(
        userInput: UserInput,
        agentScope: AgentScope,
        goal: Goal
    ): Agent {
        return agentScope.createAgent(
            name = "goal-${goal.name}",
            provider = Constants.EMBABEL_PROVIDER,
            description = goal.description,
        )
            .withSingleGoal(goal)
            .prune(userInput)
    }

    /**
     * Agent with only relevant actions
     */
    private fun Agent.prune(userInput: UserInput): Agent {
        loggerFor<Autonomy>().debug(
            "Raw agent: {}",
            infoString(),
        )
        val map = mutableMapOf<String, ConditionDetermination>()
        for (condition in this.planningSystem.knownConditions()) {
            map[condition] = ConditionDetermination.FALSE
        }
        loggerFor<Autonomy>().info("Pruning agent instance from {}", map)
        map += ("it:${userInput::class.qualifiedName}" to ConditionDetermination.TRUE)

        val planner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(
                map
            ),
        )
        val pruned = planner.prune(planningSystem)
        loggerFor<Autonomy>().info(
            "Pruned planning system: {}",
            pruned.infoString(),
        )
        return pruneTo(pruned)
    }

}
