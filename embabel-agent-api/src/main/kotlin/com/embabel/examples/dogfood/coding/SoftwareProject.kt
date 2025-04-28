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