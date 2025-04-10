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
package com.embabel.agent.dsl

import com.embabel.agent.Action
import com.embabel.agent.ProcessContext
import com.embabel.agent.primitive.LlmOptions
import kotlinx.coroutines.*
import org.springframework.ai.tool.ToolCallback

fun <T, R> Collection<T>.mapManaged(
    processContext: ProcessContext,
    concurrencyLevel: Int = 10, // Control parallelization
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (T) -> R
): List<R> =
    runBlocking { mapAsync(processContext, concurrencyLevel, dispatcher, transform) }


/**
 * Map async, using the agent process
 */
suspend fun <T, R> Collection<T>.mapAsync(
    processContext: ProcessContext,
    concurrencyLevel: Int = 10, // Control parallelization
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    val chunkedList = chunked((size / concurrencyLevel.coerceAtLeast(1)).coerceAtLeast(1))

    chunkedList.flatMap { chunk ->
        chunk.map { item ->
            async(dispatcher) {
                transform(item)
            }
        }.awaitAll()
    }
}

inline fun <reified I, reified O : Any> ProcessContext.llmTransform(
    input: I,
    action: Action? = null,
    noinline prompt: (TransformationPayload<I, O>) -> String,
    llmOptions: LlmOptions = LlmOptions(),
    tools: List<ToolCallback> = emptyList(),
): O = llmTransform(input, this, action = action, prompt, llmOptions, tools)

inline fun <reified I, reified O : Any> TransformationPayload<I, O>.llmTransform(
    noinline prompt: (TransformationPayload<I, O>) -> String,
    llmOptions: LlmOptions = LlmOptions(),
    toolCallbacks: List<ToolCallback> = emptyList(),
): O = llmTransform<I, O>(
    input = this.input,
    processContext = this.processContext,
    prompt = prompt,
    llmOptions = llmOptions,
    toolCallbacks = toolCallbacks,
)
