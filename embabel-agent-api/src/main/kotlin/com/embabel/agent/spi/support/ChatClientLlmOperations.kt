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

import com.embabel.agent.core.LlmInvocation
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
import org.springframework.ai.chat.client.ResponseEntity
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import java.lang.reflect.ParameterizedType
import java.time.Duration
import java.time.Instant

/**
 * LlmOperations implementation that uses the Spring AI ChatClient
 * @param modelProvider ModelProvider to get the LLM model
 * @param toolDecorator ToolDecorator to decorate tools to make them aware of platform
 * @param templateRenderer TemplateRenderer to render templates
 * @param maybePromptTemplate Template to use for the "maybe" prompt, which
 * can enable a failure result if the LLM does not have enough information to
 * create the desired output structure.
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

        val resources = getLlmInvocationResources(interaction.llm)
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
            // Try to lock to correct overload. Method overloading is evil.
            .toolCallbacks(interaction.toolCallbacks)
            .call()
        return if (outputClass == String::class.java) {
            val chatResponse = callResponse.chatResponse()
            chatResponse?.let { recordUsage(resources.llm, it, llmRequestEvent) }
            return chatResponse!!.result.output.text as O
        } else {
            val re = callResponse.responseEntity<O>(outputClass)!!
            re.response?.let { recordUsage(resources.llm, it, llmRequestEvent) }
            return re.entity!!
        }
    }

    private fun recordUsage(
        llm: Llm,
        chatResponse: ChatResponse,
        llmRequestEvent: LlmRequestEvent<*>?,
    ) {
        logger.info("Usage is ${chatResponse.metadata.usage}")
        llmRequestEvent?.let {
            val llmi = LlmInvocation(
                llm = llm,
                usage = chatResponse.metadata.usage,
                agentName = it.agentProcess.agent.name,
                timestamp = it.timestamp,
                runningTime = Duration.between(it.timestamp, Instant.now()),
            )
            it.agentProcess.recordLlmInvocation(llmi)
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

        val resources = getLlmInvocationResources(interaction.llm)
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
        val responseEntity: ResponseEntity<ChatResponse, MaybeReturn<*>> = resources.chatClient
            .prompt(springAiPrompt)
            .toolCallbacks(interaction.toolCallbacks)
            .call()
            .responseEntity<MaybeReturn<*>>(typeReference)
        responseEntity.response?.let { recordUsage(resources.llm, it, llmRequestEvent) }
        return responseEntity.entity!!.toResult() as Result<O>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> createParameterizedTypeReference(
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

    /**
     * LLM we're calling and Spring AI ChatClient we'll use
     */
    private data class LlmInvocationResources(
        val llm: Llm,
        val chatClient: ChatClient,
    )

    private fun getLlmInvocationResources(
        llmOptions: LlmOptions,
    ): LlmInvocationResources {
        val llm = modelProvider.getLlm(llmOptions.criteria ?: byRole(ModelProvider.BEST_ROLE))
        val chatClient = ChatClient
            .builder(llm.model)
            .defaultOptions(
                // TODO this should not be OpenAI specific but we lose tools if we aren't
//                llm.optionsConverter.invoke(llmOptions)
                OpenAiChatOptions.builder()
                    .temperature(llmOptions.temperature)
                    .build()
            )
            .build()
        return LlmInvocationResources(chatClient = chatClient, llm = llm)
    }
}

/**
 * Structure to be returned by the LLM.
 * Allows the LLM to return a result structure, under success, or an error message
 * One of success or failure must be set, but not both.
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
