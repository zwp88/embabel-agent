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

import com.embabel.common.core.StableIdentified
import com.embabel.common.core.types.*
import io.swagger.v3.oas.annotations.media.Schema

sealed interface Retrieved : HasInfoString

interface Chunk : Retrieved {
    val text: String

    companion object {
        operator fun invoke(text: String): Chunk {
            return ChunkImpl(
                text = text,
            )
        }
    }

    override fun infoString(verbose: Boolean?): String {
        return "chunk: $text"
    }
}

private data class ChunkImpl(
    override val text: String,
) : Chunk

interface EntityData : StableIdentified, Retrieved, Described {

    @get:Schema(
        description = "description of this entity",
        example = "A customer",
        required = true,
    )
    override val description: String

    /**
     * Labels of the entity. In Neo, this might include multiple labels.
     * In a relational database, this might be a single table name.
     */
    @get:Schema(
        description = "Labels of the entity. In Neo, this might include multiple labels. In a relational database, this might be a single table name.",
        example = "[\"Person\", \"Customer\"]",
        required = true,
    )
    val labels: Set<String>

    @get:Schema(
        description = "Properties of this object. Arbitrary key-value pairs, although likely specified in schema. Must filter out embedding",
        example = "{\"birthYear\": 1854, \"deathYear\": 1930}",
        required = true,
    )
    val properties: Map<String, Any>

    override fun infoString(verbose: Boolean?): String {
        val labelsString = labels.joinToString(":")
        return "(${labelsString} id='$id')"
    }

}

/**
 * There can be multiple RAG services.
 */
interface RagResponse {

    /**
     * RAG service that produced this result
     */
    val service: String

    val results: List<SimilarityResult<out Retrieved>>

    companion object {

        operator fun invoke(
            service: String,
            results: List<SimilarityResult<out Retrieved>>,
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
    override val results: List<SimilarityResult<out Retrieved>>,
) : RagResponse


data class RagRequest(
    val content: String,
    override val similarityThreshold: ZeroToOne = .8,
    override val topK: Int = 8,
) : SimilarityCutoff

interface RagService : Described, HasInfoString {
    val name: String

    fun search(ragRequest: RagRequest): RagResponse

    companion object {
        fun empty(
            name: String = "empty",
            description: String = "empty",
        ): RagService {
            return EmptyRagService(
                name = name,
                description = description,
            )
        }
    }
}

private data class EmptyRagService(
    override val name: String,
    override val description: String,
) : RagService {
    override fun search(ragRequest: RagRequest): RagResponse {
        return RagResponse(
            service = name,
            results = emptyList(),
        )
    }

    override fun infoString(verbose: Boolean?): String {
        return "Empty RAG service: $name"
    }
}
