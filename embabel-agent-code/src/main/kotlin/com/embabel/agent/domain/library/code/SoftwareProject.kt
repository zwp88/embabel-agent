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
package com.embabel.agent.domain.library.code

import com.embabel.agent.api.common.LlmReference
import com.embabel.agent.tools.file.*
import com.embabel.coding.tools.ci.BuildOptions
import com.embabel.coding.tools.ci.BuildResult
import com.embabel.coding.tools.ci.Ci
import com.embabel.common.util.StringTransformer
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam

/**
 * Open to allow extension
 */
@JsonClassDescription("Analysis of a technology project")
open class SoftwareProject @JvmOverloads constructor(
    override val root: String,
    val url: String? = null,
    @get:JsonPropertyDescription("The technologies used in the project. List, comma separated. Include 10")
    val tech: String = "Java,Embabel,Spring Boot,Maven",
    val defaultCodingStyle: String = """
            No coding style guide found at ${DEFAULT_CODING_STYLE_GUIDE}.
            Try to follow the conventions of files you read in the project.
        """.trimIndent(),
    @get:JsonPropertyDescription("Build command, such as 'mvn clean test'")
    val buildCommand: String = "mvn clean test",
    val wasCreated: Boolean = false,
) : LlmReference, FileTools, SymbolSearch, FileChangeLog by DefaultFileChangeLog(), FileReadLog by DefaultFileReadLog() {

    init {
        if (!exists()) {
            error("Directory does not exist")
        }
        loggerFor<SoftwareProject>().info(
            "Software project tools: ${
                toolCallbacks.map { it.toolDefinition.name() }.sorted()
            }"
        )
    }

    override val name
        get() = root.substringAfterLast('/')

    override val description get() = "Software project at $root${if (url != null) " from $url" else ""} using $tech"

    val codingStyle: String
        get() {
            val location = DEFAULT_CODING_STYLE_GUIDE
            loggerFor<SoftwareProject>().info("Looking for coding style guide at '$location'")
            val content = safeReadFile(location)
            loggerFor<SoftwareProject>().info("Found coding style guide at $location")
            return content
                ?: defaultCodingStyle
        }

    override val fileContentTransformers: List<StringTransformer>
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
        @ToolParam(description = "glob pattern for file to search") globPattern: String,
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

    override fun notes() =
        """
            |Project:
            |${url ?: "No URL"}
            |$tech
            |
            |Coding style:
            |$codingStyle
        """.trimMargin()

    companion object {
        const val DEFAULT_CODING_STYLE_GUIDE = ".embabel/coding-style.md"
    }

}
