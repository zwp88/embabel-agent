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
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.ResearchReport
import com.embabel.common.ai.model.LlmOptions
import com.embabel.examples.dogfood.research.Researcher
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

data class GeneratedName(
    val name: String,
    @JsonPropertyDescription("the essential domain name, like 'embabel.com'")
    val domain: String,
    val reason: String,
)

data class GeneratedNames(val names: List<GeneratedName>)
data class AllNames(val accepted: List<GeneratedName>, val rejected: List<GeneratedName>)

@ConfigurationProperties("embabel.examples.namer")
data class NamingProperties(
    val namesRequired: Int = 10,
)

@Configuration
@Profile("!test")
class NamerAgentConfiguration {
    @Bean
    fun namingAgent(
        domainChecker: DomainChecker,
        namingProperties: NamingProperties,
    ): Agent {
        return simpleNamingAgent(
            llms = listOf(
                LlmOptions(OpenAiModels.GPT_41_MINI).withTemperature(.3),
                LlmOptions(OpenAiModels.GPT_41_MINI).withTemperature(1.3),
                LlmOptions(AnthropicModels.CLAUDE_35_HAIKU).withTemperature(.9),
                LlmOptions(AnthropicModels.CLAUDE_35_HAIKU).withTemperature(.8),
                LlmOptions("ai/llama3.2").withTemperature(.3),
            ),
            domainChecker = domainChecker,
            namingProperties = namingProperties,
        )
    }
}

/**
 * Naming agent that generates names for a company or project.
 */
fun simpleNamingAgent(
    llms: List<LlmOptions>,
    domainChecker: DomainChecker,
    namingProperties: NamingProperties,
) = agent(
    name = "CompanyNamer",
    description = "Name a company or project, using internet research"
) {

    fun generateNamesWith(llm: LlmOptions, context: BiInputActionContext<UserInput, ResearchReport>): GeneratedNames {
        return context.promptRunner(
            llm = llm,
            toolGroups = setOf(
                CoreToolGroups.WEB
            ),
            toolObjects = listOf(domainChecker),
        ).createObject(
            """
            Generate a list of ${namingProperties.namesRequired} names for a company or project, based on the following input.
            Each name return must include a primary domain name, which would be essential
            to the company.
            Consider the following research report:
            ${context.input2.content}

            # Input
            ${context.input1.content}
            """.trimIndent()
        )
    }

    flow {

        referencedAgentAction<UserInput, ResearchReport>(agentName = Researcher::class.java.simpleName)

        biAggregate<UserInput, ResearchReport, GeneratedNames, AllNames>(
            transforms = llms.map { llm ->
                { context ->
                    generateNamesWith(llm, context)
                }
            },
            merge = { generatedNamesList ->
                val accepted = generatedNamesList.flatMap { it.names }
                    .distinctBy { it.name }
                    .filter { domainChecker.isDomainAvailable(it.domain) }
                val rejected = generatedNamesList.flatMap { it.names }
                    .distinctBy { it.name }
                    .filterNot { domainChecker.isDomainAvailable(it.domain) }
                AllNames(
                    accepted = accepted,
                    rejected = rejected,
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
