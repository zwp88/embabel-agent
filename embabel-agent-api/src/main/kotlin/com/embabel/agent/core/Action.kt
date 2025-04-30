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

import com.embabel.agent.core.support.SerializableAction
import com.embabel.common.core.types.Described
import com.embabel.common.util.kotlin.loggerFor
import com.embabel.plan.goap.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Binding definition of form name:Type
 * If name is omitted, it is assumed to be 'it'
 * Used to build preconditions from input and output bindings.
 * Default name ("it") has a special meaning. It will be satisfied
 * by an instance of the correct type being bound to "it", but also by
 * the final result of the action having the correct type.
 */
@JvmInline
value class IoBinding(val value: String) {
    init {
        require(value.isNotBlank()) { "Type definition must not be blank" }
    }

    constructor(name: String, type: String) : this("$name:$type")

    constructor(name: String? = DEFAULT_BINDING, type: Class<*>) : this("$name:${type.name}")
    constructor(name: String? = DEFAULT_BINDING, type: KClass<*>) : this("$name:${type.qualifiedName}")

    val type: String
        get() = if (value.contains(":")) {
            value.split(":")[1]
        } else {
            value
        }

    val name: String
        get() =
            if (value.contains(":")) {
                value.split(":")[0]
            } else {
                DEFAULT_BINDING
            }

    companion object {
        /**
         * The default binding, when it is not otherwise specified.
         * Consistent with Groovy and Kotlin behavior.
         */
        const val DEFAULT_BINDING = "it"
    }

}

data class Goal(
    override val name: String,
    override val description: String,
    val pre: Set<String> = emptySet(),
    override val inputs: Set<IoBinding> = emptySet(),
    override val value: Double = 0.0,
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
            val conditions = pre.associate { it to ConditionDetermination(true) }.toMutableMap()
            inputs.forEach { input ->
                conditions[input.value] = ConditionDetermination(true)
            }
            conditions
        }

    override fun infoString(verbose: Boolean?) =
        "$name - pre=${preconditions} value=${value}: $description"

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
            // TODO check validity
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

interface AgentSystemStep : GoapStep, Described {

    /**
     * Data inputs to this set.
     * Will be used to build preconditions
     * in addition to explicit preconditions.
     */
    val inputs: Set<IoBinding>

}

/**
 * Core Action model in Agent system.
 * An individual action step used in an Agent
 * Not intended for direct use by application code.
 * User applications should use the annotation programming model
 * with @Agentic and @Action or the Kotlin DSL, or a non-code
 * representation such as YML.
 * @qos Quality of Service. Governs retry policy
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SerializableAction::class),
)
interface Action : AgentSystemStep, GoapAction, ActionRunner, DataDictionary, ToolConsumer {
    val outputs: Set<IoBinding>
    override val cost: Double get() = 0.0

    /**
     * Can this action be run again if it's already run?
     */
    val canRerun: Boolean

    val transitions: List<Transition>
    val qos: ActionQos

    override val schemaTypes: Collection<SchemaType>
        get() =
            (inputs + outputs)
                .mapNotNull {
                    referencedType(it, this)
                }

    private fun referencedType(binding: IoBinding, action: Action): SchemaType? {
        var type = SchemaType(name = binding.type)
        for (prop in action.referencedInputProperties(binding.name)) {
            loggerFor<Action>().debug("Discovered property {}", prop)
            type = type.withProperty(PropertyDefinition(name = prop))
        }
        loggerFor<Action>().debug(
            "Action {} references variable {} of type {}: {}",
            action.name,
            binding.name,
            binding.type,
            type,
        )

        return type
    }

    override fun infoString(verbose: Boolean?): String =
        "$name - pre=${preconditions} post=${effects}"

}
