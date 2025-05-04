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
package com.embabel.agent.api.annotation.support

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.CreateObjectPromptException
import com.embabel.agent.api.common.EvaluateConditionPromptException
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.dsl.expandInputBindings
import com.embabel.agent.core.AgentScope
import com.embabel.agent.core.ComputedBooleanCondition
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.support.HAS_RUN_CONDITION_PREFIX
import com.embabel.agent.core.support.safelyGetToolCallbacksFrom
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.Named
import com.embabel.common.core.util.DummyInstanceCreator
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import com.embabel.agent.core.Action as CoreAction
import com.embabel.agent.core.Agent as CoreAgent
import com.embabel.agent.core.Goal as AgentCoreGoal

/**
 * Agentic info about a type
 */
data class AgenticInfo(
    val type: Class<*>,
) {

    val agenticAnnotation: Agentic? = type.getAnnotation(Agentic::class.java)
    val agentAnnotation: Agent? = type.getAnnotation(Agent::class.java)

    /**
     * Is this type agentic at all?
     */
    fun agentic() = agenticAnnotation != null || agentAnnotation != null

    fun validationErrors(): Collection<String> {
        val errors = mutableListOf<String>()
        if (agenticAnnotation != null && agentAnnotation != null) {
            errors += "Both @Agentic and @Agent annotations found on ${type.name}. Treating class as Agent, but both should not be used"
        }
        if (agentAnnotation != null && agentAnnotation.description.isBlank()) {
            errors + "No description provided for @${Agent::class.java.simpleName} on ${type.name}"
        }
        return errors
    }

    fun noAutoScan() = agenticAnnotation?.scan == false || agentAnnotation?.scan == false
}

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
    fun createAgentMetadata(instance: Any): AgentScope? {
        if (instance is Class<*>) {
            logger.warn(
                "❓Call to createAgentMetadata with class {}. Pass an instance",
                instance.name,
            )
            return null
        }
        val agenticInfo = AgenticInfo(instance.javaClass)
        if (!agenticInfo.agentic()) {
            logger.debug(
                "No @{} or @{} annotation found on {}",
                Agentic::class.simpleName,
                Agent::class.simpleName,
                agenticInfo.type.name,
            )
            return null
        }
        if (agenticInfo.validationErrors().isNotEmpty()) {
            logger.warn(
                agenticInfo.validationErrors().joinToString("\n"),
                Agentic::class.simpleName,
                Agent::class.simpleName,
                agenticInfo.type.name,
            )
            return null
        }
        val getterGoals = findGoalGetters(agenticInfo.type).map { getGoal(it, instance) }
        val actionMethods = findActionMethods(agenticInfo.type)
        val conditionMethods = findConditionMethods(agenticInfo.type)

        val toolCallbacksOnInstance = safelyGetToolCallbacksFrom(instance)

        val conditions = conditionMethods.map { createCondition(it, instance) }.toSet()
        val (actions, actionGoals) = actionMethods.map { actionMethod ->
            val action = createAction(actionMethod, instance, toolCallbacksOnInstance)
            Pair(action, createGoalFromActionMethod(actionMethod, action, instance))
        }.unzip()

        val goals = getterGoals + actionGoals.filterNotNull()

        if (actionMethods.isEmpty() && goals.isEmpty() && conditionMethods.isEmpty()) {
            logger.warn(
                "❓No methods annotated with @{} or @{} and no goals defined on {}",
                Action::class.simpleName,
                Condition::class.simpleName,
                agenticInfo.type.name,
            )
            return null
        }

        if (agenticInfo.agentAnnotation != null) {
            return CoreAgent(
                name = agenticInfo.agentAnnotation.name.ifBlank { agenticInfo.type.name },
                description = agenticInfo.agentAnnotation.description,
                version = agenticInfo.agentAnnotation.version,
                conditions = conditions,
                actions = actions,
                goals = goals.toSet(),
                toolGroups = agenticInfo.agentAnnotation.toolGroups.toList(),
            )
        }

        return AgentScope(
            name = agenticInfo.type.name,
            conditions = conditions,
            actions = actions,
            goals = goals.toSet(),
        )
    }

    /**
     * Generate a qualified name to avoid name clashes.
     * @param instance The instance of the class we are reading
     * @param name The name of the method or property for which we should generate a method
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
            logger.debug("No methods annotated with @{} found in {}", Action::class.simpleName, type)
        }
        return actionMethods
    }

    private fun findGoalGetters(type: Class<*>): List<Method> {
        val goalGetters = mutableListOf<Method>()
        type.declaredMethods.forEach { method ->
            if (method.parameterCount == 0 &&
                method.returnType != Void.TYPE
            ) {
                if (AgentCoreGoal::class.java.isAssignableFrom(method.returnType)) {
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
    ): AgentCoreGoal {
        // We need to change the name to be the property name
        val rawGoal = ReflectionUtils.invokeMethod(method, instance) as AgentCoreGoal
        return rawGoal.copy(name = generateName(instance, getterToPropertyName(method.name)))
    }

    private fun createCondition(
        method: Method,
        instance: Any,
    ): ComputedBooleanCondition {
        val conditionAnnotation = method.getAnnotation(Condition::class.java)
        return ComputedBooleanCondition(
            name = conditionAnnotation.name.ifBlank {
                generateName(instance, method.name)
            },
            cost = conditionAnnotation.cost,
        )
        { processContext ->
            invokeConditionMethod(
                method = method,
                instance = instance,
                processContext = processContext,
            )
        }
    }

    /**
     * Create an Action from a method
     */
    private fun createAction(
        method: Method,
        instance: Any,
        toolCallbacksOnInstance: List<ToolCallback>,
    ): MultiTransformer<Any> {
        val actionAnnotation = method.getAnnotation(Action::class.java)
        val allToolCallbacks = toolCallbacksOnInstance.toMutableList()
        val inputClasses = method.parameters
            .map { it.type }
        val inputs = method.parameters
            .filterNot {
                OperationContext::class.java.isAssignableFrom(it.type)
            }
            .map {
                val nameMatchAnnotation = it.getAnnotation(RequireNameMatch::class.java)
                expandInputBindings(if (nameMatchAnnotation != null) it.name else IoBinding.DEFAULT_BINDING, it.type)
            }
            .flatten()
        method.parameters
            .filterNot {
                OperationContext::class.java.isAssignableFrom(it.type)
            }
            .forEach {
                // Dummy tools to drive metadata. Will not be invoked.
                val parameterInstance = DummyInstanceCreator.LoremIpsum.createDummyInstance(it.type)
                allToolCallbacks += safelyGetToolCallbacksFrom(parameterInstance)
            }
        return MultiTransformer(
            name = generateName(instance, method.name),
            description = actionAnnotation.description.ifBlank { method.name },
            cost = actionAnnotation.cost,
            inputs = inputs.toSet(),
            canRerun = actionAnnotation.canRerun,
            pre = actionAnnotation.pre.toList(),
            post = actionAnnotation.post.toList(),
            inputClasses = inputClasses,
            outputClass = method.returnType as Class<Any>,
            outputVarName = actionAnnotation.outputBinding,
            toolCallbacks = allToolCallbacks.toList(),
            toolGroups = actionAnnotation.toolGroups.asList(),
        ) { context ->
            invokeActionMethod(
                method = method,
                instance = instance,
                context = context,
                toolCallbacks = toolCallbacksOnInstance,
            )
        }
    }

    private fun <O> invokeActionMethod(
        method: Method,
        instance: Any,
        context: TransformationActionContext<List<Any>, O>,
        toolCallbacks: List<ToolCallback>,
    ): O {
        logger.debug("Invoking action method {} with payload {}", method.name, context.input)
        val toolCallbacksOnDomainObjects = context.input.flatMap { safelyGetToolCallbacksFrom(it) }
        val actionToolCallbacks = toolCallbacksOnDomainObjects.toMutableList()
        // Replace dummy tools
        actionToolCallbacks += toolCallbacks.filterNot { tc -> toolCallbacksOnDomainObjects.any { it.toolDefinition.name() == tc.toolDefinition.name() } }
        var args = context.input.toTypedArray()
        if (method.parameters.any { OperationContext::class.java.isAssignableFrom(it.type) }) {
            // We need to add the payload as the last argument
            args += context
        }
        val result = try {
            ReflectionUtils.invokeMethod(method, instance, *args)
        } catch (e: CreateObjectPromptException) {
            // This is our own exception to get typesafe prompt execution
            // It is not a failure

            // TODO or default options
            val toolCallbacks =
                (actionToolCallbacks + e.toolCallbacks).distinctBy { it.toolDefinition.name() }
            val promptContributors = e.promptContributors

            val promptRunner = context.promptRunner(
                llm = e.llm ?: LlmOptions(),
                toolCallbacks = toolCallbacks,
                promptContributors = promptContributors,
            )

            if (e.requireResult) {
                promptRunner.createObject(
                    prompt = e.prompt,
                    outputClass = e.outputClass,
                )
            } else {
                promptRunner.createObjectIfPossible(
                    prompt = e.prompt,
                    outputClass = context.outputClass as Class<Any>,
                )
            }
        } catch (t: Throwable) {
            logger.warn(
                "Error invoking action method {}.{}: {}",
                instance.javaClass.name,
                method.name,
                t.message,
            )
            throw t
        }
        logger.debug(
            "Result of invoking action method {} was {}: payload {}",
            method.name,
            result,
            context.input
        )
        return result as O
    }

    private fun invokeConditionMethod(
        method: Method,
        instance: Any,
        processContext: ProcessContext,
    ): Boolean {
        logger.debug("Invoking condition method {}", method.name)
        val args = mutableListOf<Any>()
        val operationContext = OperationContext(
            operation = object : Named {
                override val name: String = method.name
            },
            processContext = processContext,
        )
        for (m in method.parameters) {
            when {
                ProcessContext::class.java.isAssignableFrom(m.type) -> {
                    args += processContext
                }

                OperationContext::class.java.isAssignableFrom(m.type) -> {
                    args += operationContext
                }
                // TODO required match binding: Consistent with actions

                else -> {
                    val domainTypes = processContext.agentProcess.agent.domainTypes
                    args += processContext.blackboard.getValue(
                        type = m.type.name,
                        domainTypes = domainTypes,
                    )
                        ?: return run {
                            // TODO assignable?
                            if (domainTypes.contains(m.type)) {
                                logger.warn(
                                    "Condition method {}.{} has no value for parameter {} of known type",
                                    instance.javaClass.name,
                                    m.name,
                                    m.type,
                                )
                            } else {
                                logger.info(
                                    "Condition method {}.{} has unsupported argument type {}",
                                    instance.javaClass.name,
                                    m.name,
                                    m.type,
                                )
                            }
                            false
                        }
                }
            }
        }
        return try {
            ReflectionUtils.invokeMethod(method, instance, *args.toTypedArray()) as Boolean
        } catch (e: EvaluateConditionPromptException) {
            // This is our own exception to get typesafe prompt execution
            // It is not a failure

            // TODO or default options
//            val toolCallbacks =
//                (actionToolCallbacks + e.toolCallbacks).distinctBy { it.toolDefinition.name() }
//            val promptContributors = e.promptContributors
//
            val promptRunner = operationContext.promptRunner(
                llm = e.llm ?: LlmOptions(),
                toolCallbacks = emptyList(),
                promptContributors = emptyList(),
            )

            promptRunner.evaluateCondition(
                condition = e.condition,
                context = e.context,
                confidenceThreshold = e.confidenceThreshold,
            )
        } catch (t: Throwable) {
            logger.warn("Error invoking condition method ${method.name} with args $args", t)
            false
        }
    }

    /**
     * If the @Action method also has an @AchievesGoal annotation,
     * create a goal from it.
     */
    private fun createGoalFromActionMethod(
        method: Method,
        action: CoreAction,
        instance: Any,
    ): AgentCoreGoal? {
        val actionAnnotation = method.getAnnotation(Action::class.java)
        val goalAnnotation = method.getAnnotation(AchievesGoal::class.java)
        if (goalAnnotation == null) {
            return null
        }
        val inputBinding = IoBinding(
            name = actionAnnotation.outputBinding,
            type = method.returnType.name,
        )
        return AgentCoreGoal(
            name = generateName(instance, method.name),
            description = goalAnnotation.description,
            inputs = setOf(inputBinding),
            value = goalAnnotation.value,
            // Add precondition of the action having run
            pre = setOf(HAS_RUN_CONDITION_PREFIX + action.name),
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
