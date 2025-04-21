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
package com.embabel.agent.core.support

import com.embabel.agent.core.*
import com.embabel.agent.event.*
import com.embabel.agent.spi.PlatformServices
import com.embabel.plan.goap.AStarGoapPlanner
import com.embabel.plan.goap.WorldStateDeterminer
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import java.time.Instant

internal class SimpleAgentProcess(
    override val id: String,
    override val parentId: String?,
    override val agent: Agent,
    private val processOptions: ProcessOptions,
    val blackboard: Blackboard,
    @get:JsonIgnore
    val platformServices: PlatformServices,
    override val startedDate: Instant = Instant.now(),
) : AgentProcess, Blackboard by blackboard {

    private val logger = LoggerFactory.getLogger(SimpleAgentProcess::class.java)

    private var goalName: String? = null

    private val _history: MutableList<ActionInvocation> = mutableListOf()

    private var _status: AgentProcessStatusCode = AgentProcessStatusCode.RUNNING

    override val processContext = ProcessContext(
        platformServices = platformServices,
        agentProcess = this,
        processOptions = processOptions,
    )

    private val worldStateDeterminer: WorldStateDeterminer = BlackboardWorldStateDeterminer(processContext)

    private val planner = AStarGoapPlanner(worldStateDeterminer)

    override val status: AgentProcessStatusCode
        get() = _status

    override val history: List<ActionInvocation>
        get() = _history.toList()

    override fun bind(name: String, value: Any): Bindable {
        blackboard[name] = value
        processContext.onProcessEvent(
            ObjectBoundEvent(
                agentProcess = this,
                name = name,
                value = value,
            )
        )
        return this
    }

    override fun plusAssign(pair: Pair<String, Any>) {
        bind(pair.first, pair.second)
    }

    // Override set to bind so that delegation works
    override operator fun set(key: String, value: Any) {
        bind(key, value)
    }

    override fun addObject(value: Any): Bindable {
        blackboard.addObject(value)
        processContext.platformServices.eventListener.onProcessEvent(
            ObjectAddedEvent(
                agentProcess = this,
                value = value,
            )
        )
        return this
    }

    override operator fun plusAssign(value: Any) {
        addObject(value)
    }

    override fun run(): AgentProcess {
        if (agent.goals.isEmpty()) {
            logger.info("🤔 Process {} has no goals: {}", this.id, agent.goals)
            error("Agent ${agent.name} has no goals")
        }

        tick()
        var actions = 0
        while (status == AgentProcessStatusCode.RUNNING) {
            if (++actions > processOptions.maxActions) {
                logger.info("Process {} exceeded max actions: {}", this.id, processOptions.maxActions)
                _status = AgentProcessStatusCode.FAILED
                return this
            }
            tick()
        }
        when (status) {
            AgentProcessStatusCode.COMPLETED -> {
                platformServices.eventListener.onProcessEvent(AgentProcessFinishedEvent(this))
            }

            AgentProcessStatusCode.FAILED -> {
                platformServices.eventListener.onProcessEvent(AgentProcessFinishedEvent(this))
            }

            AgentProcessStatusCode.WAITING -> {
                platformServices.eventListener.onProcessEvent(AgentProcessWaitingEvent(this))
            }

            else -> {
                platformServices.eventListener.onProcessEvent(AgentProcessStuckEvent(this))
            }
        }
        return this
    }

    override fun tick(): AgentProcess {
        val worldState = worldStateDeterminer.determineWorldState()
        platformServices.eventListener.onProcessEvent(
            AgentProcessReadyToPlanEvent(
                agentProcess = this,
                worldState = worldState,
            )
        )
        logger.debug(
            "Process {} tick (about to plan): {}, blackboard={}",
            id,
            worldState,
            blackboard.infoString(verbose = false),
        )
        val plan = planner.bestValuePlanToAnyGoal(system = agent.goapSystem)
        if (plan == null) {
            logger.info(
                "❌ Process {} stuck: No plan from {} in {}, context={}",
                id,
                worldState,
                agent.goapSystem.infoString(),
                blackboard,
            )
            _status = AgentProcessStatusCode.STUCK
            return this
        }
        platformServices.eventListener.onProcessEvent(
            AgentProcessPlanFormulatedEvent(
                agentProcess = this,
                worldState = worldState,
                plan = plan,
            )
        )
        if (goalName != null && goalName != plan.goal.name) {
            logger.info("Process {} goal changed: {} -> {}", this.id, goalName, plan.goal.name)
            require(processOptions.allowGoalChange) {
                "Process ${this.id} goal changed from $goalName to ${plan.goal.name}, but allowGoalChange is false"
            }
        }
        goalName = plan.goal.name

        if (plan.isComplete()) {
            logger.debug(
                "✅ Process {} completed, achieving goal {} in {} seconds",
                this.id,
                plan.goal.name,
                this.runningTime.seconds,
            )
            logger.debug("Final blackboard: {}", blackboard.infoString())
            _status = AgentProcessStatusCode.COMPLETED
        } else {
            logger.debug("▶️ Process {} running: {}\n\tPlan: {}", id, worldState, plan.infoString())
            val agent = agent.actions.single { it.name == plan.actions.first().name }
            val actionStatus = executeAction(agent)
            _status = actionStatusToAgentProcessStatus(actionStatus)
        }
        return this
    }

    private fun executeAction(action: Action): ActionStatus {
        val outputTypes: Map<String, SchemaType> =
            action.outputs.associateBy({ it.name }, { agent.findDataType(it.type) })
        logger.debug(
            "⚙️ Process {} executing action {}: outputTypes={}",
            id,
            action.name,
            outputTypes,
        )

        val actionStatus = action.qos.retryTemplate().execute<ActionStatus, Exception> {
            platformServices.eventListener.onProcessEvent(
                ActionExecutionStartEvent(
                    agentProcess = this,
                    action = action,
                )
            )
            val actionStatus = action.execute(
                processContext = processContext,
                outputTypes = outputTypes,
                action = action,
            )
            _history += ActionInvocation(
                actionName = action.name,
            )
            platformServices.eventListener.onProcessEvent(
                ActionExecutionResultEvent(
                    agentProcess = this,
                    action = action,
                    actionStatus = actionStatus,
                )
            )
            actionStatus
        }
        logger.debug("New world state: {}", worldStateDeterminer.determineWorldState())
        return actionStatus
    }

    private fun actionStatusToAgentProcessStatus(actionStatus: ActionStatus): AgentProcessStatusCode {
        return when (actionStatus.status) {
            ActionStatusCode.SUCCEEDED -> {
                logger.debug("Process {} action {} is running", id, actionStatus.status)
                AgentProcessStatusCode.RUNNING
            }

            ActionStatusCode.FAILED -> {
                logger.debug("❌ Process {} action {} failed", id, actionStatus.status)
                AgentProcessStatusCode.FAILED
            }

            ActionStatusCode.WAITING -> {
                logger.debug("⏳ Process {} action {} waiting", id, actionStatus.status)
                AgentProcessStatusCode.WAITING
            }
        }
    }
}
