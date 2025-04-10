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
package com.embabel.agent.toolgroups.web.search

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant

/**
 * SearchResults for any search service.
 * Returns EntityGraphReturn so will be persisted
 */
interface WebSearchResults {
    val name: String
    val request: WebSearchRequest
    val results: List<WebSearchResult>
    val timestamp: Instant
}

@JsonDeserialize(`as` = SimpleWebSearchRequest::class)
interface WebSearchRequest {
    val query: String
    val count: Int

    val offset: Int

    companion object {

        operator fun invoke(query: String, count: Int, offset: Int = 0): WebSearchRequest {
            return SimpleWebSearchRequest(query, count, offset)
        }
    }
}

private data class SimpleWebSearchRequest(
    override val query: String,
    override val count: Int,
    override val offset: Int = 0,
) : WebSearchRequest

interface WebSearchResult {
    val title: String
    val url: String
    val description: String

    val name: String
        get() = title

}

interface AbstractWebSearchService<T : WebSearchRequest, R : WebSearchResults> {

    val name: String
    val description: String

    /**
     * Must be a concrete class
     */
    val payloadType: Class<out T>

    fun search(request: T): R
}

interface WebSearchService<R : WebSearchResults> : AbstractWebSearchService<WebSearchRequest, R>
