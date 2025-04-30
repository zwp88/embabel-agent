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
package com.embabel.agent.api.dsl

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.asTransformation
import com.embabel.agent.core.*
import com.embabel.agent.experimental.primitive.PromptCondition
import com.embabel.common.ai.model.LlmOptions
import com.embabel.plan.goap.ConditionDetermination
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Context for the condition evaluation.
 */
data class ConditionContext(
    override val processContext: ProcessContext,
) : OperationContext, Blackboard by processContext.blackboard

typealias ConditionPredicate = (
    context: ConditionContext,
) -> Boolean?

/**
 * DSL for creating an agent.
 */
fun agent(
    name: String,
    version: String = "0.1.0-SNAPSHOT",
    description: String,
    toolGroups: List<String> = emptyList(),
    block: AgentBuilder.() -> Unit,
): Agent {
    return AgentBuilder(name = name, version = version, description = description, toolGroups = toolGroups)
        .apply(block)
        .build()
}

class AgentBuilder(
    val name: String,
    val version: String = "0.1.0-SNAPSHOT",
    val description: String,
    val toolGroups: List<String> = emptyList(),
) {
    private val actions = mutableListOf<Action>()

    private val goals = mutableSetOf<Goal>()

    private val conditions = mutableSetOf<Condition>()

    private var _toolGroups = mutableListOf<String>()

    init {
        _toolGroups += toolGroups
    }

    fun requireTools(vararg roles: String) {
        _toolGroups += roles
    }

    fun action(block: AgentBuilder.() -> Action) {
        actions.add(block())
    }

    fun goal(
        name: String,
        description: String,
        satisfiedBy: KClass<*>? = null,
        requires: Set<KClass<*>> = if (satisfiedBy != null) {
            setOf(satisfiedBy)
        } else {
            emptySet()
        },
        inputs: Set<IoBinding> = requires.map {
            IoBinding(
//                name = it.simpleName,
                type = it,
            )
        }.toSet(),
        pre: List<Condition> = emptyList(),
        value: Double = 0.0
    ) {
        // TODO check validity
        goals.add(
            Goal(
                name = name,
                description = description,
                inputs = inputs,
                pre = pre.map { it.name }.toSet(),
                value = value
            )
        )
    }

    fun condition(block: AgentBuilder.() -> Condition) {
        conditions.add(block())
    }

    class ConditionDelegateProvider(
        private val specifiedName: String? = null,
        private val factory: (name: String) -> Condition,
        private val conditions: MutableSet<Condition>
    ) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ConditionDelegate {
            val condition = factory(specifiedName ?: prop.name)
            conditions.add(condition) // Store the condition
            return ConditionDelegate(condition)
        }
    }

    class ConditionDelegate(
        private val condition: Condition
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Condition = condition
    }

    fun condition(
        name: String? = null,
        cost: Double = 0.0,
        block: ConditionPredicate
    ): ConditionDelegateProvider {
        return ConditionDelegateProvider(
            name,
            {
                object : Condition {
                    override val name = it
                    override val cost = cost
                    override fun evaluate(processContext: ProcessContext): ConditionDetermination =
                        ConditionDetermination(block(ConditionContext(processContext)))
                }
            },
            this.conditions
        )
    }

    fun condition(
        name: String? = null,
        prompt: (processContext: ProcessContext) -> String,
        llm: LlmOptions = LlmOptions(),
    ): ConditionDelegateProvider {
        return ConditionDelegateProvider(
            specifiedName = name,
            factory = {
                PromptCondition(
                    name = it,
                    prompt = prompt,
                    llm = llm,
                )
            },
            conditions = this.conditions,
        )
    }

    fun build(): Agent {
        return Agent(
            name = name,
            version = version,
            description = description,
            toolGroups = _toolGroups,
            conditions = conditions,
            actions = actions,
            goals = goals,
        )
    }

}

inline fun <reified I, reified O : Any> AgentBuilder.agentAction(agent: Agent) {
    action {
        transformer<I, O>(
            name = agent.name,
            block = agent.asTransformation<I, O>()
        )
    }
}
