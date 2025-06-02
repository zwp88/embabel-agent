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
package com.embabel.agent.validation

import org.springframework.stereotype.Component
import com.embabel.agent.core.AgentScope
import com.embabel.plan.goap.AStarGoapPlanner
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.GoapAction
import com.embabel.plan.goap.GoapGoal
import com.embabel.plan.goap.WorldStateDeterminer

/**
 * Validator that checks whether an agent definition has at least one possible path
 * from its initial conditions, through available actions, to achieve its defined goals.
 *
 * Uses the GOAP planner to validate that goals can be achieved through a sequence of actions.
 * Reports specific errors when no such path exists.
 */

@Component
class GoapPathToCompletionValidator : AgentValidator {

    override fun validate(agentScope: AgentScope): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (agentScope.goals.isEmpty()) {
            return ValidationResult(true, emptyList())
        }

        if (agentScope.actions.isEmpty()) {
            errors.add(error("NO_ACTIONS_TO_GOALS", "Agent '${agentScope.name}' has no actions.", agentScope))
            return ValidationResult(false, errors)
        }

        // Create initial world state
        val initialWorldState = mutableMapOf<String, ConditionDetermination>()

        val actionDependencies = mutableMapOf<String, Set<String>>()
        val actionOutputs = mutableMapOf<String, Set<String>>()

        // For each action, need to track:
        // 1. What data it needs as input (preconditions)
        // 2. What data it produces as output (effects)
        // So need to exclude hasRun_ conditions as they are runtime state indicators, not data dependencies.
        agentScope.actions.forEach { action ->
            val nonHasRunPreconditions = action.preconditions.filterKeys { !it.startsWith("hasRun_") }.keys
            val nonHasRunEffects = action.effects.filterKeys { !it.startsWith("hasRun_") }.keys
            
            actionDependencies[action.name] = nonHasRunPreconditions
            actionOutputs[action.name] = nonHasRunEffects
        }

        // Find entry point actions (those that can start a valid chain)
        val entryPointActions = agentScope.actions.filter { action ->
            val deps = actionDependencies[action.name] ?: emptySet()
            // An action is an entry point if:
            // 1. It has at least one precondition that is not produced by any other action
            // 2. That precondition is not FALSE (which means it's an output of this action)
            // 3. All its preconditions are either external inputs or FALSE (self-produced)
            deps.any { dep ->
                !actionOutputs.values.any { outputs -> outputs.contains(dep) } &&
                action.preconditions[dep] != ConditionDetermination.FALSE
            } && deps.all { dep ->
                // All preconditions must be either:
                // - Not produced by any action (external input)
                // - FALSE (self-produced)
                !actionOutputs.values.any { outputs -> outputs.contains(dep) } ||
                action.preconditions[dep] == ConditionDetermination.FALSE
            }
        }

        // Find actions that don't depend on other actions' outputs
        val firstActions = agentScope.actions.filter { action ->
            val deps = actionDependencies[action.name] ?: emptySet()
            // An action is a first action if:
            // 1. It's an entry point action, OR
            // 2. Its dependencies are not produced by any other action
            action in entryPointActions || deps.all { dep ->
                !actionOutputs.values.any { outputs -> outputs.contains(dep) }
            }
        }

        if (firstActions.isEmpty()) {
            errors.add(error(
                "NO_STARTING_ACTION",
                "No action found that can start the chain. All actions depend on outputs from other actions.",
                agentScope
            ))
            return ValidationResult(false, errors)
        }

        // Set initial conditions based on first actions
        firstActions.forEach { action ->
            action.preconditions.forEach { (key, value) ->
                if (!key.startsWith("hasRun_")) {
                    initialWorldState[key] = value
                }
            }
        }

        // Add all action effects to the initial state as FALSE
        agentScope.actions.forEach { action ->
            action.effects.forEach { (key, _) ->
                if (!key.startsWith("hasRun_") && key !in initialWorldState) {
                    initialWorldState[key] = ConditionDetermination.FALSE
                }
            }
        }

        // Create planner with the determined world state
        val planner = AStarGoapPlanner(WorldStateDeterminer.fromMap(initialWorldState))

        // Convert agent actions to GOAP actions, removing hasRun_ conditions
        val goapActions = agentScope.actions.map { action ->
            GoapAction(
                name = action.name,
                preconditions = action.preconditions.filterKeys { !it.startsWith("hasRun_") },
                effects = action.effects.filterKeys { !it.startsWith("hasRun_") },
                cost = action.cost,
                value = action.value
            )
        }.toSet()

        // Check each goal
        var allGoalsAchievable = true
        for (goal in agentScope.goals) {
            // Create goal with original preconditions, removing hasRun_ conditions
            val goapGoal = GoapGoal(
                name = goal.name,
                preconditions = goal.preconditions.filterKeys { !it.startsWith("hasRun_") },
                value = goal.value
            )

            // Try to find a plan to this goal
            val plan = planner.planToGoal(goapActions, goapGoal)

            if (plan == null || plan.actions.isEmpty()) {
                allGoalsAchievable = false
                break
            }
        }

        if (!allGoalsAchievable) {
            errors.add(error(
                "NO_PATH_TO_GOAL",
                "No valid path found to achieve goals. " +
                        "Either no plan exists or the goals' preconditions cannot be achieved through available actions.",
                agentScope
            ))
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun error(code: String, message: String, agentScope: AgentScope): ValidationError =
        ValidationError(
            code = code,
            message = message,
            severity = ValidationSeverity.ERROR,
            location = ValidationLocation(
                type = "Agent",
                name = agentScope.name,
                agentName = agentScope.name,
                component = agentScope.name
            )
        )
}
