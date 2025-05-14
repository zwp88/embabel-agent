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
package com.embabel.examples.dogfood.namer

import com.embabel.agent.api.common.createObject
import com.embabel.agent.api.dsl.BiInputActionContext
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.biAggregate
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.Agent
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.ResearchReport
import com.embabel.agent.toolgroups.web.domain.DomainChecker
import com.embabel.common.ai.model.LlmOptions
import com.embabel.examples.dogfood.research.Researcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

data class GeneratedName(val name: String, val reason: String)
data class GeneratedNames(val names: List<GeneratedName>)
data class AllNames(val accepted: List<GeneratedName>, val rejected: List<GeneratedName>)

@Configuration
@Profile("!test")
class NamerAgentConfiguration {
    @Bean
    fun namingAgent(domainChecker: DomainChecker): Agent {
        return simpleNamingAgent(
            llms = listOf(
                LlmOptions(OpenAiModels.GPT_41_MINI).withTemperature(.3),
                LlmOptions(OpenAiModels.GPT_41_MINI).withTemperature(1.3),
                LlmOptions(AnthropicModels.CLAUDE_35_HAIKU).withTemperature(.9),
                LlmOptions(AnthropicModels.CLAUDE_35_HAIKU).withTemperature(.8),
                LlmOptions("ai/llama3.2").withTemperature(.3),
            ),
            domainChecker = domainChecker,
        )
    }
}

/**
 * Naming agent that generates names for a company or project.
 */
fun simpleNamingAgent(
    llms: List<LlmOptions>,
    domainChecker: DomainChecker,
) = agent(
    name = "CompanyNamer",
    description = "Name a company or project, using internet research"
) {

    fun generateNamesWith(llm: LlmOptions, context: BiInputActionContext<UserInput, ResearchReport>): GeneratedNames {
        return context.promptRunner(llm = llm, toolGroups = setOf(ToolGroup.WEB)).createObject(
            """
            Generate a list of names for a company or project, based on the following input.
            Consider the following research report:
            ${context.input2.text}

            # Input
            ${context.input1.content}
            """.trimIndent()
        )
    }

    flow {

        referencedAgentAction<UserInput, ResearchReport>(agentName = Researcher::class.java.name)

        biAggregate<UserInput, ResearchReport, GeneratedNames, AllNames>(
            transforms = llms.map { llm ->
                { context ->
                    generateNamesWith(llm, context)
                }
            },
            merge = { generatedNamesList ->
                AllNames(
                    accepted = generatedNamesList.flatMap { it.names }.distinctBy { it.name },
                    rejected = emptyList()
                )
            },
        ).parallelize()
    }

    goal(
        name = "namingDone",
        description = "Names were generated",
        satisfiedBy = AllNames::class,
    )


}
