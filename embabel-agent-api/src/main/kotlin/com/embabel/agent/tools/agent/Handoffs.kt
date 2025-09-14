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

import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.core.ToolCallbackPublisher
import org.springframework.ai.tool.ToolCallback

/**
 * Handoffs to local agents.
 */
class Handoffs(
    autonomy: Autonomy,
    val outputTypes: List<Class<*>>,
    applicationName: String,
) : ToolCallbackPublisher {

    private val goalToolCallbackPublisher = PerGoalToolCallbackFactory(
        autonomy = autonomy,
        applicationName = applicationName,
        textCommunicator = PromptedTextCommunicator,
    )

    override val toolCallbacks: List<ToolCallback>
        get() = goalToolCallbackPublisher.goalTools(remoteOnly = false, listeners = emptyList())
            .filter { goalToolCallback ->
                outputTypes.any { outputType ->
                    goalToolCallback.goal.outputClass?.isAssignableFrom(outputType) == true
                }
            }

}
