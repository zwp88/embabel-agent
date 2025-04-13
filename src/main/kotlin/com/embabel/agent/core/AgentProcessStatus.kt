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

import com.embabel.common.core.types.Timed
import java.time.Duration

/**
 * Stuck means we failed to find a plan from here
 */
enum class AgentStatusCode {
    INVALID_AGENT, RUNNING, COMPLETED, FAILED, STUCK, WAITING
}

enum class ActionStatusCode {
    RUNNING, COMPLETED, FAILED
}

/**
 * Status of an agent or action
 */
interface OperationStatus<S> : Timed where S : Enum<S> {

    val status: S
}

open class ActionStatus(
    override val runningTime: Duration,
    override val status: ActionStatusCode,
) : OperationStatus<ActionStatusCode>

/**
 * Subclasses are open to allow AgentPlatform implementations to include their own
 * additional information
 */
sealed class AgentProcessStatus(
    val agentProcess: AgentProcess,
    override val status: AgentStatusCode,
) : OperationStatus<AgentStatusCode>, MayHaveFinalResult {

    private fun <O> doOnCompletion(block: (agentProcess: AgentProcess) -> O?): O? {
        return when (this) {
            is Completed -> {
                block(agentProcess)
            }

            is Failed -> error("Process failed")
            is Running -> throw IllegalStateException("Process is still running")
            is Stuck -> TODO("handle stuck")
            is Waiting -> TODO("handle waiting")
            is InvalidAgent -> error("Invalid agent: ${this.reason}")
        }
    }

    fun <O> resultOfType(outputClass: Class<O>): O = doOnCompletion {
        agentProcess.processContext.getValue("it", outputClass.simpleName) as O?
    }
        ?: error("No result found in process status")

    override fun finalResult(): Any? = doOnCompletion {
        agentProcess.processContext.blackboard.finalResult()
    }

    override val runningTime: Duration = agentProcess.runningTime

    open class InvalidAgent(agentProcess: AgentProcess, val reason: String) :
        AgentProcessStatus(agentProcess, AgentStatusCode.INVALID_AGENT)

    open class Completed(agentProcess: AgentProcess) : AgentProcessStatus(agentProcess, AgentStatusCode.COMPLETED)

    open class Failed(agentProcess: AgentProcess) : AgentProcessStatus(agentProcess, AgentStatusCode.FAILED)

    open class Running(agentProcess: AgentProcess) : AgentProcessStatus(agentProcess, AgentStatusCode.RUNNING)

    open class Stuck(agentProcess: AgentProcess) : AgentProcessStatus(agentProcess, AgentStatusCode.STUCK)

    open class Waiting(agentProcess: AgentProcess) : AgentProcessStatus(agentProcess, AgentStatusCode.WAITING)
}

inline fun <reified O> AgentProcessStatus.resultOfType(): O = resultOfType(O::class.java)
