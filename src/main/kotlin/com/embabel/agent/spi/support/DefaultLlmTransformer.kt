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

import com.embabel.agent.AbstractLlmTransformer
import com.embabel.agent.primitive.LlmOptions
import com.embabel.common.ai.model.ByNameModelSelectionCriteria
import com.embabel.common.ai.model.ModelProvider
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Service

@Service
class DefaultLlmTransformer(
    private val modelProvider: ModelProvider,
) : AbstractLlmTransformer() {

    override fun <I, O> doTransform(
        input: I,
        literalPrompt: String,
        llmOptions: LlmOptions,
        allToolCallbacks: List<ToolCallback>,
        outputClass: Class<O>,
    ): O {

        // TODO would be good to identify this ahead of time
        if (List::class.java.isAssignableFrom(outputClass)) {
            error("Output class must not be a List")
        }

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

        val springAiPrompt = Prompt(literalPrompt)

        val output = chatClient
            .prompt(springAiPrompt)
            .tools(allToolCallbacks)
            .call()
            .entity<O>(outputClass)!!
        return output
    }
}
