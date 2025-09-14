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
package com.embabel.agent.api.common

import com.embabel.agent.api.annotation.support.AgenticInfo
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupRequirement
import com.embabel.agent.prompt.element.ContextualPromptElement
import com.embabel.agent.rag.tools.RagOptions
import com.embabel.agent.spi.LlmUse
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.ai.prompt.PromptElement
import com.embabel.common.util.loggerFor
import org.jetbrains.annotations.ApiStatus

/**
 * Define a handoff to a subagent.

 */
class Subagent private constructor(
    private val agentRef: Any,
    val inputClass: Class<*>,
) {

    /**
     * Subagent that is an agent
     * @param agent the subagent to hand off to
     * @param inputClass the class of the input that the subagent expects
     */
    constructor(
        agent: Agent,
        inputClass: Class<*>,
    ) : this(
        agentRef = agent,
        inputClass = inputClass,
    )

    constructor(
        agentName: String,
        inputClass: Class<*>,
    ) : this(
        agentRef = agentName,
        inputClass = inputClass,
    )

    /**
     * Reference to an annotated agent class.
     */
    constructor(
        agentType: Class<*>,
        inputClass: Class<*>,
    ) : this(
        agentRef = agentType,
        inputClass = inputClass,
    )

    fun resolve(agentPlatform: AgentPlatform): Agent {
        return when (agentRef) {
            is Agent -> agentRef
            is String -> agentPlatform.agents().find { it.name == agentRef }
                ?: throw IllegalArgumentException(
                    "Subagent with name '$agentRef' not found in platform ${agentPlatform.name}. " +
                            "Available agents: ${agentPlatform.agents().map { it.name }}"
                )

            is Class<*> -> {
                val agenticInfo = AgenticInfo(agentRef)
                if (!agenticInfo.agentic()) {
                    throw IllegalArgumentException(
                        "Subagent must be an Agent or a String representing the agent name, but was: $agentRef"
                    )
                }
                agentPlatform.agents().find { it.name == agenticInfo.agentName() }
                    ?: throw IllegalArgumentException(
                        "Subagent of type $agentRef with name '$agentRef' not found in platform ${agentPlatform.name}. " +
                                "Available agents: ${agentPlatform.agents().map { it.name }}"
                    )
            }

            else -> throw IllegalArgumentException(
                "Subagent must be an Agent or a String representing the agent name, but was: $agentRef"
            )
        }
    }
}


/**
 * User code should always use this interface to execute prompts.
 * Typically obtained from an [OperationContext] or [ActionContext] parameter,
 * via [OperationContext.ai]
 * A PromptRunner is immutable once constructed, and has determined
 * LLM and hyperparameters. Use the "with" methods to evolve
 * the state to your desired configuration before executing createObject,
 * generateText or other LLM invocation methods.
 * Thus, a PromptRunner can be reused within an action implementation.
 * A contextual facade to LlmOperations.
 * @see com.embabel.agent.spi.LlmOperations
 */
interface PromptRunner : LlmUse, PromptRunnerOperations {

    /**
     * Additional objects with @Tool annotation for use in this PromptRunner
     */
    val toolObjects: List<ToolObject>

    /**
     * Specify an LLM for the PromptRunner
     */
    fun withLlm(llm: LlmOptions): PromptRunner

    /**
     * Add a tool group to the PromptRunner
     * @param toolGroup name of the toolGroup we're requesting
     * @return PromptRunner instance with the added tool group
     */
    fun withToolGroup(toolGroup: String): PromptRunner =
        withToolGroup(ToolGroupRequirement(toolGroup))

    /**
     * Allows for dynamic tool groups to be added to the PromptRunner.
     */
    fun withToolGroup(toolGroup: ToolGroup): PromptRunner

    fun withToolGroups(toolGroups: Set<String>): PromptRunner =
        toolGroups.fold(this) { acc, toolGroup -> acc.withToolGroup(toolGroup) }

    /**
     * Add a set of tool groups to the PromptRunner
     * @param toolGroups the set of named tool groups to add
     */
    fun withTools(vararg toolGroups: String): PromptRunner =
        withToolGroups(toolGroups.toSet())

    fun withToolGroup(toolGroup: ToolGroupRequirement): PromptRunner

    /**
     * Add a tool object to the prompt runner.
     * @param toolObject the object to add. If it is null, nothing is done.
     * This is not an error
     * @return PromptRunner instance with the added tool object
     */
    fun withToolObject(toolObject: Any?): PromptRunner =
        if (toolObject == null) {
            this
        } else {
            withToolObject(ToolObject.from(toolObject))
        }

