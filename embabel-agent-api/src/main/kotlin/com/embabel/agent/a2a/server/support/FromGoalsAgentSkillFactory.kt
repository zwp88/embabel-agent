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
package com.embabel.agent.a2a.server.support

import com.embabel.agent.a2a.server.AgentSkillFactory
import com.embabel.agent.a2a.spec.AgentSkill
import com.embabel.agent.core.Goal

/**
 * Expose a skill for every goal defined in the agent platform.
 */
class FromGoalsAgentSkillFactory(
    private val goals: Set<Goal>,
) : AgentSkillFactory {

    override fun skills(namespace: String): List<AgentSkill> {
        return goals
            .map { goal ->
                AgentSkill(
                    id = "${namespace}_goal_${goal.name}",
                    name = goal.name,
                    description = goal.description,
                    inputModes = listOf("application/json"),
                    outputModes = listOf("application/json"),
                    tags = goal.tags.toList(),
                    examples = goal.examples.toList(),
                )
            }
    }
}
