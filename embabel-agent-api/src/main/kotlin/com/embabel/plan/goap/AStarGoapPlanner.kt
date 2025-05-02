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
package com.embabel.plan.goap

import java.util.*

/**
 * Implements a Goal-Oriented Action Planning system using the A* algorithm.
 * A* works by finding the optimal sequence of actions to transform an initial state into a goal state
 * while minimizing total cost.
 * See https://en.wikipedia.org/wiki/A*_search_algorithm
 *
 * The algorithm works as follows:
 * 1. Start with the initial world state
 * 2. Maintain an open list (priority queue) of states to explore, prioritized by f-score
 * 3. For each state, explore all achievable actions, calculating:
 *    - g-score: The cost accumulated so far to reach this state
 *    - h-score: A heuristic estimate of the remaining cost to reach the goal
 *    - f-score: g-score + h-score (total estimated cost)
 * 4. Always expand the state with the lowest f-score first
 * 5. Track visited states to avoid cycles and redundant exploration
 * 6. Continue until finding a state that satisfies the goal conditions
 *
 * The implementation ensures finding the optimal (lowest cost) sequence of actions
 * by properly tracking path costs and using an admissible heuristic function.
 */
class AStarGoapPlanner(worldStateDeterminer: WorldStateDeterminer) :
    OptimizingGoapPlanner(worldStateDeterminer) {

    override fun planToGoalFrom(
        startState: GoapWorldState,
        actions: Collection<GoapAction>,
        goal: GoapGoal,
    ): GoapPlan? {
        val openList = PriorityQueue<SearchNode>()
        val closedList = mutableSetOf<GoapWorldState>()
        val gScores = mutableMapOf<GoapWorldState, Double>().withDefault { Double.MAX_VALUE }

        // Initialize with start node
        gScores[startState] = 0.0
        openList.add(SearchNode(startState, null, null, 0.0, heuristic(startState, goal)))

        while (openList.isNotEmpty()) {
            val currentNode = openList.poll()

            // Goal reached
            if (goal.isAchievable(currentNode.state)) {
                return GoapPlan(constructPlan(currentNode), goal, worldState = startState)
            }

            // Skip if we've already processed this state
            if (currentNode.state in closedList) {
                continue
            }

            closedList.add(currentNode.state)

            // Try each possible action
            for (action in actions) {
                if (action.isAchievable(currentNode.state)) {
                    val newState = applyAction(currentNode.state, action)
                    val tentativeGScore = gScores.getValue(currentNode.state) + action.cost

                    // Only consider this path if it's better than any previous path to this state
                    if (tentativeGScore < gScores.getValue(newState)) {
                        gScores[newState] = tentativeGScore
                        val newNode = SearchNode(
                            newState,
                            currentNode,
                            action,
                            tentativeGScore,
                            heuristic(newState, goal)
                        )
                        openList.add(newNode)
                    }
                }
            }
        }
        return null
    }

    private fun heuristic(state: GoapWorldState, goal: GoapGoal): Double {
        // Convert to double for more accurate comparisons
        return goal.preconditions.count { (key, value) -> state.state[key] != value }.toDouble()
    }

    private fun applyAction(currentState: GoapWorldState, action: GoapAction): GoapWorldState {
        val newState = currentState.state.toMutableMap()
        action.effects.forEach { (key, value) ->
            newState[key] = value
        }
        return GoapWorldState(newState as HashMap<String, ConditionDetermination>)
    }

    private fun constructPlan(node: SearchNode): List<GoapAction> {
        val plan = mutableListOf<GoapAction>()
        var currentNode: SearchNode? = node
        while (currentNode?.action != null) {
            plan.add(currentNode.action)
            currentNode = currentNode.parent
        }
        return plan.reversed()
    }
}

private class SearchNode(
    val state: GoapWorldState,
    val parent: SearchNode?,
    val action: GoapAction?,
    val cost: Double,
    val heuristic: Double
) : Comparable<SearchNode> {
    override fun compareTo(other: SearchNode): Int {
        val f1 = cost + heuristic
        val f2 = other.cost + other.heuristic
        return when {
            f1 < f2 -> -1
            f1 > f2 -> 1
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchNode) return false
        return state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}
