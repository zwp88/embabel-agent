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
package com.embabel.agent.annotation.support

import com.embabel.agent.annotation.*
import com.embabel.agent.core.AgentMetadata
import com.embabel.agent.core.BooleanCondition
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.primitive.LlmOptions
import com.embabel.agent.dsl.expandInputBindings
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import com.embabel.agent.core.Goal as IGoal


/**
 * Read AgentMetadata from annotated classes.
 * Looks for @Agentic, @Condition and @Action annotations
 * and properties of type Goal.
 * Warn on invalid or missing annotations but never throw an exception
 * as this could affect application startup.
 */
@Service
class AgentMetadataReader {

    private val logger = LoggerFactory.getLogger(AgentMetadataReader::class.java)

    /**
     * Given this configured instance, find all the methods annotated with @Action and @Condition
     * The instance will have been previous injected by Spring if it's Spring-managed.
     * @return null if the class doesn't satisfy the requirements of @Agentic
     * or doesn't have the annotation at all.
     * @return an Agent if the class has the @Agent annotation,
     * otherwise the AgentMetadata superinterface
     */
    fun createAgentMetadata(instance: Any): AgentMetadata? {
        val type = instance.javaClass
        val agenticAnnotation = type.getAnnotation(Agentic::class.java)
        val agentAnnotation = type.getAnnotation(com.embabel.agent.annotation.Agent::class.java)
        if (agenticAnnotation == null && agentAnnotation == null) {
            logger.debug(
                "No @{} or @{} annotation found on {}",
                Agentic::class.simpleName,
                com.embabel.agent.annotation.Agent::class.simpleName,
                type.name,
            )
            return null
        }
        if (agenticAnnotation != null && agentAnnotation != null) {
            logger.debug(
                "Both @{} and @{} annotations found on {}. Treating class as Agent, but both should not be used",
                Agentic::class.simpleName,
                com.embabel.agent.annotation.Agent::class.simpleName,
                type.name,
            )
            return null
        }
        val getterGoals = findGoalGetters(type).map { getGoal(it, instance) }
        val actionMethods = findActionMethods(type)
        val conditionMethods = findConditionMethods(type)
        val actionGoals = actionMethods.mapNotNull { createGoalFromActionMethod(it) }
        val goals = getterGoals + actionGoals

        if (actionMethods.isEmpty() && goals.isEmpty() && conditionMethods.isEmpty()) {
            logger.warn(
                "No methods annotated with @{} or @{} and no goals defined on {}",
                Action::class.simpleName,
                Condition::class.simpleName,
                type.simpleName,
            )
            return null
        }
        val toolCallbacks = ToolCallbacks.from(instance).toList()

        val conditions = conditionMethods.map { createCondition(it, instance) }
        val actions = actionMethods.map { createAction(it, instance, toolCallbacks) }

        if (agentAnnotation != null) {
            return com.embabel.agent.core.Agent(
                name = agentAnnotation.name.ifBlank { type.name },
                description = agentAnnotation.description.ifBlank {
                    logger.error(
                        "No description provided for @{} on {}",
                        Agent::class.simpleName,
                        type.simpleName,
                    )
                    type.simpleName
                },
                conditions = conditions,
                actions = actions,
                goals = goals.toSet(),
            )
        }

        return AgentMetadata(
            name = type.name,
            conditions = conditions,
            actions = actions,
            goals = goals.toSet(),
        )
    }

    /**
     * Generate a qualified name to avoid name clashes.
     */
    private fun generateName(instance: Any, name: String): String {
        return "${instance.javaClass.name}.$name"
    }

    private fun findConditionMethods(type: Class<*>): List<Method> {
        val conditionMethods = mutableListOf<Method>()
        ReflectionUtils.doWithMethods(
            type,
            { method -> conditionMethods.add(method) },
            // Make sure we only get annotated methods from this type, not supertypes
            { method ->
                method.isAnnotationPresent(Condition::class.java) &&
                        type.declaredMethods.contains(method)
            })
        return conditionMethods
    }

    private fun findActionMethods(type: Class<*>): List<Method> {
        val actionMethods = mutableListOf<Method>()
        ReflectionUtils.doWithMethods(
            type,
            { method -> actionMethods.add(method) },
            // Make sure we only get annotated methods from this type, not supertypes
            { method ->
                method.isAnnotationPresent(Action::class.java) &&
                        type.declaredMethods.contains(method)
            })
        if (actionMethods.isEmpty()) {
            logger.info("No methods annotated with @{} found in {}", Action::class.simpleName, type)
        }
        return actionMethods
    }

