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

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.core.support.AbstractLlmOperations
import com.embabel.agent.spi.LlmInteraction
import com.embabel.common.ai.model.ByNameModelSelectionCriteria
import com.embabel.common.ai.model.ModelProvider
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import java.lang.reflect.ParameterizedType

val DEFAULT_MAYBE_RETURN_PROMPT_CONTRIBUTION = """
    The JSON return has 2 cases: success or failure.
    If you can return the required success structure, return just the "success" property.
    If it is impossible to return this structure, return only the "failure"
    property explaining why in 10 words or less.
    NEVER MAKE ANYTHING UP. Success can only result from sufficient
    information, not subjective guesswork.
""".trimIndent()

/**
 * LlmOperations implementation that uses the Spring AI ChatClient
 */
@Service
internal class ChatClientLlmOperations(
    private val modelProvider: ModelProvider,
    private val maybeReturnPromptContribution: String = DEFAULT_MAYBE_RETURN_PROMPT_CONTRIBUTION,
) : AbstractLlmOperations() {

    override fun <I, O> doTransform(
        input: I,
        literalPrompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>,
    ): O {
        // TODO would be good to identify this ahead of time
        if (List::class.java.isAssignableFrom(outputClass)) {
            error("Output class must not be a List")
        }

        val chatClient = createChatClient(interaction.llm)

        val springAiPrompt = Prompt(literalPrompt)

        val callResponse = chatClient
            .prompt(springAiPrompt)
            .tools(interaction.toolCallbacks)
            .call()
        if (outputClass == String::class.java) {
            return callResponse.content() as O
        } else {
            return callResponse.entity<O>(outputClass)!!
        }
    }

    override fun <I, O> doTransformIfPossible(
        input: I,
        literalPrompt: String,
        interaction: LlmInteraction,
        outputClass: Class<O>
    ): Result<O> {
        val chatClient = createChatClient(interaction.llm)
        val springAiPrompt = Prompt("$literalPrompt\n$maybeReturnPromptContribution")

        val typeReference = createParameterizedTypeReference<MaybeReturn<*>>(
            MaybeReturn::class.java,
            outputClass,
        )
        val output = chatClient
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

    private fun createChatClient(
        llmOptions: LlmOptions
    ): ChatClient {
        val chatModel = modelProvider.getLlm(
            ByNameModelSelectionCriteria(
                name = llmOptions.model,
            )
        ).model
        val chatClient = ChatClient
            .builder(chatModel)
            .defaultOptions(
                // TODO should not be OpenAI specific
                OpenAiChatOptions.builder()
                    .temperature(llmOptions.temperature)
                    .build()
            )
            .build()
        return chatClient
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
