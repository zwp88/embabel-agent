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

import com.embabel.agent.Agent
import com.embabel.agent.AgentPlatform
import com.embabel.agent.ZeroToOne
import com.embabel.agent.spi.GoalRanking
import com.embabel.agent.spi.GoalRankings
import java.time.Instant

/**
 * System event such as deployment
 */
interface AgentPlatformEvent : AgenticEvent {

    val agentPlatform: AgentPlatform
}

data class GoalChoiceRequestEvent(
    override val agentPlatform: AgentPlatform,
    val basis: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentPlatformEvent {

    fun determinationEvent(goalChoice: GoalRanking, goalRankings: GoalRankings): GoalChoiceMadeEvent {
        return GoalChoiceMadeEvent(
            agentPlatform = agentPlatform,
            goalChoice = goalChoice,
            goalRankings = goalRankings,
            basis = basis,
            timestamp = timestamp,
        )
    }

    fun noDeterminationEvent(
        goalRankings: GoalRankings,
        goalConfidenceCutOff: ZeroToOne
    ): GoalChoiceCouldNotBeMadeEvent {
        return GoalChoiceCouldNotBeMadeEvent(
            agentPlatform = agentPlatform,
            goalRankings = goalRankings,
            goalConfidenceCutOff = goalConfidenceCutOff,
            basis = basis,
            timestamp = timestamp,
        )
    }
}

/**
 * @param basis why we chose this goal
 */
data class GoalChoiceMadeEvent(
    override val agentPlatform: AgentPlatform,
    val goalChoice: GoalRanking,
    val goalRankings: GoalRankings,
    val basis: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentPlatformEvent

data class GoalChoiceCouldNotBeMadeEvent(
    override val agentPlatform: AgentPlatform,
    val goalRankings: GoalRankings,
    val goalConfidenceCutOff: ZeroToOne,
    val basis: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentPlatformEvent

/**
 * Emitted when we've created an agent for a specific task
 * @param basis why we chose this agent
 */
data class DynamicAgentCreationEvent(
    override val agentPlatform: AgentPlatform,
    val agent: Agent,
    val basis: Any,
    override val timestamp: Instant = Instant.now(),
) : AgentPlatformEvent
