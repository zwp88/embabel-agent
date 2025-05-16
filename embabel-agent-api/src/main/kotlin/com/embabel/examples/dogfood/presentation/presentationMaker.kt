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
package com.embabel.examples.dogfood.presentation

import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.doSplit
import com.embabel.agent.api.dsl.parallelMap
import com.embabel.agent.core.Agent
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.ResearchReport
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

// TODO make common
data class ResearchTopic(
    val topic: String,
    val questions: List<String>,
)

data class TopicResearch(
    val topic: ResearchTopic,
    val researchReport: ResearchReport,
)

data class Deck(
    val deck: String,
)

@ConfigurationProperties("embabel.presentation-maker")
data class PresentationMakerProperties(
    val slideCount: Int = 30,
)

@Configuration
@Profile("!test")
class PresentationMakerConfiguration {
    @Bean
    fun presentationMaker(props: PresentationMakerProperties): Agent {
        return presentationMakerAgent(
            llm = LlmOptions(ModelSelectionCriteria.Auto),
            properties = props,
        )
    }
}


/**
 * Naming agent that generates names for a company or project.
 */
fun presentationMakerAgent(
    llm: LlmOptions,
    properties: PresentationMakerProperties,
) = agent(
    name = "PresentationMaker",
    description = "Build a presentation",
) {

    flow {

        doSplit<UserInput, ResearchTopic> {
            listOf(
                ResearchTopic(
                    topic = "AI in healthcare",
                    questions = listOf(
                        "What are the current applications of AI in healthcare?",
                        "How is AI improving patient outcomes?",
                        "What are the ethical considerations of using AI in healthcare?",
                    ),
                )
            )
        } andThenDo {
            val topics = it.objects.filterIsInstance<ResearchTopic>()
            topics.parallelMap(it) {
                TopicResearch(
                    topic = it, researchReport = ResearchReport(
                        text = "foo",
                        links = emptyList(),
                    )
                )
            }
        } andThenDo {
            Deck("content")
        }
    }

    goal(
        name = "deckProduced",
        description = "Slide deck was produced",
        satisfiedBy = Deck::class,
    )

}
