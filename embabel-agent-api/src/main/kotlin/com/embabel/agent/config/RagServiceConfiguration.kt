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
package com.embabel.agent.config

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.rag.ConsensusRagService
import com.embabel.agent.rag.Ingester
import com.embabel.agent.rag.MultiIngester
import com.embabel.agent.rag.RagService
import com.embabel.agent.rag.springvector.SpringVectorStoreRagService
import com.embabel.agent.toolgroups.rag.RagTools
import org.neo4j.driver.Driver
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class RagServiceConfiguration {

    @Bean
    fun defaultSpringVectorStore(vectorStore: VectorStore): RagService {
        return SpringVectorStoreRagService(vectorStore)
    }

    @Bean
    @Primary
    fun consensusRagService(
        ragServices: List<RagService>,
    ): RagService {
        return ConsensusRagService(ragServices)
    }

    @Bean
    fun ragToolGroup(ragService: RagService): ToolGroup {
        return ToolGroup(
            metadata = ToolGroupMetadata(
                description = ToolGroupDescription(
                    description = "RAG service",
                    role = "rag",
                ),
                artifact = "rag",
                provider = "embabel",
                version = "1.0.0",
                permissions = setOf(),
            ),
            toolCallbacks = RagTools(
                ragService = ragService,
            ).toolCallbacks,
        )
    }

    @Bean
    @Primary
    fun ingester(
        ragServices: List<RagService>,
    ): Ingester {
        return MultiIngester(ragServices)
    }

    @Bean
    fun vectorStore(driver: Driver, embeddingModel: EmbeddingModel): VectorStore {
        return Neo4jVectorStore.builder(
            driver,
            embeddingModel,
        )
            .build()
    }
}
