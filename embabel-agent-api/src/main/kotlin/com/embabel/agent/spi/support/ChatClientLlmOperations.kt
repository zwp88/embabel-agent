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
package com.embabel.agent.spi.support

import com.embabel.agent.core.support.AbstractLlmOperations
import com.embabel.agent.event.LlmRequestEvent
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.ToolDecorator
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelProvider
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.byRole
import com.embabel.common.textio.template.TemplateRenderer
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import java.lang.reflect.ParameterizedType

/**
 * LlmOperations implementation that uses the Spring AI ChatClient
 */
@Service
internal class ChatClientLlmOperations(
    private val modelProvider: ModelProvider,
    toolDecorator: ToolDecorator,
    private val templateRenderer: TemplateRenderer,
    private val maybePromptTemplate: String = "maybe_prompt_contribution",
) : AbstractLlmOperations(toolDecorator) {

    override fun <O> doTransform(
        prompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        llmRequestEvent: LlmRequestEvent<O>?,
    ): O {
        // TODO would be good to identify this ahead of time
        if (List::class.java.isAssignableFrom(outputClass)) {
            error("Output class must not be a List")
        }

        val resources = getResources(interaction.llm)
        val promptContributions =
            (interaction.promptContributors + resources.llm.promptContributors).joinToString("\n") { it.contribution() }

        val springAiPrompt = Prompt(
            buildList {
                if (promptContributions.isNotEmpty()) {
                    add(SystemMessage(promptContributions))
                }
                add(UserMessage(prompt))
            }
        )
        llmRequestEvent?.let {
            it.agentProcess.processContext.onProcessEvent(
                it.callEvent(springAiPrompt)
            )
        }

        val callResponse = resources.chatClient
            .prompt(springAiPrompt)
            .tools(interaction.toolCallbacks)
            .call()
        if (outputClass == String::class.java) {
            return callResponse.content() as O
        } else {
            return callResponse.entity<O>(outputClass)!!
        }
    }

    override fun <O> doTransformIfPossible(
        prompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
        llmRequestEvent: LlmRequestEvent<O>,
    ): Result<O> {
        val maybeReturnPromptContribution = templateRenderer.renderLoadedTemplate(
            maybePromptTemplate,
            emptyMap(),
        )

        val resources = getResources(interaction.llm)
        val promptContributions =
            (interaction.promptContributors + resources.llm.promptContributors).joinToString("\n") { it.contribution() }
        val springAiPrompt = Prompt(
            buildList {
                if (promptContributions.isNotEmpty()) {
                    add(SystemMessage(promptContributions))
                }
                add(UserMessage("$prompt\n$maybeReturnPromptContribution"))
            }
        )
        llmRequestEvent.agentProcess.processContext.onProcessEvent(
            llmRequestEvent.callEvent(springAiPrompt)
        )

        val typeReference = createParameterizedTypeReference<MaybeReturn<*>>(
            MaybeReturn::class.java,
            outputClass,
        )
        val output = resources.chatClient
            .prompt(springAiPrompt)
            .tools(interaction.toolCallbacks)
            .call()
            .entity(typeReference)!! as MaybeReturn<O>
        return output.toResult()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> createParameterizedTypeReference(
        rawType: Class<*>,
        typeArgument: Class<*>
    ): ParameterizedTypeReference<T> {
        // Create a type with proper generic information
        val type = object : ParameterizedType {
            override fun getRawType() = rawType
            override fun getActualTypeArguments() = arrayOf(typeArgument)
            override fun getOwnerType() = null
        }

        // Create a ParameterizedTypeReference that uses our custom type
        return object : ParameterizedTypeReference<T>() {
            override fun getType() = type
        }
    }

    private data class Resources(
        val llm: Llm,
        val chatClient: ChatClient,
    )

    private fun getResources(
        llmOptions: LlmOptions,
    ): Resources {
        val llm = modelProvider.getLlm(llmOptions.criteria ?: byRole(ModelProvider.BEST_ROLE))
        val chatClient = ChatClient
            .builder(llm.model)
            .defaultOptions(
                llm.optionsConverter.invoke(llmOptions)
            )
            .build()
        return Resources(chatClient = chatClient, llm = llm)
    }
}


/**
 * Allows the user to return a result or an error
 */
internal data class MaybeReturn<T>(
    val success: T? = null,
    val failure: String? = null,
) {

    fun toResult(): Result<T> {
        return if (success != null) {
            Result.success(success)
        } else {
            Result.failure(Exception(failure))
        }
    }
}
