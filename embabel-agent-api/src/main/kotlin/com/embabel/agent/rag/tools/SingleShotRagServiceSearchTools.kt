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
package com.embabel.agent.rag.tools

import com.embabel.agent.rag.RagService
import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * Expose a RagService as tools.
 * Once the tools instance is created,
 * options such as similarity cutoff are immutable
 * and will be used consistently in all calls.
 * The LLM needs to provide only the search query.
 */
class SingleShotRagServiceSearchTools(
    val ragService: RagService,
    val options: RagOptions,
) {

    @Tool(description = "Search for information relating to this query. Returns detailed results")
    fun search(
        @ToolParam(
            description = "Standalone query to search for. Include sufficient context",
        )
        query: String,
    ): String {
        val ragResponse = ragService.search(options.toRequest(query))
        val asString = options.ragResponseFormatter.format(ragResponse)
        loggerFor<SingleShotRagServiceSearchTools>().debug("RagResponse for query [{}]:\n{}", query, asString)
        return asString
    }

}
