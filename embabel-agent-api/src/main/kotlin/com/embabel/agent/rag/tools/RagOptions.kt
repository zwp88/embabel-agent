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

import com.embabel.agent.event.RagEventListener
import com.embabel.agent.rag.*
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.ZeroToOne
import java.time.Duration

data class DualShotConfig(
    val summaryWords: Int = 100,
)

/**
 * Operations for RAG use as an LLM tool. Options are immutable and stable.
 * @param similarityThreshold minimum similarity threshold for results (0.0 to 1.0)
 * @param topK maximum number of results to return
 * returned. If set, only the given entities will be searched for.
 * @param ragResponseFormatter formatter to convert RagResponse to String
 * @param service optional name of the RAG service to use. If null, the default service will be used.
 * @param dualShot whether to use dual-shot RAG,
 * where the first tool returns a summary and the second tool returns detailed results.
 */
data class RagOptions @JvmOverloads constructor(
    override val similarityThreshold: ZeroToOne = 0.7,
    override val topK: Int = 8,
    override val desiredMaxLatency: Duration = Duration.ofMillis(5000),
    override val compressionConfig: CompressionConfig = CompressionConfig(),
    val llm: LlmOptions = LlmOptions.withAutoLlm(),
    override val entitySearch: EntitySearch? = null,
    val ragResponseFormatter: RagResponseFormatter = SimpleRagResponseFormatter,
    val service: String? = null,
    val listener: RagEventListener = RagEventListener.NOOP,
    val dualShot: DualShotConfig? = null,
) : RagRequestRefinement<RagOptions> {

    override fun withSimilarityThreshold(similarityThreshold: ZeroToOne): RagOptions {
        return copy(similarityThreshold = similarityThreshold)
    }

    override fun withTopK(topK: Int): RagOptions {
        return copy(topK = topK)
    }

    override fun withDesiredMaxLatency(desiredMaxLatency: Duration): RagOptions {
        return copy(desiredMaxLatency = desiredMaxLatency)
    }

    override fun withCompression(compressionConfig: CompressionConfig): RagOptions {
        return copy(compressionConfig = compressionConfig)
    }

    override fun withEntitySearch(entitySearch: EntitySearch): RagOptions {
        return copy(entitySearch = entitySearch)
    }

    /**
     * Return an instance using the given Rag service name.
     */
    fun withService(service: String): RagOptions {
        return copy(service = service)
    }

    fun withListener(listener: RagEventListener): RagOptions {
        return copy(listener = this.listener + listener)
    }

    fun withDualShot(dualShot: DualShotConfig): RagOptions {
        return copy(dualShot = dualShot)
    }

}
