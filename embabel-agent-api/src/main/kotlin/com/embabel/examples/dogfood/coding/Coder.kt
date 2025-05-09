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

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.toolgroups.code.BuildResult
import com.embabel.common.util.time
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

data class CodeModificationRequest(
    @get:JsonPropertyDescription("Request to modify code")
    val request: String,
)


data class CodeModificationReport(
    @get:JsonPropertyDescription("Report of the modifications made to code")
    override val text: String,
) : HasContent

object CoderConditions {
    const val BuildNeeded = "buildNeeded"
    const val BuildFailed = "buildFailed"
    const val BuildSucceeded = "buildSucceeded"
    const val BuildWasLastAction = "buildWasLastAction"
}

/**
 * Generic coding support
 */
@Agent(
    description = "Perform changes to a software project or directory structure",
)
@Profile("!test")
class Coder(
    private val projectRepository: ProjectRepository,
    private val codingProperties: CodingProperties,
) {

    private val logger = LoggerFactory.getLogger(Coder::class.java)

    @Action
    fun loadExistingProject(): SoftwareProject? =
        projectRepository.findById(codingProperties.defaultLocation).getOrNull()


    @Action(toolGroups = [ToolGroup.GITHUB])
    fun codeModificationRequestFromUserInput(
        project: SoftwareProject,
        userInput: UserInput
    ): CodeModificationRequest = using(llm = codingProperties.primaryCodingLlm).create(
        """
        Create a CodeModification request based on this user input: ${userInput.content}
        If the user wants you to pick up an issue from GitHub, search for it at ${project.url}.
        Search for the milestone the user suggests.
        Use the GitHub tools.
        Create a CodeModificationRequest from the issue.
        """.trimIndent()
    )


    /**
     * Llm will determine the command to use to build the project.
     * Only use as a last resort, so we mark it as expensive.
     */
    @Action(
        cost = 10000.0,
        canRerun = true,
        pre = [CoderConditions.BuildNeeded],
        post = [CoderConditions.BuildSucceeded],
    )
    fun buildWithCommand(
        project: SoftwareProject,
        context: ActionContext,
    ): BuildResult {
        val (rawOutput, ms) = time {
            context.promptRunner(
                llm = codingProperties.primaryCodingLlm,
                promptContributors = listOf(project),
            ).generateText(
                """
                Build the project
            """.trimIndent(),
            )
        }
        return project.ci.parseBuildOutput(rawOutput, Duration.ofMillis(ms))
    }

    @Action(
        cost = 500.0,
        canRerun = true,
        pre = [CoderConditions.BuildNeeded],
        post = [CoderConditions.BuildSucceeded],
    )
    fun build(
        project: SoftwareProject,
    ): BuildResult = project.build()

    // The last thing we did was code modification
    @Condition(name = CoderConditions.BuildNeeded)
    fun buildNeeded(context: OperationContext): Boolean =
        context.lastResult() is CodeModificationReport

    @Condition(name = CoderConditions.BuildWasLastAction)
    fun buildWasLastAction(context: OperationContext): Boolean =
        context.lastResult() is BuildResult

    @Condition(name = CoderConditions.BuildSucceeded)
    internal fun buildSucceeded(buildResult: BuildResult): Boolean = buildResult.status?.success == true

    @Condition(name = CoderConditions.BuildFailed)
    fun buildFailed(buildResult: BuildResult): Boolean = buildResult.status?.success == false

    @Action(
        canRerun = true,
        post = [CoderConditions.BuildNeeded],
        toolGroups = [ToolGroup.GITHUB, ToolGroup.WEB]
    )
    fun modifyCode(
        codeModificationRequest: CodeModificationRequest,
        project: SoftwareProject,
        context: ActionContext,
    ): CodeModificationReport {
        logger.info("✎ Modifying code according to request: ${codeModificationRequest.request}")
        val report: String = context.promptRunner(
            llm = codingProperties.primaryCodingLlm,
            promptContributors = listOf(project),
        ).create(
            """
                Execute the following user request to modify code in the given project.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.
                The project will be in git so you can safely modify content without worrying about backups.
                Return an explanation of what you did and why.

                DO NOT ASK FOR USER INPUT: DO WHAT YOU THINK IS NEEDED TO MODIFY THE PROJECT.

                Use the web tools if you are asked to use a technology you don't know about.

                DO NOT BUILD THE PROJECT UNLESS THE USER HAS REQUESTED IT
                AND IT IS NECESSARY TO DECIDE WHAT TO MODIFY.
                IF BUILDING IS NEEDED, BE SURE TO RUN UNIT TESTS.
                DO NOT BUILD *AFTER* MODIFYING CODE.

                User request:
                "${codeModificationRequest.request}"
            }
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }

    @Action(
        canRerun = true,
        pre = [CoderConditions.BuildFailed, CoderConditions.BuildWasLastAction],
        post = [CoderConditions.BuildSucceeded]
    )
    fun fixBrokenBuild(
        codeModificationRequest: CodeModificationRequest,
        project: SoftwareProject,
        buildFailure: BuildResult,
        context: ActionContext,
    ): CodeModificationReport {
        val report: String = context.promptRunner(
            llm = codingProperties.fixCodingLlm,
            promptContributors = listOf(project, buildFailure),
        ).create(
            """
                Modify code in the given project to fix the broken build.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.
                The project will be in git so you can safely modify content without worrying about backups.
                Return an explanation of what you did and why.
                Consider the build failure report.

                DO NOT BUILD THE PROJECT. JUST MODIFY CODE.
                Consider the following user request for the necessary functionality:
                "${codeModificationRequest.request}"
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }

    @Action(pre = [CoderConditions.BuildSucceeded])
    @AchievesGoal(description = "Modify project code as per user request")
    fun codeModificationComplete(codeModificationReport: CodeModificationReport) = codeModificationReport

}
