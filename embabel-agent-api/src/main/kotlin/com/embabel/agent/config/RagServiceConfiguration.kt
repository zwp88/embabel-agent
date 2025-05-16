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

import com.embabel.agent.common.Constants
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.rag.Ingester
import com.embabel.agent.rag.RagService
import com.embabel.agent.rag.WritableRagService
import com.embabel.agent.rag.support.ConsensusRagService
import com.embabel.agent.rag.support.MultiIngester
import com.embabel.agent.rag.support.SpringVectorStoreRagService
import com.embabel.agent.rag.tools.RagServiceTools
import com.embabel.common.core.types.Semver
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class RagServiceConfiguration {

    @Bean
    @ConditionalOnBean(VectorStore::class)
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

    /**
     * Default RAG tool group
     */
    @Bean
    fun ragToolGroup(ragService: RagService): ToolGroup {
        return ToolGroup(
            metadata = ToolGroupMetadata(
                description = ToolGroupDescription(
                    description = "RAG service",
                    role = "rag",
                ),
                name = "rag",
                provider = Constants.EMBABEL_PROVIDER,
                version = Semver("1.0.0"),
                permissions = setOf(),
            ),
            toolCallbacks = RagServiceTools(
                ragService = ragService,
            ).toolCallbacks,
        )
    }

    @Bean
    @Primary
    fun ingester(
        ragServices: List<WritableRagService>,
    ): Ingester {
        return MultiIngester(ragServices)
    }

}
