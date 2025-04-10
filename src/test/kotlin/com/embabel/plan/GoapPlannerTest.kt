/*
 * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.plan

import com.embabel.common.util.time
import com.embabel.plan.goap.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

object EmptyWorldStateDeterminer : WorldStateDeterminer {
    override fun determineWorldState(): WorldState {
        return WorldState()
    }

    override fun determineCondition(condition: String): ConditionDetermination =
        ConditionDetermination.UNKNOWN
}

class GoapPlannerTest {

    @Nested
    inner class Crime {

        val cookDrugs = GoapAction(
            name = "Cook drugs",
            preconditions = emptyMap(),
            effects = mapOf("hasDrugs" to ConditionDetermination(true), "legalPeril" to ConditionDetermination(true)),
            cost = 1.20
        )

        val sellDrugs = GoapAction(
            name = "Sell drugs",
            preconditions = mapOf("hasDrugs" to ConditionDetermination(true)),
            effects = mapOf(
                "hasDrugs" to ConditionDetermination(false),
                "hasMoney" to ConditionDetermination(true),
                "legalPeril" to ConditionDetermination(true)
            ),
            cost = 1.20,
        )

        val buyGun = GoapAction(
            name = "Buy gun",
            preconditions = mapOf("hasMoney" to ConditionDetermination(true)),
            effects = mapOf("hasGun" to ConditionDetermination(true), "hasMoney" to ConditionDetermination(false)),
            cost = 1.0,
        )

        val bribeCop = GoapAction(
            name = "Bribe cop",
            preconditions = mapOf("hasMoney" to ConditionDetermination(true)),
            effects = mapOf("legalPeril" to ConditionDetermination(false), "hasMoney" to ConditionDetermination(false)),
            cost = 2.0,
        )

        val shootEnemy = GoapAction(
            name = "Shoot enemy",
            preconditions = mapOf("hasGun" to ConditionDetermination(true)),
            effects = mapOf("enemyDead" to ConditionDetermination(true), "legalPeril" to ConditionDetermination(true)),
            cost = 1.0,
        )

        val buyPoison = GoapAction(
            name = "Buy poison",
            preconditions = mapOf("hasMoney" to ConditionDetermination(true)),
            effects = mapOf("hasPoison" to ConditionDetermination(true), "hasMoney" to ConditionDetermination(false)),
            cost = 3.0,
        )

        val poisonEnemy = GoapAction(
            name = "Poison enemy",
            preconditions = mapOf("hasPoison" to ConditionDetermination(true)),
            effects = mapOf("enemyDead" to ConditionDetermination(true), "legalPeril" to ConditionDetermination(true)),
            cost = 1.0
        )

        val getAwayWithMurderGoal =
            GoapGoal(
                name = "getAwayWithMurder",
                preconditions = mapOf(
                    "enemyDead" to ConditionDetermination(true),
                    "legalPeril" to ConditionDetermination(false)
                ),
                value = 10.0,
            )

        val actions = setOf(
            cookDrugs,
            buyGun,
            shootEnemy,
            bribeCop,
            sellDrugs,
            buyPoison,
            poisonEnemy,
        )


        @Test
        fun `single plan`() {
            val planner = AStarGoapPlanner(EmptyWorldStateDeterminer)
            val plan = planner.planToGoal(actions, getAwayWithMurderGoal)
            println(plan)
            println("Cost: ${plan?.cost}, net value = ${plan?.netValue}")
            assertTrue(plan != null)
        }

        @Test
        fun `should find 2 plans`() {
            val planner = AStarGoapPlanner(EmptyWorldStateDeterminer)
            val hasGunGoal = GoapGoal("hasGun", value = 1.0)
            val goapSystem = GoapSystem(actions, setOf(getAwayWithMurderGoal, hasGunGoal))
            val plans = planner.plansToGoals(goapSystem)
            assertEquals(plans.size, 2)
            println(plans.joinToString("\n") { it.infoString() })
        }

        @Test
        fun `cheapest plan to any goal`() {
            val planner = AStarGoapPlanner(EmptyWorldStateDeterminer)
            val hasGunGoal = GoapGoal(name = "hasGun", value = 1.0)
            val goapSystem = GoapSystem(actions, setOf(getAwayWithMurderGoal, hasGunGoal))
            val plan = planner.bestValuePlanToAnyGoal(goapSystem)
            assertNotNull(plan)
            println("Plan ${plan!!.infoString()}")
        }

        @Test
        fun `find path from unknown`() {
            val touchyWorldStateDeterminer = object : WorldStateDeterminer {
                override fun determineWorldState(): WorldState {
                    return WorldState(
                        state = mapOf(
                            "hasMoney" to ConditionDetermination(true),
                            "enemyDead" to ConditionDetermination.UNKNOWN,
                        )
                    )
                }

                override fun determineCondition(condition: String): ConditionDetermination =
                    ConditionDetermination.UNKNOWN
            }
            val planner = AStarGoapPlanner(touchyWorldStateDeterminer)
            val plan = planner.planToGoal(actions, getAwayWithMurderGoal)
            println(plan!!.infoString())
            println("Cost: ${plan.cost}, net value = ${plan.netValue}")
        }

        @Test
        fun `find path, force evaluating unknown`() {
            // We are going to have to ask for enemyDead to be evaluated
            // Will find that the goal has been achieved if we force evaluate that condition
            val forceEvaluated = mutableListOf<String>()
            val touchyWorldStateDeterminer = object : WorldStateDeterminer {

                override fun determineWorldState(): WorldState {
                    return WorldState(
                        state = mapOf(
                            "legalPeril" to ConditionDetermination.FALSE,
                            "enemyDead" to ConditionDetermination.UNKNOWN,
                        )
                    )
                }

                override fun determineCondition(condition: String): ConditionDetermination {
                    assertEquals(condition, "enemyDead")
                    forceEvaluated += condition
                    return ConditionDetermination.TRUE
                }
            }
            val planner = AStarGoapPlanner(touchyWorldStateDeterminer)
            val plan = planner.planToGoal(
                actions,
                getAwayWithMurderGoal
            )
            println(plan!!.infoString())
            println("Cost: ${plan.cost}, net value = ${plan.netValue}")
            assertEquals(0, plan.actions.size)
            assertTrue(forceEvaluated.contains("enemyDead"), "Should have force evaluated enemy dead")
        }

        private fun generateRandomActions(num: Int): List<GoapAction> {
            val random = Random()
            return List(num) {
                GoapAction(
                    name = random.nextInt().toString(),
                    preconditions = mapOf(
                        "hasGun" to ConditionDetermination(true),
                        "hasPoison" to ConditionDetermination(false),
                        UUID.randomUUID().toString() to ConditionDetermination(true)
                    ),
                    effects = mapOf(
                        "hasPoison" to ConditionDetermination(true),
                        UUID.randomUUID().toString() to ConditionDetermination(true),
                        UUID.randomUUID().toString() to ConditionDetermination(false)
                    ),
                    cost = 3.0,
                )
            }
        }

        @Test
        fun `scalability testing`() {
            val planner = AStarGoapPlanner(EmptyWorldStateDeterminer)
            val paddedActions = actions + generateRandomActions(300)
            val (plan, ms) = time {
                planner.planToGoal(paddedActions, getAwayWithMurderGoal)
            }
            assertTrue(plan != null)
            println("Formulated plan with ${paddedActions.size} actions in $ms ms: ${plan!!.infoString()}")
        }
    }

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
    @Nested
    inner class WithIrrelevantActions {

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

        val actions = setOf(
            toBeliever,
            findNewsStories,
            gpt4omini,
            claude35,
            reportMerger,
            ingestMarketableProduct,
        )

        val planner = AStarGoapPlanner(
            WorldStateDeterminer.fromMap(
                mapOf(
                    "userInput" to ConditionDetermination.TRUE,
                    "astrologyBeliever" to ConditionDetermination.FALSE,
                    "relevantNewsStories" to ConditionDetermination.FALSE,
                )
            )
        )

        @Test
        fun `should not be distracted by irrelevant actions running planToGoal`() {
            val plan = planner.planToGoal(actions, goal)
            assertNotNull(plan)
            assertEquals(
                listOf("toBeliever", "findNewsStories"),
                plan!!.actions.map { it.name }
            )
        }

        @Test
        fun `should not be distracted by irrelevant actions running plans`() {
            val plans = planner.plansToGoals(system = GoapSystem(actions, goal))
            val plan = plans.single()
            assertEquals(
                listOf("toBeliever", "findNewsStories"),
                plan.actions.map { it.name }
            )
        }
    }


}
