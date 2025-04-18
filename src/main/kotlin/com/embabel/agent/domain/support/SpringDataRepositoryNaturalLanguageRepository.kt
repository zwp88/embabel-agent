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
package com.embabel.agent.domain.support

import com.embabel.agent.spi.LlmOperations
import org.springframework.data.repository.CrudRepository

inline fun <reified T, ID> CrudRepository<T, ID>.naturalLanguageRepository(
    llmOperations: LlmOperations,
): NaturalLanguageRepository<T> =
    SpringDataRepositoryNaturalLanguageRepository(repository = this, llmOperations = llmOperations)

class SpringDataRepositoryNaturalLanguageRepository<T, ID>(
    val repository: CrudRepository<T, ID>,
    val llmOperations: LlmOperations,
) : NaturalLanguageRepository<T> {

    override fun findFromDescription(
        description: String,
        entityType: Class<T>,
    ): T? {
        // Find the finder methods

        // Find the entity identification

        println("Description: $description")
        return repository.findAll().first()
    }
}
