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
import com.embabel.agent.toolgroups.web.search.WebSearchResult
import com.embabel.agent.toolgroups.web.search.WebSearchResults
import com.embabel.agent.toolgroups.web.search.WebSearchService
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant

/**
 * Common base class for Brave search services,
 * whether web, news, or video.
 */
abstract class BraveSearchService(
    override val name: String,
    override val description: String,
    @Value("\${BRAVE_API_KEY}") private val apiKey: String,
    private val baseUrl: String,
    private val restTemplate: RestTemplate,
) : WebSearchService<BraveSearchResults> {

    override val payloadType: Class<out WebSearchRequest> = WebSearchRequest::class.java

    override fun search(request: WebSearchRequest): BraveSearchResults {
        val headers = HttpHeaders().apply {
            set("X-Subscription-Token", apiKey)
            set("Accept", "application/json")
        }

        val entity = HttpEntity<String>(headers)

        val rawResponse = restTemplate.exchange(
            "$baseUrl?q={query}&count={count}&offset={offset}",
            HttpMethod.GET,
            entity,
            BraveResponse::class.java,
            mapOf(
                "query" to request.query,
                "count" to request.count,
                "offset" to request.offset,
            ),
        ).body ?: throw RuntimeException("No response body")
        return rawResponse.toBraveSearchResults(request)
    }
}

@ConditionalOnProperty("BRAVE_API_KEY")
@Service
class BraveWebSearchService(
    @Value("\${BRAVE_API_KEY}") apiKey: String,
    restTemplate: RestTemplate = RestTemplate()
) : BraveSearchService(
    name = "Brave web search",
    description = "Search the web with Brave",
    apiKey = apiKey,
    baseUrl = "https://api.search.brave.com/res/v1/web/search",
    restTemplate = restTemplate,
)

@ConditionalOnProperty("BRAVE_API_KEY")
@Service
class BraveNewsSearchService(
    @Value("\${BRAVE_API_KEY}") apiKey: String,
    restTemplate: RestTemplate
) : BraveSearchService(
    name = "Brave news search",
    description = "Search for news with Brave",
    apiKey = apiKey,
    baseUrl = "https://api.search.brave.com/res/v1/news/search",
    restTemplate = restTemplate,
)

@ConditionalOnProperty("BRAVE_API_KEY")
@Service
class BraveVideoSearchService(
    @Value("\${BRAVE_API_KEY}") apiKey: String,
    restTemplate: RestTemplate
) : BraveSearchService(
    name = "Brave video search",
    description = "Search for videos with Brave",
    apiKey = apiKey,
    baseUrl = "https://api.search.brave.com/res/v1/videos/search",
    restTemplate = restTemplate,
)

data class BraveSearchResults(
    override val request: WebSearchRequest,
    val query: Query,
    override val results: List<BraveSearchResult>,
    override val timestamp: Instant = Instant.now(),
) : WebSearchResults {

    override val name: String
        get() = "Brave search results for query: ${query.original}"
}


data class BraveSearchResult(
    override val title: String,
    override val url: String,
    override val description: String,
) : WebSearchResult

data class Query(
    val original: String
)

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(
    JsonSubTypes.Type(value = BraveWebSearchResponse::class),
    JsonSubTypes.Type(value = BraveNewsSearchResponse::class),
)
internal interface BraveResponse {
    val query: Query
    fun toBraveSearchResults(request: WebSearchRequest): BraveSearchResults
}

internal data class BraveWebSearchResponse(
    val web: WebResults,
    override val query: Query
) : BraveResponse {

    override fun toBraveSearchResults(request: WebSearchRequest): BraveSearchResults {
        return BraveSearchResults(
            request = request,
            query = query,
            results = web.results,
        )
    }
}

internal data class WebResults(
    val results: List<BraveSearchResult>
)

internal data class BraveNewsSearchResponse(
    val results: List<BraveSearchResult>,
    override val query: Query
) : BraveResponse {

    override fun toBraveSearchResults(request: WebSearchRequest): BraveSearchResults {
        return BraveSearchResults(
            request = request,
            query = query,
            results = results,
        )
    }
}
