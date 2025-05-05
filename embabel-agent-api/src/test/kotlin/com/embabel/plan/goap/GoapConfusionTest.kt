package com.embabel.plan.goap

import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.domain.library.PersonImpl
import com.embabel.agent.domain.special.UserInput
import com.embabel.examples.simple.horoscope.kotlin.StarNewsFinder
import com.embabel.plan.Goal
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Error prompting this test:
 *
 * WILES: Created agent Agent goal-horoscopeNewsFinder
 * 	goals:
 * 		horoscopeNewsFinder - pre={it:RelevantNewsStories=TRUE} value=0.0
 * 	actions:
 * 		toBeliever - pre={it:UserInput=TRUE, it:AstrologyBeliever=FALSE} post={it:AstrologyBeliever=TRUE}
 * 		findNewsStories - pre={it:AstrologyBeliever=TRUE, it:RelevantNewsStories=FALSE} post={it:RelevantNewsStories=TRUE}
 * 		gpt-4o-mini-researcher - pre={it:MarketableProduct=TRUE} post={enoughReports=TRUE}
 * 		claude-3-5-haiku-latest-researcher - pre={it:MarketableProduct=TRUE} post={enoughReports=TRUE}
 * 		reportMerger - pre={enoughReports=TRUE} post={finalReport:MarketResearchReport=TRUE}
 * 		ingest-MarketableProduct - pre={it:UserInput=TRUE, it:MarketableProduct=FALSE} post={it:MarketableProduct=TRUE}
 * 	conditions: [enoughReports]
 * 	WILES: formulated plan <ingest-MarketableProduct -> claude-3-5-haiku-latest-researcher -> reportMerger -> toBeliever -> findNewsStories; netValue=0.0; worldState=WorldState(state={it:UserInput=TRUE, it:AstrologyBeliever=FALSE, it:RelevantNewsStories=FALSE, it:MarketableProduct=FALSE, enoughReports=FALSE, finalReport:MarketResearchReport=FALSE})> from {it:UserInput=TRUE, it:AstrologyBeliever=FALSE, it:RelevantNewsStories=FALSE, it:MarketableProduct=FALSE, enoughReports=FALSE, finalReport:MarketResearchReport=FALSE} 2025-04-08 10:08:06.968  INFO 53692 --- [restartedMain] l.p.SeveranceLoggingAgenticEventListener.onProcessEvent :
 */
@DisplayName("Test irrelevant actions don't confuse GOAP planner")
class IrrelevantActionsTest {

    val goal = GoapGoal(
        name = "horoscopeNewsFinder",
        pre = listOf("relevantNewsStories")
    )

    val toBeliever = GoapAction(
        name = "toBeliever",
        preconditions = mapOf(
            "userInput" to ConditionDetermination.TRUE,
            "astrologyBeliever" to ConditionDetermination.FALSE,
        ),
        post = setOf("astrologyBeliever")
    )

    val findNewsStories = GoapAction(
        name = "findNewsStories",
        preconditions = mapOf(
            "astrologyBeliever" to ConditionDetermination.TRUE,
            "relevantNewsStories" to ConditionDetermination.FALSE
        ),
        effects = mapOf("relevantNewsStories" to ConditionDetermination.TRUE),
    )

    // Now the irrelevant actions
    val gpt4omini = GoapAction(
        name = "gpt-4o-mini-researcher",
        pre = setOf("marketableProduct"),
        post = setOf("enoughReports")
    )
    val claude35 = GoapAction(
        name = "claude-3-5-haiku-latest-researcher",
        pre = setOf("marketableProduct"),
        post = setOf("enoughReports")
    )
    val reportMerger = GoapAction(
        name = "reportMerger",
        pre = setOf("enoughReports"),
        post = setOf("finalReport:MarketResearchReport")
    )
    val ingestMarketableProduct = GoapAction(
        name = "ingest-MarketableProduct",
        pre = setOf("userInput"),
        post = setOf("marketableProduct")
    )

    val mixedActions = setOf(
        toBeliever,
        findNewsStories,
        gpt4omini,
        claude35,
        reportMerger,
        ingestMarketableProduct,
    )

    @Test
    fun `should distinguish relevant conditions`() {
        val planner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(
                mapOf(
                    "userInput" to ConditionDetermination.TRUE,
                    "astrologyBeliever" to ConditionDetermination.FALSE,
                    "relevantNewsStories" to ConditionDetermination.FALSE,
                )
            )
        )

