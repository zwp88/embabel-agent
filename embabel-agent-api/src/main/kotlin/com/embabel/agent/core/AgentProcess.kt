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
import java.time.Instant

/**
 * Run of an agent
 */
interface AgentProcess : Blackboard, Timed {

    /**
     * Unique id of this process
     */
    val id: String

    val parentId: String?

    val processContext: ProcessContext

    val startedDate: Instant

    /**
     * The agent that this process is running for
     */
    val agent: Agent

    /**
     * Perform the next step only.
     * Return when an action has been completed and the process is ready to plan,
     * regardless of the result of the action.
     * @return status code of the action. Side effects may have occurred in Blackboard
     */
    fun tick(): AgentProcessStatus

    /**
     * Run the process as far as we can.
     * Might complete, fail, get stuck or hit a waiting state.
     * @return status code of the process. Side effects may have occurred in Blackboard
     */
    fun run(): AgentProcessStatus

    /**
     * How long this process has been running
     */
    override val runningTime get(): Duration = Duration.between(startedDate, Instant.now())

}