    private fun findGoalGetters(type: Class<*>): List<Method> {
        val goalGetters = mutableListOf<Method>()
        type.declaredMethods.forEach { method ->
            if (method.parameterCount == 0 &&
                method.returnType != Void.TYPE
            ) {
                if (IGoal::class.java.isAssignableFrom(method.returnType)) {
                    goalGetters.add(method)
                }
            }
        }
        if (goalGetters.isEmpty()) {
            logger.debug("No goal getters found in {}", type)
        }
        return goalGetters
    }

    private fun getGoal(
        method: Method,
        instance: Any,
    ): IGoal {
        // We need to change the name to be the property name
        val rawg = ReflectionUtils.invokeMethod(method, instance) as IGoal
        return rawg.copy(name = generateName(instance, getterToPropertyName(method.name)))
    }

    private fun createCondition(
        method: Method,
        instance: Any,
    ): BooleanCondition {
        val conditionAnnotation = method.getAnnotation(Condition::class.java)
        return BooleanCondition(
            name = generateName(instance, method.name),
            cost = conditionAnnotation.cost,
        )
        { processContext -> ReflectionUtils.invokeMethod(method, instance, processContext) as Boolean }
    }

    private fun createAction(
        method: Method,
        instance: Any,
        toolCallbacks: List<ToolCallback>,
    ): MultiTransformer<Any> {
        val actionAnnotation = method.getAnnotation(Action::class.java)
        val inputClasses = method.parameters
            .map { it.type }
        val inputs = method.parameters
            .map {
                val nameMatchAnnotation = it.getAnnotation(RequireNameMatch::class.java)
                expandInputBindings(if (nameMatchAnnotation != null) it.name else IoBinding.DEFAULT_BINDING, it.type)
            }
            .flatten()
        return MultiTransformer(
            name = generateName(instance, method.name),
            description = actionAnnotation.description.ifBlank { method.name },
            cost = actionAnnotation.cost,
            inputs = inputs.toSet(),
            pre = actionAnnotation.pre.toList(),
            inputClasses = inputClasses,
            outputClass = method.returnType as Class<Any>,
            outputVarName = actionAnnotation.outputBinding,
            toolCallbacks = toolCallbacks,
            toolGroups = actionAnnotation.toolGroups.asList(),
        )
        { payload ->
            logger.info("Invoking action method {} with payload {}", method.name, payload.input)
            val toolCallbacksOnDomainObjects = ToolCallbacks.from(*payload.input.toTypedArray())
            try {
                ReflectionUtils.invokeMethod(method, instance, *payload.input.toTypedArray()) as Any
            } catch (e: ExecutePromptException) {
                if (e.requireResult) {
                    payload.transform(
                        input = payload.input,
                        prompt = { e.prompt },
                        // TODO or default options
                        llmOptions = e.llm ?: LlmOptions(),
                        toolCallbacks = toolCallbacks + toolCallbacksOnDomainObjects,
                        outputClass = payload.outputClass as Class<Any>,
                    )
                } else {
                    val result = payload.maybeTransform(
                        input = payload.input,
                        prompt = { e.prompt },
                        // TODO or default options
                        llmOptions = e.llm ?: LlmOptions(),
                        toolCallbacks = toolCallbacks + toolCallbacksOnDomainObjects,
                        outputClass = payload.outputClass as Class<Any>,
                    )
                    result.getOrThrow()
                }
            }
        }
    }

    /**
     * If the @Action method also has an @AchievesGoal annotation,
     * create a goal from it.
     */
    private fun createGoalFromActionMethod(
        method: Method,
    ): IGoal? {
        val actionAnnotation = method.getAnnotation(Action::class.java)
        val goalAnnotation = method.getAnnotation(AchievesGoal::class.java)
        if (goalAnnotation == null) {
            return null
        }
        val inputBinding = IoBinding(
            name = actionAnnotation.outputBinding,
            type = method.returnType.name,
        )
        return IGoal(
            name = "create_${method.returnType.simpleName}",
            description = goalAnnotation.description,
            inputs = setOf(inputBinding),
            value = goalAnnotation.value,
        )
    }
}

private fun getterToPropertyName(name: String): String {
    return if (name.startsWith("get")) {
        name.substring(3).decapitalize()
    } else {
        name
    }
}
