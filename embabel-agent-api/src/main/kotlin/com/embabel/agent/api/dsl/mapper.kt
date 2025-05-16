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
package com.embabel.agent.api.dsl

import com.embabel.agent.api.common.OperationContext
import kotlinx.coroutines.*

/**
 * Map parallel. Block on all results
 */
fun <T, R> Collection<T>.parallelMap(
    operationContext: OperationContext,
    concurrencyLevel: Int = 10, // Control parallelization
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (T) -> R,
): List<R> =
    runBlocking { mapAsync(operationContext, concurrencyLevel, dispatcher, transform) }


/**
 * Map async, using the agent process
 */
suspend fun <T, R> Collection<T>.mapAsync(
    context: OperationContext,
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
