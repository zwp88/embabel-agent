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

import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.createObject
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import java.util.*

inline fun <reified T, ID> CrudRepository<T, ID>.naturalLanguageRepository(
    payload: OperationPayload,
): NaturalLanguageRepository<T> =
    SpringDataRepositoryNaturalLanguageRepository(repository = this, payload = payload)

class SpringDataRepositoryNaturalLanguageRepository<T, ID>(
    val repository: CrudRepository<T, ID>,
    val payload: OperationPayload,
) : NaturalLanguageRepository<T> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun findFromDescription(
        description: String,
        entityType: Class<T>,
    ): T? {
        val llm = LlmOptions().withModel("gpt-4o")

        // Find the finder methods on the repository
        val finderMethodsOnRepositoryTakingOneArg: List<String> =
            repository.javaClass.methods
                .filter { it.name.startsWith("find") }
                .filter { it.parameterTypes.size == 1 }
                .map { it.name }
        logger.info(
            "Eligible repository finder methods on {}: {}",
            repository.javaClass.name,
            finderMethodsOnRepositoryTakingOneArg.sorted()
        )

        val referencedEntityFieldValues = payload.promptRunner(llm).createObject<EntityFieldValues>(
            """
            Given the following description, what fields could help resolve an entity of type ${entityType.simpleName}
            You can choose from the following fields:
            ${finderMethodsOnRepositoryTakingOneArg.joinToString("\n") { "- $it" }}
            
            <description>${description}</description>
            """.trimIndent()
        )
        logger.info(
            "Found fields for {}: {}",
            entityType.simpleName,
            referencedEntityFieldValues.fields.sortedBy { it.name }
        )
        for (field in referencedEntityFieldValues.fields) {
            // Find the method on the repository
            val repositoryMethod = repository.javaClass.methods
                .firstOrNull { it.name == field.name }
                ?: continue

            logger.info(
                "Invoking repository method {} with value {}",
                repositoryMethod,
                field.value,
            )
            val result = repositoryMethod.invoke(repository, field.value)
            logger.info("Found result for {}: {}", field.name, result)
            if (result != null) {
                // Check if the result is of the expected type
                when {
                    entityType.isAssignableFrom(result.javaClass) -> {
                        return result as T
                    }

                    Optional::class.java.isAssignableFrom(result.javaClass) ->
                        return (result as Optional<*>).orElse(null) as T
                }
            }

        }
        logger.warn(
            "No matching entity found for description: {}",
            description
        )
        return null
    }
}

private data class EntityFieldValues(
    val fields: List<EntityFieldValue>,
)

private data class EntityFieldValue(
    val name: String,
    val value: String,
)
