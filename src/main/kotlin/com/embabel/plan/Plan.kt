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
import com.embabel.common.core.types.Named

/**
 * A step in a plan. Can be an action or a goal
 */
interface Step : Named {

    /**
     * Unique name of the step
     */
    override val name: String

    /**
     * Value of completing this step.
     */
    val value: Double
}

interface Action : Step {

    /**
     * Cost of performing this action
     */
    val cost: Double

}

interface Goal : Step

/**
 * Plan to achieve a goal
 * The plan should be reassessed after each action each perform.
 * @param actions The actions to perform, in order
 * @param goal The goal to achieve
 */
open class Plan(
    val actions: List<Action>,
    val goal: Goal,
) : HasInfoString {

    fun isComplete() = actions.isEmpty()

    val cost: Double get() = actions.sumOf { it.cost }

    val actionsValue: Double get() = actions.sumOf { it.value }

    val netValue: Double get() = goal.value + actionsValue - cost

    override fun infoString(verbose: Boolean?): String {
        return if (verbose == true) {
            "\n${
                actions.mapIndexed { index, action ->
                    "\t".repeat(index + 1) + action.name
                }.joinToString(" ->\n")
            }\n\tcost=$cost; netValue=$netValue"

        } else {
            actions.joinToString(" -> ") { it.name } +
                    "; netValue=$netValue"
        }
    }

}
