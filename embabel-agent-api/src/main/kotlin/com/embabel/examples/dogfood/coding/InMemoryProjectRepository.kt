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
package com.embabel.examples.dogfood.coding

import com.embabel.examples.simple.movie.InMemoryCrudRepository
import org.springframework.stereotype.Service

@Service
class InMemoryMovieProjectRepository : ProjectRepository,
    InMemoryCrudRepository<Project>(
        idGetter = { it.location },
        idSetter = { _, _ -> TODO("shouldn't be called") },
    ) {

    init {
        save(EmbabelAgentApi)
    }
}

val EmbabelAgentApi = Project(
    location = System.getProperty("user.dir") + "/embabel-agent-api",
    tech = """
        |Kotlin, Spring Boot, Maven, Jacoco, Spring AI
        |JUnit 5, mockk
        |"""".trimMargin(),
    codingStyle = """
        Favor clarity. Use Kotlin coding conventions and consistent formatting.
        Use the Spring idiom where possible.
        Favor immutability.
        Favor test cases using @Nested classes. Use ` instead of @DisplayName for test cases.
        In log statements, use placeholders for efficiency.
        E.g. logger.info("{} {}", a, b) instead of logger.info("computed string").
        
        In Java, use modern Java features like var, records, and switch expressions.
        Use multiline strings rather than concatenation.
        
       
    """.trimIndent(),
)
