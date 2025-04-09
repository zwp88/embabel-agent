/*
 * Copyright 2025 Embabel Software, Inc.
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

import com.embabel.agent.*
import com.embabel.agent.annotation.*
import com.embabel.agent.annotation.Action
import com.embabel.agent.annotation.Condition
import com.embabel.agent.dsl.Transformation
import com.embabel.agent.dsl.TransformationPayload
import com.embabel.agent.dsl.expandInputBindings
import com.embabel.agent.primitive.LlmOptions
import com.embabel.agent.support.AbstractAction
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

/**
 * Read AgentMetadata from annotated classes.
 * Looks for @Agentic, @Condition and @Action annotations
 * and properties of type Goal.
 */
@Service
class AgentMetadataReader {

    private val logger = LoggerFactory.getLogger(AgentMetadataReader::class.java)

    /**
     * Given this configured instance, find all the methods annotated with @Action and @Condition
     * The instance will have been previous injected by Spring if it's Spring-managed.
     * @return null if the class doesn't satisfy the requirements of @Agentic
     * or doesn't have the annotation at all.
     */
    fun createAgentMetadata(instance: Any): AgentMetadata? {
        val type = instance.javaClass
        if (!type.isAnnotationPresent(Agentic::class.java)) {
            logger.debug("No @{} annotation found on {}", Agentic::class.simpleName, type)
            return null
        }
        val getterGoals = findGoalGetters(type).map { getGoal(it, instance) }
        val actionMethods = findActionMethods(type)
        val conditionMethods = findConditionMethods(type)
        val actionGoals = actionMethods.mapNotNull { createGoalFromActionMethod(it) }
        val goals = getterGoals + actionGoals

        if (actionMethods.isEmpty() && goals.isEmpty() && conditionMethods.isEmpty()) {
            throw IllegalArgumentException(
                "No methods annotated with @${Action::class.simpleName} or @${Condition::class.simpleName} and no goals defined on ${type.simpleName}",
            )
        }

        val toolCallbacks = ToolCallbacks.from(instance).toList()
        return AgentMetadata(
            name = type.name,
            actions = actionMethods.map { createAction(it, instance, toolCallbacks) },
            conditions = conditionMethods.map { createCondition(it, instance) },
            goals = goals.toSet(),
        )
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
                if (Goal::class.java.isAssignableFrom(method.returnType)) {
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
    ): Goal {
        // We need to change the name to be the property name
        val rawg = ReflectionUtils.invokeMethod(method, instance) as Goal
        return rawg.copy(name = getterToPropertyName(method.name))
    }

    private fun createCondition(
        method: Method,
        instance: Any,
    ): BooleanCondition {
        val conditionAnnotation = method.getAnnotation(Condition::class.java)
        return BooleanCondition(
            name = method.name,
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
            name = method.name,
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
                payload.transform(
                    input = payload.input,
                    prompt = { e.prompt },
                    // TODO or default options
                    llmOptions = e.llm ?: LlmOptions(),
                    toolCallbacks = toolCallbacks + toolCallbacksOnDomainObjects,
                    outputClass = payload.outputClass as Class<Any>,
                )
            }
        }
    }

    private fun createGoalFromActionMethod(
        method: Method,
    ): Goal? {
        val actionAnnotation = method.getAnnotation(Action::class.java)
        val goalAnnotation = method.getAnnotation(AchievesGoal::class.java)
        if (goalAnnotation == null) {
            return null
        }
        val inputBinding = IoBinding(
            name = actionAnnotation.outputBinding,
            type = method.returnType.simpleName,
        )
        return Goal(
            name = "Create ${method.returnType.simpleName}",
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

private class MultiTransformer<O : Any>(
    name: String,
    description: String = name,
    pre: List<String> = emptyList(),
    post: List<String> = emptyList(),
    cost: Double = 0.0,
    value: Double = 0.0,
    transitions: List<Transition> = emptyList(),
    canRerun: Boolean = false,
    qos: Qos = Qos(),
    inputs: Set<IoBinding>,
    private val inputClasses: List<Class<*>>,
    private val outputClass: Class<O>,
    private val outputVarName: String? = "it",
    private val referencedInputProperties: Set<String>? = null,
    override val toolCallbacks: Collection<ToolCallback>,
    toolGroups: Collection<String>,
    private val block: Transformation<List<Any>, O>,
) : AbstractAction(
    name = name,
    description = description,
    pre = pre,
    post = post,
    cost = cost,
    value = value,
    inputs,
    outputs = if (outputVarName == null) emptySet() else setOf(IoBinding(outputVarName, outputClass.simpleName)),
    transitions = transitions,
    toolCallbacks = toolCallbacks,
    toolGroups = toolGroups,
    canRerun = canRerun,
    qos = qos,
) {

    override val domainTypes
        get() = inputClasses + outputClass

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, SchemaType>,
        action: com.embabel.agent.Action
    ): ActionStatus = ActionRunner.execute {
        val inputValues: List<Any> = inputs.map {
            processContext.getValue(variable = it.name, type = it.type)
                ?: throw IllegalArgumentException("Input ${it.name} of type ${it.type} not found in process context")
        }
        logger.debug("Resolved action {} inputs {}", name, inputValues)
        val output = block.transform(
            TransformationPayload(
                input = inputValues,
                processContext = processContext,
                inputClass = List::class.java as Class<List<Any>>,
                outputClass = outputClass,
                action = this,
            )
        )
        if (outputVarName != null) {
            processContext.blackboard[outputVarName] = output
        } else {
            processContext.blackboard += output
        }
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return referencedInputProperties ?: run {
            val fields = inputClasses.map { it.declaredFields.map { it.name } }.flatten().toSet()
            fields
        }
    }
}
