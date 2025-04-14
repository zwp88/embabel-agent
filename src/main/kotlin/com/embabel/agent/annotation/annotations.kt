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
package com.embabel.agent.annotation

import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ZeroToOne
import org.springframework.stereotype.Component


/**
 * Indicates that this class exposes agentic functions.
 * This is a Spring stereotype annotation, so will be picked up on the classpath and injected
 * if it's on the configuration classpath.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
)
@Component
annotation class Agentic

/**
 * Indicates that this class is an agent.
 * It doesn't just contribution actions, goals and conditions:
 * it is an agent in itself.
 * Either @Agent or @Agentic should be used: not both
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
)
@Component
annotation class Agent(
    val name: String = "",
    val description: String,
)

/**
 * Annotates a method that evaluates a condition.
 * This will have access to the processContext and also
 * can use any other state.
 * @param cost Cost of evaluating the condition, between 0 and 1.
 * 0 is cheap; 1 is the most expensive. The platform can use this
 * information for optimization.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Condition(
    val cost: ZeroToOne = 0.0,
)

/**
 * Annotation to indicate a method implementing an Action.
 * Methods can have any number of parameters, which represent
 * necessary input types.
 * Methods can return any type. The return type will become
 * an effect.
 * @param description Description of the action. Less important than for
 * goals as a planner chooses actions based on preconditions
 * and effects rather than by description. The description property is
 * used for documentation purposes, having the advantage over comments
 * that it can appear in logs.
 * @param pre Preconditions for the action
 * @param outputBinding Output binding for the action.
 * Only required for a custom binding.
 * @param cost Cost of executing the action
 * @param value Value of performing the action
 * @param toolGroups Tool groups that this action requires. These are well known tools from the server.
 * @Tool methods on the @Agentic class are automatically added.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Action(
    val description: String = "",
    val pre: Array<String> = [],
    val post: Array<String> = [],
    val canRerun: Boolean = false,
    val outputBinding: String = IoBinding.DEFAULT_BINDING,
    val cost: Double = 0.0,
    val value: Double = 0.0,
    val toolGroups: Array<String> = [],
)

/**
 * Annotation that can be added to an @Action method
 * to indicate that its execution achieves a goal
 * @param description description of the goal. The name will be auto-generated
 * @param value value of achieving the goal
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AchievesGoal(
    val description: String,
    val value: Double = 0.0,
)

/**
 * Annotation that can added to parameters of an @Action method
 * to indicate that the parameter name must match the input binding.
 * Otherwise, it can match the current ("it") value.
 * @see IoBinding
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireNameMatch
