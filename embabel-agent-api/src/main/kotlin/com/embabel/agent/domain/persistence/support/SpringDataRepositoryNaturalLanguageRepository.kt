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
package com.embabel.agent.domain.persistence.support

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.createObject
import com.embabel.agent.domain.persistence.EntityMatch
import com.embabel.agent.domain.persistence.FindEntitiesRequest
import com.embabel.agent.domain.persistence.FindEntitiesResponse
import com.embabel.agent.domain.persistence.NaturalLanguageRepository
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.util.loggerFor
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import java.util.*

inline fun <reified T, ID> CrudRepository<T, ID>.naturalLanguageRepository(
    noinline idGetter: (T) -> ID?,
    context: OperationContext,
    llm: LlmOptions,
): NaturalLanguageRepository<T> =
    SpringDataRepositoryNaturalLanguageRepository(
        repository = this,
        entityType = T::class.java,
        idGetter = idGetter,
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
    private val idGetter: (T) -> ID?,
    val context: OperationContext,
    val llm: LlmOptions,
) : NaturalLanguageRepository<T> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun find(
        findEntitiesRequest: FindEntitiesRequest,
    ): FindEntitiesResponse<T> {

        // Find the finder methods on the repository
        val finderMethodsOnRepository: List<String> =
            repository.javaClass.methods
                .filter { it.name.startsWith("find") }
                .filter { it.parameterTypes.size >= 1 }
                .map { it.name }
                .distinct()
        logger.info(
            "Eligible repository finder methods on {}: {}",
            repository.javaClass.name,
            finderMethodsOnRepository.sorted(),
        )

        val finderInvocations = context.promptRunner(llm).createObject<FinderInvocations>(
            """
            Given the following description, what finder methods could help resolve an entity of type ${entityType.simpleName}
            You can choose from the following finders, which follow Spring Data conventions:
            ${finderMethodsOnRepository.joinToString("\n") { "- $it" }}
            For each finder method, return its name and the values you would use to call it.
            Remember that findById might work with one of the fields.

            <description>${findEntitiesRequest.content}</description>
            """.trimIndent()
        )
        logger.info(
            "Found finder invocations for {}: {}",
            entityType.simpleName,
            finderInvocations.invocations().sortedBy { it.name },
        )

        val matches = invokeFinders(findEntitiesRequest, finderInvocations)

        return FindEntitiesResponse(
            request = findEntitiesRequest,
            matches = matches,
        )
    }

    private fun invokeFinders(
        findEntitiesRequest: FindEntitiesRequest,
        finderInvocations: FinderInvocations
    ): List<EntityMatch<T>> {
        val allMatches = mutableListOf<EntityMatch<T>>()
        for (finder in finderInvocations.invocations()) {
            val repositoryMethod = repository.javaClass.methods
                .firstOrNull { it.name == finder.name }
                ?: continue

            logger.info(
                "Invoking repository method {} with values {}",
                repositoryMethod,
                finder.args,
            )
            val result = repositoryMethod.invoke(repository, *finder.args.values.toTypedArray())
            val maybeEntity = extractResultIfPossible(result)
            logger.debug("Found result for {}: {}", finder.name, maybeEntity)
            if (maybeEntity != null) {
                allMatches.add(
                    EntityMatch(
                        match = maybeEntity,
                        score = 1.0,
                        source = "${entityType.name}.${finder.name}",
                    )
                )
            }
        }
        val matches = allMatches.distinctBy { idGetter(it.match) }
        if (matches.isEmpty()) {
            logger.warn(
                "No matching entities found for description: {}",
                findEntitiesRequest.content
            )
        } else {
            logger.info(
                "Found {} matches for description: {}",
                matches.size,
                findEntitiesRequest.content
            )
        }
        return matches
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

/**
 * Returned by LLM. Contains desired invocations of finder methods
 */
internal data class FinderInvocations(
    private val invocations: List<FinderInvocation>,
) {

    /**
     * Complete list of invocations to be made on the repository by considering case variants
     */
    fun invocations(): List<FinderInvocation> {
        val invs = mutableListOf<FinderInvocation>()
        for (invocation in invocations) {
            // Add the original invocation
            invs.add(invocation)
            if (invocation.args.containsKey("name") && !(invocation.args["name"] as String)[0].isUpperCase()) {
                // Add a case-insensitive variant if the name is present
                val nameArg = invocation.args["name"] as? String ?: continue
                val casedFinder = FinderInvocation(
                    name = invocation.name,
                    args = invocation.args + ("name" to nameArg.replaceFirstChar { it.uppercase() })
                )
                loggerFor<FinderInvocations>().info(
                    "Adding case-insensitive variant for {}: {}",
                    invocation.name,
                    casedFinder
                )
                invs.add(casedFinder)
            }
        }
        return invs
    }
}

internal data class FinderInvocation(
    val name: String,
    val args: Map<String, Any>
)