        val goapSystem = GoapPlanningSystem(mixedActions, goal)
        val pruned = planner.prune(goapSystem)
        assertEquals(
            setOf("toBeliever", "findNewsStories"),
            pruned.actions.map { it.name }.toSet()
        )
    }

    @Test
    fun `should not be distracted by irrelevant actions running planToGoal`() {
        val planner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(
                mapOf(
                    "userInput" to ConditionDetermination.TRUE,
                    "astrologyBeliever" to ConditionDetermination.FALSE,
                    "relevantNewsStories" to ConditionDetermination.FALSE,
                )
            )
        )

        val plan = planner.planToGoal(mixedActions, goal)
        assertNotNull(plan)
        assertEquals(
            listOf("toBeliever", "findNewsStories"),
            plan!!.actions.map { it.name }
        )
    }

    @Test
    fun `should not be distracted by irrelevant actions running plans`() {
        val planner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(
                mapOf(
                    "userInput" to ConditionDetermination.TRUE,
                    "astrologyBeliever" to ConditionDetermination.FALSE,
                    "relevantNewsStories" to ConditionDetermination.FALSE,
                )
            )
        )

        val plans = planner.plansToGoals(system = GoapPlanningSystem(mixedActions, goal))
        val plan = plans.single()
        assertEquals(
            listOf("toBeliever", "findNewsStories"),
            plan.actions.map { it.name }
        )
    }

    @Test
    fun `realistic irrelevant conditions representing agent platform`() {
        val starFinderAgent =
            AgentMetadataReader().createAgentMetadata(StarNewsFinder(mockk(), 5))!! as com.embabel.agent.core.Agent
        val confusionAgent =
            AgentMetadataReader().createAgentMetadata(CreateConfusionAgent())!! as com.embabel.agent.core.Agent
        starFinderWithIrrelevantActions(starFinderAgent.goals.single(), starFinderAgent, confusionAgent)
    }

    @Test
    fun `even more realistic irrelevant conditions representing agent platform`() {
        val starFinderAgent =
            AgentMetadataReader().createAgentMetadata(StarNewsFinder(mockk(), 5))!! as com.embabel.agent.core.Agent
        val confusionAgent =
            AgentMetadataReader().createAgentMetadata(CreateConfusionAgent())!! as com.embabel.agent.core.Agent
        val moreConfusionAgent =
            AgentMetadataReader().createAgentMetadata(MoreConfusionAgent())!! as com.embabel.agent.core.Agent
        starFinderWithIrrelevantActions(
            starFinderAgent.goals.single(),
            starFinderAgent,
            confusionAgent,
            moreConfusionAgent
        )
    }

    private fun starFinderWithIrrelevantActions(goal: Goal, vararg agents: com.embabel.agent.core.Agent) {
        val noisyWorldState = mutableMapOf<String, ConditionDetermination>()
        agents.forEach { agent ->
            noisyWorldState += agent.planningSystem.knownConditions()
                .associate { it to ConditionDetermination.FALSE } +
                    agent.planningSystem.knownConditions()
                        .associate { it to ConditionDetermination.FALSE }
        }
        noisyWorldState["it:com.embabel.agent.domain.special.UserInput"] = ConditionDetermination.TRUE
        val distractedPlanner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(noisyWorldState)
        )

        val plan = distractedPlanner.planToGoal(agents.flatMap { it.actions }, goal)
        assertNotNull(plan, "Should have found a plan")
        assertEquals(
            listOf("extractStarPerson", "retrieveHoroscope", "findNewsStories", "writeup"),
            plan!!.actions.map { it.name.split(".").last() }
        )
    }
}


data class Confusion1(val lots: Boolean)
data class Confusion2(val lots: Boolean)

data class Confusion3(val lots: Boolean)

data class Confusion4(val lots: Boolean)
data class Confusion5(val lots: Boolean)

data class Confusion6(val lots: Boolean)

data class Confusion7(val lots: Boolean)

data class Confusion8(val lots: Boolean)
data class Confusion9(val lots: Boolean)
data class Confusion10(val lots: Boolean)


@Agent(description = "test", scan = false)
class CreateConfusionAgent {
    @com.embabel.agent.api.annotation.Action
    fun createConfusion(userInput: UserInput): Confusion1 {
        return Confusion1(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion2(confusion1: Confusion1): Confusion2 {
        return Confusion2(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion3(confusion2: Confusion2): Confusion3 {
        return Confusion3(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion4(confusion3: Confusion3): Confusion4 {
        return Confusion4(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion5(confusion4: Confusion4): Confusion5 {
        return Confusion5(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion6(confusion5: Confusion5): Confusion6 {
        return Confusion6(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion7(confusion6: Confusion6): Confusion7 {
        return Confusion7(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion8(confusion7: Confusion7): Confusion8 {
        return Confusion8(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion9(confusion8: Confusion8, person: PersonImpl): Confusion9 {
        return Confusion9(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun createConfusion10(confusion9: Confusion9, userInput: UserInput): Confusion10 {
        return Confusion10(true)
    }

    @com.embabel.agent.api.annotation.Action
    @com.embabel.agent.api.annotation.AchievesGoal(description = "Confusion!")
    fun utterConfusion(confusion10: Confusion10, confusion2: Confusion2, userInput: UserInput): String {
        return "Confusion!"
    }
}

data class OtherThing(val lots: Boolean = true)
data class MoreOtherThing(val lots: Boolean = true)
data class MoreConfusion(val lots: Boolean = true)
data class MoreConfusion2(val lots: Boolean = true)
data class MoreConfusion3(val lots: Boolean = true)
data class MoreConfusion4(val lots: Boolean = true)
data class MoreConfusion5(val lots: Boolean = true)
data class MoreConfusion6(val lots: Boolean = true)
data class MoreConfusion7(val lots: Boolean = true)
data class MoreConfusion8(val lots: Boolean = true)

@com.embabel.agent.api.annotation.Agent(description = "confuse the planner, please", scan = false)
class MoreConfusionAgent {

    @com.embabel.agent.api.annotation.Action
    fun createAnOtherThing(userInput: UserInput): OtherThing {
        return OtherThing(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun tomother(otherThing: OtherThing): MoreOtherThing {
        return MoreOtherThing(true)
    }

    @com.embabel.agent.api.annotation.Action
    fun toMoConfusion(moreOtherThing: MoreOtherThing): MoreConfusion {
        return MoreConfusion(true)
    }

    @com.embabel.agent.api.annotation.Action
    @com.embabel.agent.api.annotation.AchievesGoal(description = "Trying to confuse the planner")
    fun ohyeahBabyMoreAndMore(moreConfusion: MoreConfusion): MoreConfusion2 {
        return MoreConfusion2(true)
    }
}

