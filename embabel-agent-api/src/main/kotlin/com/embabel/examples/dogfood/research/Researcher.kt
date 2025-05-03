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
package com.embabel.examples.dogfood.research

import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.special.UserInput

/**
 * Researcher agent that can be used independently or as a subflow
 */
@Agent(
    description = "Perform deep web research on a topic",
    toolGroups = [ToolGroup.WEB, ToolGroup.BROWSER_AUTOMATION]
)
class Researcher {

    @Action
    fun research(
        userInput: UserInput,
    ) {
        // Research logic goes here
    }
}
