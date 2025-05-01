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

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Condition
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.lastOrNull
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.toolgroups.code.BuildResult
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile


data class CodeModificationReport(
    override val text: String,
) : HasContent

object CoderConditions {
    const val BuildNeeded = "buildNeeded"
    const val BuildFailed = "buildFailed"
    const val BuildSucceeded = "buildSucceeded"

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
    fun loadExistingProject(): SoftwareProject? {
        val found = projectRepository.findById(codingProperties.defaultLocation)
        if (found.isPresent) {
            logger.info("Found existing project at ${codingProperties.defaultLocation}")
        }
        return found.orElse(null)
    }

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
        val buildOutput = context.promptRunner(
            llm = codingProperties.primaryCodingLlm,
            promptContributors = listOf(project),
        ).generateText(
            """
                Build the project
            """.trimIndent(),
        )
        return project.ci.parseBuildOutput(buildOutput)
    }

    @Action(
        cost = 500.0,
        canRerun = true,
        pre = [CoderConditions.BuildNeeded],
        post = [CoderConditions.BuildSucceeded],
    )
    fun build(
        project: SoftwareProject,
    ): BuildResult {
        return project.build()
    }

    // The last thing we did was code modification
    @Condition(name = CoderConditions.BuildSucceeded)
    fun buildNeeded(context: OperationContext): Boolean =
        context.lastResult() is CodeModificationReport

    @Condition(name = CoderConditions.BuildSucceeded)
    fun buildSucceeded(buildResult: BuildResult): Boolean = buildResult.success

    @Condition(name = CoderConditions.BuildSucceeded)
    fun buildFailed(buildResult: BuildResult): Boolean = buildResult.success

    @Action(canRerun = true, post = [CoderConditions.BuildNeeded])
    fun modifyCode(
        userInput: UserInput,
        project: SoftwareProject,
        context: ActionContext,
    ): CodeModificationReport {
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
                Consider any build failure report.

                User request:
                "${userInput.content}"
            }
            """.trimIndent(),
        )
        context.setCondition(CoderConditions.BuildNeeded, true)
        return CodeModificationReport(report)
    }

    @Action(
        canRerun = true,
        pre = [CoderConditions.BuildFailed],
        post = [CoderConditions.BuildSucceeded]
    )
    fun fixBrokenBuild(
        userInput: UserInput,
        project: SoftwareProject,
        context: ActionContext,
    ): CodeModificationReport {
        val buildFailure = context.lastOrNull<BuildResult> { !it.success }
        val report: String = context.promptRunner(
            llm = codingProperties.primaryCodingLlm,
            promptContributors = listOf(project, buildFailure),
        ).create(
            """
                Modify code in the given project to fix the broken build.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.
                The project will be in git so you can safely modify content without worrying about backups.
                Return an explanation of what you did and why.
                Consider any build failure report.

                Consider the following user request for the necessary functionality:
                "${userInput.content}"

                ${
                buildFailure?.let {
                    "Build failure:\n" + it.contribution()
                }
            }
            """.trimIndent(),
        )
        context.setCondition(CoderConditions.BuildNeeded, true)
        return CodeModificationReport(report)
    }

    @Action(pre = [CoderConditions.BuildSucceeded])
    @AchievesGoal(description = "Modify project code as per user request")
    fun codeModificationComplete(codeModificationReport: CodeModificationReport): CodeModificationReport {
        return codeModificationReport
    }
}
