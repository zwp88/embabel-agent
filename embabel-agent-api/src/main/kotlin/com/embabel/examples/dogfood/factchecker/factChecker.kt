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
package com.embabel.examples.dogfood.factchecker

import com.embabel.agent.api.common.InputActionContext
import com.embabel.agent.api.common.createObject
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.aggregate
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.io.UserInput
import com.embabel.common.ai.model.LlmOptions

data class FactualAssertion(val standaloneAssertion: String)
data class FactualAssertions(val factualAssertions: List<FactualAssertion>)

/**
 * Naming agent that generates names for a company or project.
 */
fun factCheckerAgent(
    llms: List<LlmOptions>,
) = agent(
    name = "FactChecker",
    description = "Check content for factual accuracy",
) {

    fun discernFactualAssertions(
        llm: LlmOptions,
        context: InputActionContext<UserInput>
    ): FactualAssertions {
        return context.promptRunner(llm = llm, toolGroups = setOf(ToolGroup.WEB)).createObject(
            """
            Given the following content, identify any factual assertions.
            Phrase them as standalone assertions.

            # Input
            ${context.input.content}
            """.trimIndent()
        )
    }

    flow {

//        referencedAgentAction<UserInput, ResearchReport>(agentName = Researcher::class.java.name)

        aggregate<UserInput, FactualAssertions, FactualAssertions>(
            transforms = llms.map { llm ->
                { context ->
                    discernFactualAssertions(llm, context)
                }
            },
            merge = { list, _ ->
                // TODO merge
                FactualAssertions(
                    factualAssertions = list.flatMap { it.factualAssertions }
                )
            },
        ).parallelize()
    }

    promptedTransformer<FactualAssertions, FactualAssertions>(
        name = "mergeAssertions",
    ) { context ->
        """
            Given the following factual assertions, check them for accuracy.
            Provide a list of factual assertions that are true or false.

            # Input
            ${context.input.factualAssertions.joinToString("\n") { "- " + it.standaloneAssertion }}
            """.trimIndent()
    }

    goal(
        name = "factCheckingDone",
        description = "Content was fact checked",
        satisfiedBy = FactualAssertions::class,
    )

}
