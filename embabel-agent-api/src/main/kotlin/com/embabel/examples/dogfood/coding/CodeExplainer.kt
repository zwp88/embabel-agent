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
package com.embabel.examples.dogfood.coding

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.create
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.common.ai.model.LlmOptions

data class Explanation(
    override val text: String,
) : HasContent

@Agent(description = "Explain code")
class CodeExplainer(
    val root: String = System.getProperty("user.dir"),
) {

    @Action(
        toolGroups = ["file"],
    )
    @AchievesGoal(description = "Code has been explained to the user")
    fun explainCode(userInput: UserInput, payload: OperationPayload): Explanation {
        val explanation: String = payload.promptRunner(
            llm = LlmOptions(
                AnthropicModels.CLAUDE_37_SONNET
            )
        ).create(
            """
                Explain code
                Use the file tools to read code and directories before explaining it

                The user request:
                "${userInput.content}"
            """.trimIndent(),
        )
        return Explanation(explanation)
    }
}
