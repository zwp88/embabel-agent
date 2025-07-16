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

import com.embabel.common.core.types.SimilarityResult
import com.embabel.common.core.types.TextSimilaritySearchRequest
import com.embabel.common.core.types.Timestamped
import com.embabel.common.core.types.ZeroToOne
import java.time.Instant

/**
 * Response to a RAG request.
 * Embabel RagResponses can contain results from multiple RAG services.
 * Results are not necessarily chunks, but can be entities.
 */
interface RagResponse : Timestamped {

    /**
     * RAG service that produced this result
     */
    val service: String

    val results: List<SimilarityResult<out Retrievable>>

    companion object {

        operator fun invoke(
            service: String,
            results: List<SimilarityResult<out Retrievable>>,
        ): RagResponse {
            return DefaultRagResponse(
                service = service,
                results = results,
            )
        }

    }
}

data class DefaultRagResponse(
    override val service: String,
    override val results: List<SimilarityResult<out Retrievable>>,
    override val timestamp: Instant,
) : RagResponse {

    /**
     * For use from Java
     */
    constructor (
        service: String,
        results: List<SimilarityResult<out Retrievable>>,
    ) : this(
        service = service,
        results = results,
        timestamp = Instant.now(),
    )
}


/**
 * RAG request.
 * Contains a query and parameters for similarity search.
 * @param query the query string to search for
 * @param similarityThreshold the minimum similarity score for results (default is 0.8)
 * @param topK the maximum number of results to return (default is 8)
 * @param labels optional set of labels to filter results. If not set all entities may be returned.
 * If set, only the given entities will be searched for.
 */
data class RagRequest(
    override val query: String,
    override val similarityThreshold: ZeroToOne = .8,
    override val topK: Int = 8,
    val labels: Set<String> = emptySet(),
) : TextSimilaritySearchRequest {

    fun withSimilarityThreshold(threshold: ZeroToOne): RagRequest {
        return this.copy(similarityThreshold = threshold)
    }

    fun withTopK(topK: Int): RagRequest {
        return this.copy(topK = topK)
    }

    fun matchingLabels(vararg labels: String): RagRequest {
        return this.copy(labels = this.labels + labels)
    }

    companion object {

        @JvmStatic
        fun query(
            query: String,
        ): RagRequest = RagRequest(query = query)
    }
}
