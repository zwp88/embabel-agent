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
    SpringDataRepositoryNaturalLanguageRepository(
        repository = this,
        entityType = T::class.java,
        payload = payload,
    )

/**
 * Implementation of [NaturalLanguageRepository] for Spring Data repositories.
 * Discovers methods on the repository that can be used to find entities.
 */
class SpringDataRepositoryNaturalLanguageRepository<T, ID>(
    val repository: CrudRepository<T, ID>,
    val entityType: Class<T>,
    val payload: OperationPayload,
) : NaturalLanguageRepository<T> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun find(
        findEntitiesRequest: FindEntitiesRequest,
    ): FindEntitiesResponse<T> {
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

        val referencedFinderInvocations = payload.promptRunner(llm).createObject<FinderInvocations>(
            """
            Given the following description, what finder methods could help resolve an entity of type ${entityType.simpleName}
            You can choose from the following finders:
            ${finderMethodsOnRepositoryTakingOneArg.joinToString("\n") { "- $it" }}
            For each finder method, return its name and the value you would use to call it.

            <description>${findEntitiesRequest.description}</description>
            """.trimIndent()
        )
        logger.info(
            "Found fields for {}: {}",
            entityType.simpleName,
            referencedFinderInvocations.fields.sortedBy { it.name }
        )

        val matches = mutableListOf<EntityMatch<T>>()

        for (finder in referencedFinderInvocations.fields) {
            // Find the method on the repository
            val repositoryMethod = repository.javaClass.methods
                .firstOrNull { it.name == finder.name }
                ?: continue

            logger.info(
                "Invoking repository method {} with value {}",
                repositoryMethod,
                finder.value,
            )
            val result = repositoryMethod.invoke(repository, finder.value)
            val maybeEntity = extractResultIfPossible(result)
            logger.info("Found result for {}: {}", finder.name, maybeEntity)
            if (maybeEntity != null) {
                matches.add(
                    EntityMatch(
                        entity = maybeEntity,
                        confidence = 1.0,
                        source = "${entityType.name}.${finder.name}",
                    )
                )
            }

        }
        if (matches.isEmpty()) {
            logger.warn(
                "No matching entities found for description: {}",
                findEntitiesRequest.description
            )
        } else {
            logger.info(
                "Found {} matches for description: {}",
                matches.size,
                findEntitiesRequest.description
            )
        }
        logger.warn(
            "No matching entities found for description: {}",
            findEntitiesRequest.description
        )
        return FindEntitiesResponse(
            request = findEntitiesRequest,
            matches = matches,
        )
    }

    private fun extractResultIfPossible(result: Any?): T? {
        return when {
            result == null -> null
            entityType.isAssignableFrom(result.javaClass) -> result as T
            Optional::class.java.isAssignableFrom(result.javaClass) ->
                (result as Optional<*>).orElse(null) as T

            else -> null
        }
    }
}


internal data class FinderInvocations(
    val fields: List<FinderInvocation>,
)

internal data class FinderInvocation(
    val name: String,
    val value: String,
)
