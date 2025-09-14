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

import com.embabel.common.core.types.HasInfoString
import com.embabel.common.core.types.Timestamped

/**
 * A planning system is a set of actions and goals.
 */
interface PlanningSystem : HasInfoString {

    val actions: Set<Action>

    val goals: Set<Goal>
}

/**
 * Tag interface for WorldState
 * Different planners have different world state.
 */
interface WorldState : HasInfoString, Timestamped

/**
 * A planner is a system that can plan from a set of actions to a set of goals.
 * A planner should have a way of determining present state, such as
 * the GOAP WorldStateDeterminer. The representation of state
 * can differ between planners.
 */
interface Planner<S : PlanningSystem, W : WorldState, P : Plan> {

    /**
     * Current world state
     */
    fun worldState(): W

    /**
     * Plan from here to the given goal
     */
    fun planToGoal(
        actions: Collection<Action>,
        goal: Goal,
    ): P?

    /**
     * Return the best plan to each goal from the present world state.
     * The plans (one for each goal) are sorted by net value, descending.
     */
    fun plansToGoals(system: S): List<P> =
        system.goals.mapNotNull { goal ->
            planToGoal(system.actions, goal)
        }.sortedByDescending { p -> p.netValue }

    /**
     * Return the best plan to any goal
     */
    fun bestValuePlanToAnyGoal(system: S): P? =
        plansToGoals(system).firstOrNull()

    /**
     * Return a PlanningSystem that excludes all actions that cannot
     * help achieve one of the goals from the present world state.
     */
    fun prune(planningSystem: S): S

}
