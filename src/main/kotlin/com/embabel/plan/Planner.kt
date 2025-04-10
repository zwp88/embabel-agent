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
package com.embabel.plan

/**
 * A planning system is a set of actions and goals.
 */
interface PlanningSystem {

    val actions: Set<Action>

    val goals: Set<Goal>
}

/**
 * A planner is a system that can plan from a set of actions to a set of goals.
 * A planner should have a way of determining present state, such as
 * the GOAP WorldStateDeterminer. The representation of state
 * can differ between planners.
 */
interface Planner {

    /**
     * Plan from here to the given goal
     */
    fun planToGoal(actions: Collection<Action>, goal: Goal): Plan?

    /**
     * Return the best plan to each goal
     */
    fun plansToGoals(system: PlanningSystem): List<Plan> {
        val plans = mutableListOf<Plan>()
        for (goal in system.goals) {
            val plan = planToGoal(system.actions, goal)
            if (plan != null) {
                plans.add(plan)
            }
        }
        return plans.sortedBy { p -> -p.netValue }
    }

    /**
     * Return the best plan to any goal
     */
    fun bestValuePlanToAnyGoal(system: PlanningSystem): Plan? =
        plansToGoals(system).firstOrNull()
}
