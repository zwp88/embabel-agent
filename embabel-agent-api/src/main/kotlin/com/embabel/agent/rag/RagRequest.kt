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
import com.embabel.common.core.types.SimilarityResult
import com.embabel.common.core.types.ZeroToOne

/**
 * There can be multiple RAG services.
 */
interface RagResponse {

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
            return RagResponseImpl(
                service = service,
                results = results,
            )
        }
    }
}

private data class RagResponseImpl(
    override val service: String,
    override val results: List<SimilarityResult<out Retrievable>>,
) : RagResponse


data class RagRequest(
    val query: String,
    override val similarityThreshold: ZeroToOne = .8,
    override val topK: Int = 8,
) : SimilarityCutoff
