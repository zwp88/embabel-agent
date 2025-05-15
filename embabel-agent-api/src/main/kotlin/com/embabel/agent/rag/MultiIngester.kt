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

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.reader.TextReader
import org.springframework.ai.transformer.splitter.TextSplitter
import org.springframework.ai.transformer.splitter.TokenTextSplitter

/**
 * Write to all RAG services that implement [WritableRagService].
 */
class MultiIngester(
    override val ragServices: List<WritableRagService>,
    private val splitter: TextSplitter = TokenTextSplitter.builder().withChunkSize(800).build(),
) : Ingester {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun ingest(resourceUrl: String): IngestionResult {
        val sourceDocs = TextReader(resourceUrl).get()
        val documents = splitter.split(sourceDocs)
        logger.info("Split {} into {} documents from {}", sourceDocs.size, documents.size, resourceUrl)
        return writeToStores(documents)
    }

    override fun accept(documents: List<Document>) {
        writeToStores(documents)
    }

    private fun writeToStores(documents: List<Document>): IngestionResult {
        val storesWrittenTo = ragServices
            .map {
                it.write(documents)
                it.name
            }
        return IngestionResult(
            documentsWritten = documents.size,
            storesWrittenTo = storesWrittenTo.toSet(),
        )
    }
}
