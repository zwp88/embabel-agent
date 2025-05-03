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