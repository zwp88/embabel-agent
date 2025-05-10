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

import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.createObject
import com.embabel.agent.domain.persistence.EntityMatch
import com.embabel.agent.domain.persistence.FindEntitiesRequest
import com.embabel.agent.domain.persistence.FindEntitiesResponse
import com.embabel.agent.domain.persistence.NaturalLanguageRepository
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import java.util.*

inline fun <reified T, ID> CrudRepository<T, ID>.naturalLanguageRepository(
    context: ActionContext,
    llm: LlmOptions,
): NaturalLanguageRepository<T> =
    SpringDataRepositoryNaturalLanguageRepository(
        repository = this,
        entityType = T::class.java,
        context = context,
        llm = llm,
    )

/**
 * Implementation of [NaturalLanguageRepository] for Spring Data repositories.
 * Discovers methods on the repository that can be used to find entities.
 */
class SpringDataRepositoryNaturalLanguageRepository<T, ID>(
    val repository: CrudRepository<T, ID>,
    val entityType: Class<T>,
    val context: ActionContext,
    val llm: LlmOptions,
) : NaturalLanguageRepository<T> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun find(
        findEntitiesRequest: FindEntitiesRequest,
    ): FindEntitiesResponse<T> {

        // Find the finder methods on the repository
        val finderMethodsOnRepositoryTakingOneArg: List<String> =
            repository.javaClass.methods
                .filter { it.name.startsWith("find") }
                // TODO questionable
                .filterNot { it.name.startsWith("findAll") }
                .filter { it.parameterTypes.size == 1 }
                .map { it.name }
                .distinct()
        logger.info(
            "Eligible repository finder methods on {}: {}",
            repository.javaClass.name,
            finderMethodsOnRepositoryTakingOneArg.sorted()
        )

        val referencedFinderInvocations = context.promptRunner(llm).createObject<FinderInvocations>(
            """
            Given the following description, what finder methods could help resolve an entity of type ${entityType.simpleName}
            You can choose from the following finders:
            ${finderMethodsOnRepositoryTakingOneArg.joinToString("\n") { "- $it" }}
            For each finder method, return its name and the value you would use to call it.
            Remember that findById might work with one of the fields.

            <description>${findEntitiesRequest.description}</description>
            """.trimIndent()
        )
        logger.info(
            "Found finder methods for {}: {}",
            entityType.simpleName,
            referencedFinderInvocations.invocations.sortedBy { it.name }
        )

        val matches = mutableListOf<EntityMatch<T>>()

        for (finder in referencedFinderInvocations.invocations) {
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
            logger.debug("Found result for {}: {}", finder.name, maybeEntity)
            if (maybeEntity != null) {
                matches.add(
                    EntityMatch(
                        match = maybeEntity,
                        score = 1.0,
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

    @Suppress("UNCHECKED_CAST")
    private fun extractResultIfPossible(result: Any?): T? {
        return when {
            result == null -> null
            result is Iterable<*> -> result.firstOrNull() as T?
            entityType.isAssignableFrom(result.javaClass) -> result as T
            Optional::class.java.isAssignableFrom(result.javaClass) ->
                (result as Optional<*>).orElse(null) as T

            else -> null
        }
    }
}


internal data class FinderInvocations(
    val invocations: List<FinderInvocation>,
)

internal data class FinderInvocation(
    val name: String,
    val value: String,
)
