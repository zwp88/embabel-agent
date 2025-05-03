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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the [GoapPlanningSystem] class.
 *
 * The GoapPlanningSystem collects and manages a set of actions and goals for
 * goal-oriented action planning. These tests verify:
 *
 * - Collection of unique preconditions from all actions
 * - Collection of unique effects from all actions
 * - Determination of all conditions (union of preconditions and effects)
 * - System construction with multiple goals or a single goal
 * - Formatting of system information for display
 *
 * These tests ensure the planning system correctly tracks and manages
 * the conditions needed for planning with GOAP algorithms.
 */
class GoapPlanningSystemTest {

    @Test
    fun `test knownPreconditions returns all unique preconditions`() {
        // Create actions with various preconditions
        val action1 = GoapAction("Action1",
            preconditions = mapOf("cond1" to ConditionDetermination.TRUE, "cond2" to ConditionDetermination.FALSE))

        val action2 = GoapAction("Action2",
            preconditions = mapOf("cond2" to ConditionDetermination.TRUE, "cond3" to ConditionDetermination.UNKNOWN))

        val system = GoapPlanningSystem(setOf(action1, action2), GoapGoal("Goal1"))

        // Verify
        val preconditions = system.knownPreconditions()
        assertEquals(3, preconditions.size)
        assertTrue(preconditions.containsAll(listOf("cond1", "cond2", "cond3")))
    }

    @Test
    fun `test knownEffects returns all unique effects`() {
        // Create actions with various effects
        val action1 = GoapAction("Action1",
            effects = mapOf("effect1" to ConditionDetermination.TRUE, "effect2" to ConditionDetermination.FALSE))

        val action2 = GoapAction("Action2",
            effects = mapOf("effect2" to ConditionDetermination.TRUE, "effect3" to ConditionDetermination.UNKNOWN))

        val system = GoapPlanningSystem(setOf(action1, action2), GoapGoal("Goal1"))

        // Verify
        val effects = system.knownEffects()
        assertEquals(3, effects.size)
        assertTrue(effects.containsAll(listOf("effect1", "effect2", "effect3")))
    }

    @Test
    fun `test knownConditions returns union of preconditions and effects`() {
        // Create actions with preconditions and effects
        val action1 = GoapAction("Action1",
            preconditions = mapOf("cond1" to ConditionDetermination.TRUE),
            effects = mapOf("effect1" to ConditionDetermination.TRUE))

        val action2 = GoapAction("Action2",
            preconditions = mapOf("cond2" to ConditionDetermination.TRUE),
            effects = mapOf("effect2" to ConditionDetermination.TRUE))

        val system = GoapPlanningSystem(setOf(action1, action2), GoapGoal("Goal1"))

        // Verify
        val conditions = system.knownConditions()
        assertEquals(4, conditions.size)
        assertTrue(conditions.containsAll(listOf("cond1", "cond2", "effect1", "effect2")))
    }

    @Test
    fun `test GoapPlanningSystem construction with multiple goals`() {
        val goal1 = GoapGoal("Goal1")
        val goal2 = GoapGoal("Goal2")

        val system = GoapPlanningSystem(emptySet(), setOf(goal1, goal2))

        assertEquals(2, system.goals.size)
        assertTrue(system.goals.containsAll(listOf(goal1, goal2)))
    }

    @Test
    fun `test GoapPlanningSystem infoString contains all key information`() {
        val action = GoapAction("Action1",
            preconditions = mapOf("cond1" to ConditionDetermination.TRUE),
            effects = mapOf("effect1" to ConditionDetermination.TRUE))

        val goal = GoapGoal("Goal1")

        val system = GoapPlanningSystem(setOf(action), setOf(goal))

        val info = system.infoString()
        assertTrue(info.contains("Action1"))
        assertTrue(info.contains("Goal1"))
        assertTrue(info.contains("knownPreconditions"))
        assertTrue(info.contains("knownEffects"))
    }

    @Test
    fun `test GoapPlanningSystem constructor from collection of actions and single goal`() {
        val action1 = GoapAction("Action1")
        val action2 = GoapAction("Action2")
        val goal = GoapGoal("Goal1")

        val system = GoapPlanningSystem(
            actions = listOf(action1, action2),
            goal = goal
        )

        assertEquals(2, system.actions.size)
        assertEquals(1, system.goals.size)
        assertTrue(system.actions.containsAll(listOf(action1, action2)))
        assertTrue(system.goals.contains(goal))
    }
}
