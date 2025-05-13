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
package com.embabel.agent.core

import com.embabel.common.core.types.ZeroToOne
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.EffectSpec
import com.embabel.plan.goap.GoapGoal
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Agent platform goal. Exposes GOAP metadata.
 */
data class Goal(
    override val name: String,
    override val description: String,
    val pre: Set<String> = emptySet(),
    override val inputs: Set<IoBinding> = emptySet(),
    override val value: ZeroToOne = 0.0,
) : GoapGoal, AgentSystemStep {

    // These methods are for Java, to obviate the builder antipattern
    fun withPrecondition(preconditions: String): Goal {
        return copy(pre = pre + preconditions)
    }

    fun withValue(value: Double): Goal {
        return copy(value = value)
    }

    @JsonIgnore
    override val preconditions: EffectSpec =
        run {
            val conditions = pre.associate { it to ConditionDetermination.Companion(true) }.toMutableMap()
            inputs.forEach { input ->
                conditions[input.value] = ConditionDetermination.Companion(true)
            }
            conditions
        }

    override fun infoString(verbose: Boolean?): String {
        val separator = if (verbose == true) "\n\t\t" else " - "
        return "$description: $name${separator}pre=${preconditions} value=${value}"
    }

    companion object {

        // Methods for Java

        /**
         * Goal of creating an instance of this type
         */
        @JvmStatic
        fun createInstance(
            description: String,
            type: Class<*>,
            name: String = "Create ${type.simpleName}",
        ): Goal {
            return invoke(name = name, description = description, satisfiedBy = type)
        }

        operator fun invoke(
            name: String,
            description: String,
            satisfiedBy: Class<*>? = null,
            requires: Set<Class<*>> = if (satisfiedBy != null) {
                setOf(satisfiedBy)
            } else {
                emptySet()
            },
            inputs: Set<IoBinding> = requires.map {
                IoBinding(
                    type = it,
                )
            }.toSet(),
            pre: List<Condition> = emptyList(),
            value: Double = 0.0,
        ): Goal {
            return Goal(
                name = name,
                description = description,
                inputs = inputs,
                pre = pre.map { it.name }.toSet(),
                value = value
            )
        }
    }

}
