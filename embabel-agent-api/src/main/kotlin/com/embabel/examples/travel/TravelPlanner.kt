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
package com.embabel.examples.travel

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.experimental.prompt.Persona
import com.embabel.agent.shell.markdownToConsole
import com.embabel.common.ai.model.LlmOptions
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

data class TravelBrief(
    val areaToExplore: String,
    val stayingAt: String,
    val brief: String,
    val dates: String,
)

data class PointOfInterest(
    val name: String,
    val description: String,
    val location: String,
)

data class ItineraryIdeas(
    val pointsOfInterest: List<PointOfInterest>,
)

val TravelPlannerPersona = Persona(
    name = "Hermes",
    persona = "You are an expert travel planner",
    voice = "friendly and concise",
    objective = "Make a detailed travel plan meeting requirements",
)

class TravelPlan(
    val plan: String,
)

@Agent(description = "Make a detailed travel plan")
class TravelPlanner(
    val persona: Persona = TravelPlannerPersona,
    val wordCount: Int = 500,
) {

    @Action
    fun findPointsOfInterest(
        travelBrief: TravelBrief,
    ): ItineraryIdeas {
        return using(toolGroups = setOf(CoreToolGroups.WEB, CoreToolGroups.MAPS))
            .withPromptContributor(persona)
            .create(
                prompt = """
                Consider the following travel brief.
                ${travelBrief.brief}
                The travelers want to explore ${travelBrief.areaToExplore} and are staying at ${travelBrief.stayingAt}.
                Find points of interest in ${travelBrief.areaToExplore} that are relevant to the travel brief.

            """.trimIndent(),
            )
    }

    @AchievesGoal(
        description = "Create a detailed travel plan based on the travel brief and itinerary ideas",
    )
    @Action
    fun createTravelPlan(
        travelBrief: TravelBrief,
        itineraryIdeas: ItineraryIdeas,
    ): TravelPlan {
        return using(
            LlmOptions(AnthropicModels.CLAUDE_37_SONNET),
            toolGroups = setOf(CoreToolGroups.WEB, CoreToolGroups.MAPS)
        )
            .withPromptContributor(persona)
            .create<TravelPlan>(
                prompt = """
                Given the following travel brief, create a detailed activity plan.
                ${travelBrief.brief}
                The travelers want to explore ${travelBrief.areaToExplore} and are staying at ${travelBrief.stayingAt}.
                They need activities on ${travelBrief.dates}.
                Consider the weather in your recommendations. Use mapping tools to consider distance of driving or walking.
                Write up in $wordCount words or less.
                Include links.

                Consider the following itinerary ideas:
                ${itineraryIdeas.pointsOfInterest.joinToString("\n") { it.description }}

                Create a markup plan.
            """.trimIndent(),
            )
    }
}

@ShellComponent("Travel planner commands")
class TravelPlannerShell(
    private val agentPlatform: AgentPlatform,
) {
    @ShellMethod
    fun planTravel() {
        val travelBrief = TravelBrief(
            areaToExplore = "Paris",
            stayingAt = "La Fantasie Hotel in Montmartre",
            dates = "May 18 2025",
            brief = """
                The travelers are interested in history, art, food
                and classical music.
                They love walking
            """.trimIndent(),
        )

        val ap = agentPlatform.runAgentWithInput(
            agent = agentPlatform.agents().single({ it.name == "TravelPlanner" }),
            input = travelBrief,
        )
        val travelPlan = ap.lastResult() as TravelPlan

        println("Travel Plan: ${markdownToConsole(travelPlan.plan)}")
    }
}
