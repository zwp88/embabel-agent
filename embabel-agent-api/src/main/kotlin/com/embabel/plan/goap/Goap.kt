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

import com.embabel.plan.*

/**
 * Conditions may be true, false or unknown
 */
enum class ConditionDetermination {
    TRUE, FALSE, UNKNOWN;

    // Was it calculated?

    companion object {
        operator fun invoke(value: Boolean?) = when (value) {
            true -> TRUE
            false -> FALSE
            null -> UNKNOWN
        }
    }
}

typealias GoapState = Map<String, ConditionDetermination>

typealias EffectSpec = Map<String, ConditionDetermination>

private fun preconditionsSatisfied(preconditions: EffectSpec, currentState: GoapState): Boolean =
    preconditions.all { (key, value) -> currentState[key] == value }

/**
 * Determine the world state: the conditions that drive GOAP planning
 * Our conditions can have 3 values: true, false or unknown.
 * Unknown may be genuinely unknown, or it may mean that the condition has been lazily evaluated
 * and needs to be evaluated again.
 */
interface WorldStateDeterminer {

    /**
     * Determine world state. Optimization is permitted.
     * Implementations may choose to return UNKNOWN for expensive conditions,
     * which the planner should invoke lazily
     */
    fun determineWorldState(): WorldState

    /**
     * Determine an individual condition, disabling any caching.
     * Any previously UNKNOWN condition must be re-evaluated if possible.
     */
    fun determineCondition(condition: String): ConditionDetermination

    companion object {

        fun fromMap(
            map: Map<String, ConditionDetermination>,
        ): WorldStateDeterminer =
            FromMapWorldStateDeterminer(map)

    }

}

private class FromMapWorldStateDeterminer(
    private val map: Map<String, ConditionDetermination>,
) : WorldStateDeterminer {

    override fun determineWorldState(): WorldState = WorldState(map)

    override fun determineCondition(condition: String): ConditionDetermination {
        return map[condition] ?: ConditionDetermination.UNKNOWN
    }
}

data class WorldState(
    val state: GoapState = emptyMap(),
) {

    fun unknownConditions(): Collection<String> =
        state.entries
            .filter { it.value == ConditionDetermination.UNKNOWN }
            .map { it.key }

    /**
     * Generate variants with different values for this condition
     */
    internal fun variants(unknownCondition: String): Collection<WorldState> {
        return setOf(ConditionDetermination.TRUE, ConditionDetermination.FALSE).map {
            this + (unknownCondition to it)
        }
    }

    operator fun plus(pair: Pair<String, ConditionDetermination>): WorldState =
        WorldState(this.state + pair)
}

interface GoapStep : Step {

    /**
     * Conditions that must be true for this step to execute
     */
    val preconditions: EffectSpec

    /**
     * Whether the step is available in the current world state
     */
    fun isAchievable(currentState: WorldState): Boolean {
        return preconditionsSatisfied(preconditions, currentState.state)
    }

}

/**
 * Action in a GOAP system.
 */
interface GoapAction : GoapStep, Action {

    /**
     * Expected effects of this action.
     * World state should be checked afterwards as these effects may not
     * have been achieved
     */
    val effects: EffectSpec

    override fun infoString(verbose: Boolean?): String =
        "$name - pre=${preconditions} cost=$cost value=${value}"


    companion object {

        operator fun invoke(
            name: String,
            pre: Collection<String> = emptySet(),
            preconditions: EffectSpec = pre.associateWith { ConditionDetermination.TRUE },
            post: Collection<String> = emptySet(),
            effects: EffectSpec = post.associateWith { ConditionDetermination.TRUE },
            cost: Double = 0.0,
            value: Double = 0.0,
        ): GoapAction {
            return SimpleGoapAction(
                name = name,
                preconditions,
                effects,
                cost = cost,
                value = value,
            )
        }
    }

}

private data class SimpleGoapAction(
    override val name: String,
    override val preconditions: EffectSpec,
    override val effects: EffectSpec,
    override val cost: Double,
    override val value: Double,
) : GoapAction

/**
 * Goal in a GOAP system.
 */
interface GoapGoal : GoapStep, Goal {

    override fun infoString(verbose: Boolean?): String =
        "$name - pre=${preconditions} value=${value}"

    companion object {

        operator fun invoke(
            name: String,
            preconditions: EffectSpec = mapOf(name to ConditionDetermination(true)),
            value: Double = 0.0
        ): GoapGoal {
            return GoapGoalImpl(name, preconditions, value)
        }

        operator fun invoke(
            name: String,
            pre: Collection<String>,
            value: Double = 0.0
        ): GoapGoal {
            return GoapGoalImpl(
                name,
                pre.associateWith { ConditionDetermination.TRUE },
                value,
            )
        }
    }

}

private data class GoapGoalImpl(
    override val name: String,
    override val preconditions: EffectSpec,
    override val value: Double = 0.0,
) : GoapGoal

data class GoapSystem(
    override val actions: Set<GoapAction>,
    override val goals: Set<GoapGoal>,
) : PlanningSystem {

    constructor(
        actions: Collection<GoapAction>,
        goal: GoapGoal,
    ) : this(
        actions = actions.toSet(),
        goals = setOf(goal),
    )

    fun knownPreconditions(): Set<String> {
        return actions.flatMap { it.preconditions.keys }.toSet()
    }

    fun knownEffects(): Set<String> {
        return actions.flatMap { it.effects.keys }.toSet()
    }

    fun knownConditions(): Set<String> {
        return knownPreconditions() + knownEffects()
    }

    fun infoString(): String =
        "GOAP system: actions=${actions.map { it.name }}, goals=${goals.map { it.name }}, knownPreconditions=${knownPreconditions()}, knownEffects=${knownEffects()}"
}

class GoapPlan(
    actions: List<Action>,
    goal: Goal,
    val worldState: WorldState,
) : Plan(actions, goal) {

    override fun infoString(verbose: Boolean?): String {
        return super.infoString(verbose) + "; worldState=$worldState"
    }

    override fun toString(): String {
        return infoString(verbose = false)
    }
}
