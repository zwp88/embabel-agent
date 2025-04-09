package com.embabel.agent.toolgroups.web.search

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant

/**
 * SearchResults for any search service.
 * Returns EntityGraphReturn so will be persisted
 */
interface WebSearchResults  {
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

data class SimpleWebSearchRequest(
    override val query: String,
    override val count: Int,
    override val offset: Int = 0,
) : WebSearchRequest

interface WebSearchResult  {
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