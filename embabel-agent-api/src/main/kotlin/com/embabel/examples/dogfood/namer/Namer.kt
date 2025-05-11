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

import com.embabel.agent.api.common.InputActionContext
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.aggregate
import com.embabel.agent.core.Agent
import com.embabel.agent.domain.special.UserInput
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

data class GeneratedName(val name: String, val reason: String)
data class GeneratedNames(val names: List<GeneratedName>)
data class AllNames(val accepted: List<GeneratedName>, val rejected: List<GeneratedName>)

@Configuration
class NamerAgentConfiguration {
    @Bean
    fun namingAgent(): Agent {
        return simpleNamingAgent()
    }
}


fun simpleNamingAgent() = agent(
    "Company namer",
    description = "Name a company or project, using internet research"
) {

    fun generateNames(context: InputActionContext<UserInput>): GeneratedNames {
        return GeneratedNames(
            names = listOf(GeneratedName("musicology.com", "Sounds good")),
        )
    }

    actions {
        aggregate<UserInput, GeneratedNames, AllNames>(
            transforms = listOf(
                ::generateNames,
                { GeneratedNames(names = listOf(GeneratedName("money.com", "Helps make money"))) }),
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
