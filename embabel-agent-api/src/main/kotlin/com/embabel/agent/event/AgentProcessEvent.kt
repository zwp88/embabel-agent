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
package com.embabel.agent.event

import com.embabel.agent.core.*
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.support.springai.ChatModelCallEvent
import com.embabel.chat.Message
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.Timed
import com.embabel.common.util.VisualizableTask
import com.embabel.plan.Goal
import com.embabel.plan.Plan
import com.embabel.plan.WorldState
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.ai.chat.prompt.Prompt
import java.time.Duration
import java.time.Instant

/**
 * Event relating to a specific process. Most events are related to a process.
 */
interface AgentProcessEvent : AgenticEvent, InProcess

/**
 * Convenient superclass for AgentProcessEvent implementations
 */
abstract class AbstractAgentProcessEvent(
    @JsonIgnore
    val agentProcess: AgentProcess,
) : AgentProcessEvent {

    override val timestamp: Instant = Instant.now()

    override val processId: String
        get() = agentProcess.id

    val history: List<ActionInvocation>
        get() = agentProcess.history

    val status: AgentProcessStatusReport get() = agentProcess.statusReport()
}

class AgentProcessCreationEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)

class AgentProcessReadyToPlanEvent(
    agentProcess: AgentProcess,
    val worldState: WorldState,
) : AbstractAgentProcessEvent(agentProcess)

class AgentProcessPlanFormulatedEvent(
    agentProcess: AgentProcess,
    val worldState: WorldState,
    val plan: Plan,
) : AbstractAgentProcessEvent(agentProcess)

class GoalAchievedEvent(
    agentProcess: AgentProcess,
    val worldState: WorldState,
    val goal: Goal,
) : AbstractAgentProcessEvent(agentProcess)

class ActionExecutionStartEvent(
    agentProcess: AgentProcess,
    val action: Action,
) : AbstractAgentProcessEvent(agentProcess) {

    fun resultEvent(
        actionStatus: ActionStatus,
    ): ActionExecutionResultEvent {
        return ActionExecutionResultEvent(
            agentProcess = agentProcess,
            action = action,
            actionStatus = actionStatus,
            runningTime = Duration.between(timestamp, Instant.now())
        )
    }
}

class ActionExecutionResultEvent internal constructor(
    agentProcess: AgentProcess,
    val action: Action,
    val actionStatus: ActionStatus,
    override val runningTime: Duration,
) : AbstractAgentProcessEvent(agentProcess), Timed

/**
 * Call to a function from an LLM
 * @param correlationId correlation ID for this tool call, useful for UI
 */
class ToolCallRequestEvent(
    agentProcess: AgentProcess,
    val action: Action?,
    val tool: String,
    val toolGroupMetadata: ToolGroupMetadata?,
    val toolInput: String,
    val llmOptions: LlmOptions,
    val correlationId: String = "${agentProcess.id}-$tool-${System.currentTimeMillis()}",
) : AbstractAgentProcessEvent(agentProcess) {

    fun responseEvent(
        result: Result<String>,
        runningTime: Duration,
    ): ToolCallResponseEvent {
        return ToolCallResponseEvent(
            request = this,
            result = result,
            runningTime = runningTime
        )
    }
}

/**
 * Response from a tool call, whether successful or not.
 */
class ToolCallResponseEvent internal constructor(
    val request: ToolCallRequestEvent,
    val result: Result<String>,
    override val runningTime: Duration,
) : AbstractAgentProcessEvent(request.agentProcess), Timed

/**
 * The agent process has finished.
 * It may have completed successfully or failed.
 * Check the status code to determine the outcome.
 */
sealed class AgentProcessFinishedEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)

class AgentProcessCompletedEvent(
    agentProcess: AgentProcess,
) : AgentProcessFinishedEvent(agentProcess) {

    val result: Any
        get() =
            agentProcess.lastResult() ?: throw IllegalStateException("Agent process ${agentProcess.id} has no result")
}

class AgentProcessFailedEvent(
    agentProcess: AgentProcess,
) : AgentProcessFinishedEvent(agentProcess)

class AgentProcessWaitingEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)

class AgentProcessPausedEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)

/**
 * The AgentProcess is unable to plan from its present state.
 * @param agentProcess the agent process
 */
class AgentProcessStuckEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)


class LlmRequestEvent<O>(
    agentProcess: AgentProcess,
    action: Action?,
    val outputClass: Class<O>,
    val interaction: LlmInteraction,
    val llm: Llm,
    val messages: List<Message>,
) : AbstractAgentProcessEvent(agentProcess) {

    /**
     * Return a low level event showing Spring AI prompt details.
     */
    fun callEvent(springAiPrompt: Prompt): ChatModelCallEvent<O> {
        return ChatModelCallEvent(
            agentProcess = agentProcess,
            outputClass = outputClass,
            interaction = interaction,
            llm = llm,
            springAiPrompt = springAiPrompt
        )
    }

    fun responseEvent(
        response: O,
        runningTime: Duration,
    ): LlmResponseEvent<O> {
        return LlmResponseEvent(
            request = this,
            outputClass = outputClass,
            response = response,
            runningTime = runningTime
        )
    }

    fun maybeResponseEvent(
        response: Result<O>,
        runningTime: Duration,
    ): LlmResponseEvent<Result<O>> {
        return LlmResponseEvent(
            request = this,
            outputClass = outputClass,
            response = response,
            runningTime = runningTime
        )
    }

    override fun toString(): String {
        return "LlmRequestEvent(outputClass=$outputClass, interaction=$interaction, messages=$messages)"
    }
}


/**
 * Response from an LLM
 * @param outputClass normally O, except if this is a maybe response
 * in which case it will be Result<O>
 */
class LlmResponseEvent<O> internal constructor(
    val request: LlmRequestEvent<*>,
    val outputClass: Class<*>,
    val response: O,
    override val runningTime: Duration,
) : AbstractAgentProcessEvent(request.agentProcess), Timed {

    override fun toString(): String {
        return "LlmResponseEvent(outputClass=$outputClass, request=$request, response=$response, runningTime=$runningTime)"
    }
}

/**
 * An object was bound to the process.
 * May or may not be found. See subclasses for details.
 */
interface ObjectBindingEvent : AgentProcessEvent {

    val value: Any

    val type: String
        get() = value::class.java.name
}

/**
 * Binding to context
 */
class ObjectAddedEvent(
    agentProcess: AgentProcess,
    override val value: Any,
) : AbstractAgentProcessEvent(agentProcess), ObjectBindingEvent

class ObjectBoundEvent(
    agentProcess: AgentProcess,
    val name: String,
    override val value: Any,
) : AbstractAgentProcessEvent(agentProcess), ObjectBindingEvent

/**
 * Progress update
 */
class ProgressUpdateEvent(
    agentProcess: AgentProcess,
    override val name: String,
    override val current: Int,
    override val total: Int,
) : AbstractAgentProcessEvent(agentProcess), VisualizableTask

class ProcessKilledEvent(
    agentProcess: AgentProcess,
) : AbstractAgentProcessEvent(agentProcess)

/**
 * Any RAG event that occurs within an agent process
 */
class AgentProcessRagEvent(
    agentProcess: AgentProcess,
    val ragEvent: RagEvent,
) : AbstractAgentProcessEvent(agentProcess)
