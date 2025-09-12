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

import com.embabel.common.core.types.SimilarityCutoff
import com.embabel.common.core.types.TextSimilaritySearchRequest
import com.embabel.common.core.types.Timestamped
import com.embabel.common.core.types.ZeroToOne
import org.jetbrains.annotations.ApiStatus
import java.time.Duration
import java.time.Instant

/**
 * Narrowing of RagRequest
 */
interface RagRequestRefinement<T : RagRequestRefinement<T>> : SimilarityCutoff {

    val compressionConfig: CompressionConfig

    @get:ApiStatus.Experimental
    val entitySearch: EntitySearch?

    val desiredMaxLatency: Duration

    /**
     * Create a RagRequest from this refinement and a query.
     */
    fun toRequest(query: String): RagRequest {
        return RagRequest(
            query = query,
            similarityThreshold = similarityThreshold,
            topK = topK,
            entitySearch = entitySearch,
            compressionConfig = compressionConfig,
            desiredMaxLatency = desiredMaxLatency,
        )
    }

    // Java-friendly builders

    fun withSimilarityThreshold(similarityThreshold: ZeroToOne): T

    fun withTopK(topK: Int): T

    fun withDesiredMaxLatency(desiredMaxLatency: Duration): T

    fun withCompression(compressionConfig: CompressionConfig): T

    fun withEntitySearch(entitySearch: EntitySearch): T

}

/**
 * Controls entity search
 * Open to allow specializations
 *
 */
sealed interface EntitySearch

open class TypedEntitySearch(
    val entities: List<Class<*>>,
) : EntitySearch

open class LabeledEntitySearch(
    val labels: Set<String>,
) : EntitySearch

open class SchemaEntitySearch(
    val schema: String,
) : EntitySearch

open class CompressionConfig(
    val enabled: Boolean = true,
)

/**
 * RAG request.
 * Contains a query and parameters for similarity search.
 * @param query the query string to search for
 * @param similarityThreshold the minimum similarity score for results (default is 0.8)
 * @param topK the maximum number of results to return (default is 8)
 * If set, only the given entities will be searched for.
 */
data class RagRequest(
    override val query: String,
    override val similarityThreshold: ZeroToOne = .8,
    override val topK: Int = 8,
    override val desiredMaxLatency: Duration = Duration.ofMillis(5000),
    override val compressionConfig: CompressionConfig = CompressionConfig(),
    override val entitySearch: EntitySearch? = null,
    override val timestamp: Instant = Instant.now(),
) : TextSimilaritySearchRequest, RagRequestRefinement<RagRequest>, Timestamped {

    override fun withSimilarityThreshold(similarityThreshold: ZeroToOne): RagRequest {
        return this.copy(similarityThreshold = similarityThreshold)
    }

    override fun withTopK(topK: Int): RagRequest {
        return this.copy(topK = topK)
    }

    override fun withCompression(compressionConfig: CompressionConfig): RagRequest {
        return this.copy(compressionConfig = compressionConfig)
    }

    @ApiStatus.Experimental
    override fun withEntitySearch(entitySearch: EntitySearch): RagRequest {
        return this.copy(entitySearch = entitySearch)
    }

    override fun withDesiredMaxLatency(desiredMaxLatency: Duration): RagRequest {
        return this.copy(desiredMaxLatency = desiredMaxLatency)
    }

    companion object {

        @JvmStatic
        fun query(
            query: String,
        ): RagRequest = RagRequest(query = query)
    }
}
