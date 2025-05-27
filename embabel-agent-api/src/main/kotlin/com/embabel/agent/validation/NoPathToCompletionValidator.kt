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
import org.slf4j.LoggerFactory

/**
 * Validator that checks whether an agent definition has at least one possible path
 * from its initial conditions, through available actions, to achieve its defined goals.
 *
 * This includes:
 * - Verifying that actions exist for agents with goals.
 * - Ensuring there are actions whose effects can achieve goals.
 * - Propagating triggerability to confirm that, starting from initial conditions,
 *   it is possible to reach at least one goal via a valid chain of actions.
 *
 * Reports specific errors when no such path exists.
 */
@Component
class NoPathToCompletionValidator : AgentValidator {
    private val logger = LoggerFactory.getLogger(NoPathToCompletionValidator::class.java)

    override fun validate(agentScope: AgentScope): ValidationResult {
        // No goals, validation OK
        if (agentScope.goals.isEmpty()) return ValidationResult(true, emptyList())

        // No actions, error
        if (agentScope.actions.isEmpty()) {
            return errorResult(
                code = "NO_ACTIONS_TO_GOALS",
                message = "Agent '${agentScope.name}' has goals but no actions to achieve them",
                agentScope = agentScope
            )
        }

        val actionsLeadingToGoals = findActionsLeadingToGoals(agentScope)
        if (actionsLeadingToGoals.isEmpty()) {
            return errorResult(
                code = "NO_PATH_TO_GOALS",
                message = "Agent '${agentScope.name}' has no actions that can lead to its goals",
                agentScope = agentScope
            )
        }

        // Precondition validation
        val preconditionErrors = findMissingPreconditionErrors(agentScope)
        if (preconditionErrors.isNotEmpty()) {
            return ValidationResult(false, preconditionErrors)
        }

        // Find triggerable actions
        val triggerableActions = findTriggerableActions(agentScope)
        logger.info("Final triggerableActions: $triggerableActions")

        // At least one goal-reaching action must be triggerable
        if (!actionsLeadingToGoals.any { it.name in triggerableActions }) {
            return errorResult(
                code = "NO_PATH_FROM_INITIAL_CONDITIONS",
                message = "Agent '${agentScope.name}' has no actions that can be triggered from initial conditions",
                agentScope = agentScope
            )
        }

        return ValidationResult(true, emptyList())
    }

    private fun errorResult(code: String, message: String, agentScope: AgentScope): ValidationResult {
        return ValidationResult(
            false,
            listOf(
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
            )
        )
    }

    private fun findActionsLeadingToGoals(agentScope: AgentScope) =
        agentScope.actions.filter { action ->
            action.effects.any { (effectName, _) ->
                agentScope.goals.any { goal ->
                    goal.name == effectName || goal.preconditions.containsKey(effectName)
                }
            }
        }

    private fun findMissingPreconditionErrors(agentScope: AgentScope): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        for (action in agentScope.actions) {
            val missing = getConditionPreconditions(action.preconditions)
                .filterNot { hasCondition(agentScope, it) }
            if (missing.isNotEmpty()) {
                errors.add(
                    ValidationError(
                        code = "MISSING_PRECONDITION",
                        message = "Action '${action.name}' has preconditions that don't exist: $missing",
                        severity = ValidationSeverity.ERROR,
                        location = ValidationLocation(
                            type = "Action",
                            name = action.name,
                            agentName = agentScope.name,
                            component = action.name
                        )
                    )
                )
            }
        }
        return errors
    }

    private fun findTriggerableActions(agentScope: AgentScope): Set<String> {
        val triggerable = mutableSetOf<String>()
        val actions = agentScope.actions
        val preconditionsFor = actions.associateBy({ it.name }) { getConditionPreconditions(it.preconditions) }

        // First pass: directly triggerable from initial conditions
        for ((actionName, preconditions) in preconditionsFor) {
            if (preconditions.isEmpty() || preconditions.all { hasCondition(agentScope, it) }) {
                triggerable.add(actionName)
            }
        }

        /**
         * Propagate triggerability through action chaining:
         * For each action, if all its preconditions are satisfied either by agent conditions
         * or by the effects of actions already marked as triggerable, then mark it as triggerable.
         * Repeat until no new actions can be marked triggerable in a pass.
         */
        var foundNew: Boolean
        do {
            foundNew = false
            for ((actionName, preconditions) in preconditionsFor) {
                if (actionName !in triggerable && (preconditions.isEmpty() || preconditions.all {
                        hasCondition(agentScope, it) ||
                                actions.any { other ->
                                    other.name in triggerable &&
                                            other.effects.any { (effectName, _) -> effectName.endsWith(it) }
                                }
                    })
                ) {
                    triggerable.add(actionName)
                    foundNew = true
                }
            }
        } while (foundNew)

        return triggerable
    }

    private fun hasCondition(agentScope: AgentScope, preconditionName: String) =
        agentScope.conditions.any { it.name.endsWith(preconditionName) }

    /**
     * Returns only the meaningful, user-defined precondition names,
     * filtering out framework-specific/internal keys such as those starting with
     * "it:" (internal variables/placeholders) or "hasRun_" (execution markers).
     * This ensures validation checks only real, agent-defined conditions.
     */
    private fun getConditionPreconditions(preconditions: Map<String, Any>) =
        preconditions.keys.filterNot { it.startsWith("it:") || it.startsWith("hasRun_") }
}

