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

import com.embabel.agent.toolgroups.code.BuildOptions
import com.embabel.agent.toolgroups.code.BuildResult
import com.embabel.agent.toolgroups.code.Ci
import com.embabel.agent.toolgroups.code.SymbolSearch
import com.embabel.agent.toolgroups.file.FileContentTransformer
import com.embabel.agent.toolgroups.file.FileTools
import com.embabel.agent.toolgroups.file.WellKnownFileContentTransformers
import com.embabel.common.ai.prompt.PromptContributor
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.data.repository.CrudRepository

/**
 * Open to allow extension
 */
@JsonClassDescription("Analysis of a technology project")
open class SoftwareProject(
    override val root: String,
    val url: String? = null,
    @get:JsonPropertyDescription("The technologies used in the project. List, comma separated. Include 10")
    val tech: String,
    @get:JsonPropertyDescription("Notes on the coding style used in this project. 20 words.")
    val codingStyle: String,
    @get:JsonPropertyDescription("Build command, such as 'mvn clean test'")
    val buildCommand: String,
) : PromptContributor, FileTools, SymbolSearch /*CiTools*/ {

    override val fileContentTransformers: List<FileContentTransformer>
        get() = listOf(WellKnownFileContentTransformers.removeApacheLicenseHeader)

    val ci = Ci(root)

    @Tool(description = "Returns the file containing a class with the given name")
    fun findClass(@ToolParam(description = "class name") name: String): String {
        val matches = findClassInProject(name, globPattern = "**/*.{java,kt}")
        return if (matches.isNotEmpty()) {
            matches.joinToString("\n") { it.relativePath }
        } else {
            "No class found with name $name"
        }
    }

    @Tool(description = "Returns the file containing a class with the given name")
    fun findPattern(
        @ToolParam(description = "regex pattern") regex: String,
        @ToolParam(description = "glob pattern for file to search") globPattern: String
    ): String {
        val matches = findPatternInProject(pattern = Regex(regex), globPattern = globPattern)
        return if (matches.isNotEmpty()) {
            matches.joinToString("\n") { it.relativePath }
        } else {
            "No matches for pattern '$regex' in $globPattern"
        }
    }

    @Tool(description = "Build the project using the given command in the root")
    fun build(command: String): String {
        val br = ci.buildAndParse(BuildOptions(command, true))
        return br.relevantOutput()
    }

    fun build(): BuildResult {
        return ci.buildAndParse(BuildOptions(buildCommand, true))
    }

    override fun toString(): String {
        return "SoftwareProject($root)"
    }

    override fun contribution() =
        """
            |Project:
            |${url ?: "No URL"}
            |$tech
            |
            |Coding style:
            |$codingStyle
        """.trimMargin()

}

interface ProjectRepository : CrudRepository<SoftwareProject, String>
