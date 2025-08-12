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
package com.embabel.agent.tools.agent

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.api.common.autonomy.DefaultPlanLister
import com.embabel.agent.common.Constants
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.domain.io.UserInput

class ToolGroupFactory(
    private val autonomy: Autonomy,
) {

    /**
     * Creates a ToolGroup containing achievable goals for the chat agent
     * from the present OperationContext
     * @param bindings any additional bindings to pass to the agent process
     */
    fun achievableGoalsToolGroup(
        context: OperationContext,
        bindings: Map<String, Any>,
    ): ToolGroup {
        val planLister = DefaultPlanLister(context.agentPlatform())
        val achievableGoals = planLister.achievableGoals(
            processOptions = context.processContext.processOptions,
            bindings = bindings,
        )
        return ToolGroup.Companion(
            metadata = ToolGroupMetadata.Companion(
                name = "Default chat tools",
                description = "Default tools for chat agent",
                role = "chat",
                provider = Constants.EMBABEL_PROVIDER,
                permissions = emptySet(),
            ),
            toolCallbacks = achievableGoals.mapIndexed { i, goal ->
                GoalToolCallback(
                    autonomy = autonomy,
                    name = "tool_$i",
                    goal = goal,
                    textCommunicator = PromptedTextCommunicator,
                    objectMapper = context.agentPlatform().platformServices.objectMapper,
                    inputType = UserInput::class.java,
                )
            }
        )
    }
}
