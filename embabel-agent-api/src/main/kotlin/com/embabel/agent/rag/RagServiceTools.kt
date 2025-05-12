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
package com.embabel.agent.rag

import com.embabel.agent.spi.support.SelfToolCallbackPublisher
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * Expose RagService as tools
 */
interface RagServiceTools : SelfToolCallbackPublisher {

    val ragService: RagService

    val toolFormatter: RagResponseFormatter get() = SimpleRagRagResponseFormatter

    @Tool
    fun search(
        @ToolParam(
            description = "Query to search",
        )
        query: String,
    ): String {
        return toolFormatter.format(ragService.search(RagRequest(query)))
    }

}

fun interface RagResponseFormatter {
    fun format(ragResponse: RagResponse): String
}

val SimpleRagRagResponseFormatter = RagResponseFormatter { ragResponse ->
    val results = ragResponse.results
    if (results.isEmpty()) {
        "No results found"
    } else {
        results.joinToString(separator = "\n\n") { result ->
            when (val match = result.match) {
                is EntityData -> {
                    val properties = match.properties.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    "${result.score}: ${match.infoString(verbose = true)} ($properties)"
                }

                is Chunk -> {
                    "${result.score}: $match"
                }
            }
        }
    }
}