    /**
     * Add a tool object
     * @param toolObject the object to add.
     */
    fun withToolObject(
        toolObject: ToolObject,
    ): PromptRunner

    fun withToolObjects(vararg toolObjects: Any?): PromptRunner =
        toolObjects.fold(this) { acc, toolObject -> acc.withToolObject(toolObject) }

    /**
     * Add tools for RAG. Will use platform RagService
     * @param options options for the RAG tools. Control similarity threshold, topK, labels, and response formatting.
     */
    @ApiStatus.Experimental
    fun withRag(options: RagOptions): PromptRunner

    /**
     * Add a reference which provides tools and prompt contribution.
     */
    fun withReference(reference: LlmReference): PromptRunner {
        return withToolObject(reference.toolObject())
            .withPromptContributor(reference)
    }

    /**
     * Add a list of references which provide tools and prompt contributions.
     */
    fun withReferences(references: List<LlmReference>): PromptRunner {
        return references.fold(this) { acc, reference -> acc.withReference(reference) }
    }

    /**
     * Add varargs of references which provide tools and prompt contributions.
     */
    fun withReferences(vararg references: LlmReference): PromptRunner =
        withReferences(references.toList())

    /**
     * Add a list of handoffs to agents on this platform
     * @param outputTypes the types of objects that can result from output flow
     */
    @ApiStatus.Experimental
    fun withHandoffs(
        vararg outputTypes: Class<*>,
    ): PromptRunner

    /**
     * Add a list of subagents to hand off to.
     */
    @ApiStatus.Experimental
    fun withSubagents(
        vararg subagents: Subagent,
    ): PromptRunner

    /**
     * Add a literal system prompt
     */
    fun withSystemPrompt(systemPrompt: String): PromptRunner =
        withPromptContributor(
            PromptContributor.fixed(systemPrompt)
        )

    /**
     * Add a prompt contributor that can add to the prompt.
     * Facilitates reuse of prompt elements.
     * @param promptContributor
     * @return PromptRunner instance with the added PromptContributor
     */
    fun withPromptContributor(promptContributor: PromptContributor): PromptRunner =
        withPromptContributors(listOf(promptContributor))

    fun withPromptContributors(promptContributors: List<PromptContributor>): PromptRunner

    /**
     * Add varargs of prompt contributors and contextual prompt elements.
     */
    fun withPromptElements(vararg elements: PromptElement): PromptRunner {
        val promptContributors = elements.filterIsInstance<PromptContributor>()
        val contextualPromptElements = elements.filterIsInstance<ContextualPromptElement>()
        val oddOnesOut = elements.filterNot { it is PromptContributor || it is ContextualPromptElement }
        if (oddOnesOut.isNotEmpty()) {
            loggerFor<PromptRunner>().warn(
                "{} arguments to withPromptElements were not prompt contributors or contextual prompt elements and will be ignored: {}",
                oddOnesOut.size,
                oddOnesOut.joinToString(
                    ", ", prefix = "[", postfix = "]"
                )
            )
        }
        return withPromptContributors(promptContributors)
            .withContextualPromptContributors(contextualPromptElements)
    }

    /**
     * Add a prompt contributor that can see context
     */
    fun withContextualPromptContributors(
        contextualPromptContributors: List<ContextualPromptElement>,
    ): PromptRunner

    fun withContextualPromptContributor(
        contextualPromptContributor: ContextualPromptElement,
    ): PromptRunner =
        withContextualPromptContributors(listOf(contextualPromptContributor))

    /**
     * Set whether to generate examples of the output in the prompt
     * on a per-PromptRunner basis. This overrides platform defaults.
     * Note that adding individual examples with [ObjectCreator.withExample]
     * will always override this.
     */
    fun withGenerateExamples(generateExamples: Boolean): PromptRunner

    /**
     * Create an object creator for the given output class.
     * Allows setting strongly typed examples.
     */
    fun <T> creating(outputClass: Class<T>): ObjectCreator<T>

}

/**
 * Create an object of the given type
 */
inline infix fun <reified T> PromptRunner.createObject(prompt: String): T =
    createObject(prompt, T::class.java)

/**
 * Create an object of the given type.
 * Method overloading is evil
 **/
inline infix fun <reified T> PromptRunner.create(prompt: String): T =
    createObject(prompt, T::class.java)

inline fun <reified T> PromptRunner.createObjectIfPossible(prompt: String): T? =
    createObjectIfPossible(prompt, T::class.java)

inline fun <reified T> TemplateOperations.createObject(
    model: Map<String, Any>,
): T =
    createObject(outputClass = T::class.java, model = model)
