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
import com.embabel.agent.spi.*
import com.embabel.plan.Planner
import com.embabel.plan.WorldState
import com.embabel.plan.goap.WorldStateDeterminer
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * Abstract implementation of AgentProcess that provides common functionality
 */
abstract class AbstractAgentProcess(
    override val id: String,
    override val parentId: String?,
    override val agent: Agent,
    protected val processOptions: ProcessOptions,
    protected val blackboard: Blackboard,
    @get:JsonIgnore
    protected val platformServices: PlatformServices,
    override val timestamp: Instant = Instant.now(),
) : AgentProcess, Blackboard by blackboard {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private var _lastWorldState: WorldState? = null

    protected var goalName: String? = null

    protected val _history: MutableList<ActionInvocation> = mutableListOf()

    protected var _status: AgentProcessStatusCode = AgentProcessStatusCode.RUNNING

    override val lastWorldState: WorldState?
        get() = _lastWorldState

    override val processContext = ProcessContext(
        platformServices = platformServices,
        agentProcess = this,
        processOptions = processOptions,
    )

    /**
     * Get the WorldStateDeterminer for this process
     */
    protected abstract val worldStateDeterminer: WorldStateDeterminer

    /**
     * Get the planner for this process
     */
    protected abstract val planner: Planner<*, *, *>

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
            error("Agent ${agent.name} has no goals: ${agent.infoString(verbose = true)}")
        }

        tick()
        while (status == AgentProcessStatusCode.RUNNING) {
            val earlyTermination = processOptions.control.earlyTerminationPolicy.shouldTerminate(this)
            if (earlyTermination != null) {
                logger.debug(
                    "Process {} terminated by {} because {}",
                    this.id,
                    earlyTermination.policy,
                    earlyTermination.reason,
                )
                platformServices.eventListener.onProcessEvent(earlyTermination)
                _status = AgentProcessStatusCode.FAILED
                return this
            }
            tick()
        }
        when (status) {
            AgentProcessStatusCode.RUNNING -> {
                logger.debug("Process {} is happily running: {}", this.id, status)
            }

            AgentProcessStatusCode.COMPLETED -> {
                platformServices.eventListener.onProcessEvent(AgentProcessFinishedEvent(this))
            }

            AgentProcessStatusCode.FAILED -> {
                platformServices.eventListener.onProcessEvent(AgentProcessFinishedEvent(this))
            }

            AgentProcessStatusCode.KILLED -> {
                // Event will have been raised at the point of termination
            }

            AgentProcessStatusCode.WAITING -> {
                platformServices.eventListener.onProcessEvent(AgentProcessWaitingEvent(this))
            }

            AgentProcessStatusCode.STUCK -> {
                platformServices.eventListener.onProcessEvent(AgentProcessPausedEvent(this))
                handleStuck(agent)
            }

            AgentProcessStatusCode.PAUSED -> {
                platformServices.eventListener.onProcessEvent(AgentProcessStuckEvent(this))
                handleStuck(agent)
            }
        }
        return this
    }

    /**
     * Try to resolve a stuck process using StuckHandler if provided
     */
    protected fun handleStuck(agent: Agent) {
        val stuckHandler = agent.stuckHandler
        if (stuckHandler == null) {
            logger.warn("Process {} is stuck: no handler", this.id)
            return
        }
        val result = stuckHandler.handleStuck(this)
        platformServices.eventListener.onProcessEvent(result)
        when (result.code) {
            StuckHandlingResultCode.REPLAN -> {
                logger.info("Process {} unstuck and will replan: {}", this.id, result.message)
                _status = AgentProcessStatusCode.RUNNING
                run()
            }

            StuckHandlingResultCode.NO_RESOLUTION -> {
                logger.warn("Process {} stuck: {}", this.id, result.message)
                _status = AgentProcessStatusCode.STUCK
            }
        }
    }

    override fun tick(): AgentProcess {
        val worldState = worldStateDeterminer.determineWorldState()
        _lastWorldState = worldState
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

        // Let subclasses handle the planning and execution
        return formulateAndExecutePlan(worldState)
    }

    /**
     * Execute the plan based on the current world state
     * @param worldState The current world state
     */
    protected abstract fun formulateAndExecutePlan(
        worldState: WorldState,
    ): AgentProcess

    /**
     * Execute an action
     */
    protected fun executeAction(action: Action): ActionStatus {
        val outputTypes: Map<String, SchemaType> =
            action.outputs.associateBy({ it.name }, { agent.resolveSchemaType(it.type) })
        logger.debug(
            "⚙️ Process {} executing action {}: outputTypes={}",
            id,
            action.name,
            outputTypes,
        )

        val actionExecutionStartEvent = ActionExecutionStartEvent(
            agentProcess = this,
            action = action,
        )
        platformServices.eventListener.onProcessEvent(actionExecutionStartEvent)
        val actionExecutionSchedule = platformServices.operationScheduler.scheduleAction(actionExecutionStartEvent)
        when (actionExecutionSchedule) {
            is ProntoActionExecutionSchedule -> {
                // Do nothing
            }

            is DelayedActionExecutionSchedule -> {
                // Delay and move on
                logger.debug("Process {} delayed action {}: {}", id, action.name, actionExecutionSchedule)
                Thread.sleep(actionExecutionSchedule.delay.toMillis())
                logger.debug("Process {} delayed action {}: done", id, action.name)
            }

            is ScheduledActionExecutionSchedule -> {
                return ActionStatus(
                    Duration.between(actionExecutionStartEvent.timestamp, Instant.now()),
                    ActionStatusCode.PAUSED
                )
            }
        }

        val timestamp = Instant.now()
        val actionStatus = action.qos.retryTemplate().execute<ActionStatus, Exception> {
            action.execute(
                processContext = processContext,
                outputTypes = outputTypes,
                action = action,
            )
        }
        val runningTime = Duration.between(timestamp, Instant.now())
        _history += ActionInvocation(
            actionName = action.name,
            timestamp = timestamp,
            runningTime = runningTime,
        )
        platformServices.eventListener.onProcessEvent(
            actionExecutionStartEvent.resultEvent(
                actionStatus = actionStatus,
            )
        )

        logger.debug("New world state: {}", worldStateDeterminer.determineWorldState())
        return actionStatus
    }

    /**
     * Convert action status to agent process status
     */
    protected fun actionStatusToAgentProcessStatus(actionStatus: ActionStatus): AgentProcessStatusCode {
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

            ActionStatusCode.PAUSED -> {
                logger.debug("⏳ Process {} action {} paused", id, actionStatus.status)
                AgentProcessStatusCode.PAUSED
            }
        }
    }
}
