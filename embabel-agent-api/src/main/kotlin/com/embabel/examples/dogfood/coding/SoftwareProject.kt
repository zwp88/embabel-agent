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

import com.embabel.common.ai.prompt.PromptContributor
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.data.repository.CrudRepository

@JsonClassDescription("Analysis of a technology project")
data class SoftwareProject(
    val location: String,
    @get:JsonPropertyDescription("The technologies used in the project. List, comma separated. Include 10")
    val tech: String,
    @get: JsonPropertyDescription("Notes on the coding style used in this project. 20 words.")
    val codingStyle: String,
) : PromptContributor {

    override fun contribution() =
        """
            |Project:
            |$tech
            |
            |Coding style:
            |$codingStyle
        """.trimMargin()


}

interface ProjectRepository : CrudRepository<SoftwareProject, String>
