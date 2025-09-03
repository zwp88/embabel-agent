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

import com.embabel.agent.api.common.support.SelfToolCallbackPublisher
import com.embabel.agent.rag.RagService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * Expose a RagService as tools. Options are stable.
 */
class RagServiceSearchTools(
    val ragService: RagService,
    val options: RagOptions,
) : SelfToolCallbackPublisher {

    @Tool(description = "Search for information relating to this query. Returns detailed results")
    fun search(
        @ToolParam(
            description = "Query to search for",
        )
        query: String,
    ): String {
        return options.ragResponseFormatter.format(ragService.search(options.toRequest(query)))
    }

}
