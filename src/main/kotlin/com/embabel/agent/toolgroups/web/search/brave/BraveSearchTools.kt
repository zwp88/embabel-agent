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
package com.embabel.agent.toolgroups.web.search.brave

import com.embabel.agent.toolgroups.web.search.WebSearchRequest
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

fun braveSearchTools(
    braveWebSearchService: BraveWebSearchService,
    braveNewsSearchService: BraveNewsSearchService,
    braveVideoSearchService: BraveVideoSearchService,
): List<ToolCallback> =
    ToolCallbacks.from(
        BraveSearchTools(
            braveWebSearchService,
            braveNewsSearchService,
            braveVideoSearchService,
        )
    ).toList()

/**
 * Expose the various Brave searches as Spring AI tools
 */
private class BraveSearchTools(
    private val braveWebSearchService: BraveWebSearchService,
    private val braveNewsSearchService: BraveNewsSearchService,
    private val braveVideoSearchService: BraveVideoSearchService,
) {

    // TODO duplication is because Spring AI doesn't support
    // metadata annotations on parameter types
    @Tool(description = "Brave web search")
    private fun webSearch(
        @ToolParam(description = "Search query") query: String,
        @ToolParam(description = "results to return. maximum 20") count: Int = 10,
        @ToolParam(description = "page offset, used with count. starts at 0 & increments by 1 for each page") offset: Int = 0
    ): BraveSearchResults {
        return braveWebSearchService.search(WebSearchRequest(query, count, offset))
    }

    @Tool(description = "Brave news search")
    private fun newsSearch(
        @ToolParam(description = "Search query") query: String,
        @ToolParam(description = "results to return. maximum 20") count: Int = 10,
        @ToolParam(description = "page offset, used with count. starts at 0 & increments by 1 for each page") offset: Int = 0
    ): BraveSearchResults {
        return braveNewsSearchService.search(WebSearchRequest(query, count, offset))
    }

    @Tool(description = "Brave video search")
    private fun videoSearch(
        @ToolParam(description = "Search query") query: String,
        @ToolParam(description = "results to return. maximum 20") count: Int = 10,
        @ToolParam(description = "page offset, used with count. starts at 0 & increments by 1 for each page") offset: Int = 0
    ): BraveSearchResults {
        return braveVideoSearchService.search(WebSearchRequest(query, count, offset))
    }
}
