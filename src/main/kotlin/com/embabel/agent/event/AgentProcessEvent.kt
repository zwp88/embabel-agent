/*
 * Copyright 2025 Embabel Software, Inc.
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

import com.embabel.agent.Action
import com.embabel.agent.ActionStatus
import com.embabel.agent.AgentProcess
import com.embabel.agent.AgentProcessStatus
import com.embabel.agent.primitive.LlmOptions
import com.embabel.common.core.types.Timed
import com.embabel.plan.Plan
import com.embabel.plan.goap.WorldState
import org.springframework.ai.tool.ToolCallback
import java.time.Duration
import java.time.Instant

/**
 * Event relating to a specific process
 */
interface AgentProcessEvent : AgenticEvent {

    val processId: String
}

abstract class AbstractAgentProcessEvent(
    val agentProcess: AgentProcess,
) : AgentProcessEvent {

    override val timestamp: Instant = Instant.now()

    override val processId: String
        get() = agentProcess.id
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

class ActionExecutionStartEvent(
    agentProcess: AgentProcess,
    val action: Action,
) : AbstractAgentProcessEvent(agentProcess)

class ActionExecutionResultEvent(
    agentProcess: AgentProcess,
    val action: Action,
    val actionStatus: ActionStatus,
) : AbstractAgentProcessEvent(agentProcess)

/**
 * Call to a function from an LLM
 */
class AgentProcessFunctionCallRequestEvent(
    agentProcess: AgentProcess,
    val function: String,
    val arguments: Map<String, Any>,
    val llmOptions: LlmOptions,
) : AbstractAgentProcessEvent(agentProcess) {

    fun responseEvent(response: String, runningTime: Duration): AgentProcessFunctionCallResponseEvent {
        return AgentProcessFunctionCallResponseEvent(
            agentProcess = agentProcess,
            function = function,
            arguments = arguments,
            llmOptions = llmOptions,
            response = response,
            runningTime = runningTime
        )
    }
}

class AgentProcessFunctionCallResponseEvent internal constructor(
    agentProcess: AgentProcess,
    val function: String,
    val arguments: Map<String, Any>,
    val llmOptions: LlmOptions,
    val response: String,
    override val runningTime: Duration,
) : AbstractAgentProcessEvent(agentProcess), Timed

class AgentProcessTerminationEvent(
    val agentProcessStatus: AgentProcessStatus,
) : AbstractAgentProcessEvent(agentProcessStatus.agentProcess)

class LlmTransformRequestEvent<I, O>(
    agentProcess: AgentProcess,
    val input: I,
    val outputClass: Class<O>,
    val llmOptions: LlmOptions,
    val tools: Collection<ToolCallback>,
    val prompt: String,
) : AbstractAgentProcessEvent(agentProcess) {

    fun responseEvent(response: O, runningTime: Duration): LlmTransformResponseEvent<I, O> {
        return LlmTransformResponseEvent(
            agentProcess = agentProcess,
            input = input,
            outputClass = outputClass,
            llmOptions = llmOptions,
            prompt = prompt,
            response = response,
            runningTime = runningTime
        )
    }
}

class LlmTransformResponseEvent<I, O> internal constructor(
    agentProcess: AgentProcess,
    val input: I,
    val outputClass: Class<O>,
    val llmOptions: LlmOptions,
    val prompt: String,
    val response: O,
    override val runningTime: Duration,
) : AbstractAgentProcessEvent(agentProcess), Timed

/**
 * Binding to context
 */
data class ObjectAddedEvent(
    override val processId: String,
    val value: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentProcessEvent

data class ObjectBoundEvent(
    override val processId: String,
    val name: String,
    val value: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentProcessEvent
