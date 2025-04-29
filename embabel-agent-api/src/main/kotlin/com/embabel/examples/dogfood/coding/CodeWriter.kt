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
import com.embabel.agent.api.common.create
import com.embabel.agent.core.lastOrNull
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.toolgroups.code.BuildResult
import com.embabel.agent.toolgroups.code.toMavenBuildResult
import org.springframework.context.annotation.Profile

data class Explanation(
    override val text: String,
) : HasContent

data class CodeModificationReport(
    override val text: String,
) : HasContent

object Conditions {
    const val BuildSucceeded = "buildSucceeded"
    const val SpringProjectCreated = "springProjectCreated"
}

@Agent(
    description = "Explain code or perform changes to a software project or directory structure",
)
@Profile("!test")
class CodeWriter(
    projectRepository: ProjectRepository,
    defaultLocation: String = System.getProperty("user.dir") + "/embabel-agent-api",
) : CodeHelperSupport(projectRepository, defaultLocation) {

    @Action(
        cost = 1000.0,
        canRerun = true,
        post = [Conditions.BuildSucceeded],
    )
    fun build(
        project: SoftwareProject,
        context: ActionContext,
    ): BuildResult {
        val buildOutput = context.promptRunner(
            llm = claudeSonnet,
            promptContributors = listOf(project),
        ).generateText(
            """
                Build the project
            """.trimIndent(),
        )
        // TODO shouldn't be had coded
        return toMavenBuildResult(buildOutput)
    }
    

    @Condition(name = Conditions.BuildSucceeded)
    fun buildSucceeded(buildResult: BuildResult): Boolean = buildResult.success

    @Action(pre = [Conditions.BuildSucceeded])
    @AchievesGoal(description = "Modify project code as per user request")
    fun codeModificationComplete(codeModificationReport: CodeModificationReport): CodeModificationReport {
        return codeModificationReport
    }

    @Action(canRerun = true, post = [Conditions.BuildSucceeded])
    fun modifyCode(
        userInput: UserInput,
        project: SoftwareProject,
        context: ActionContext,
    ): CodeModificationReport {
        val buildFailure = context.lastOrNull<BuildResult> { !it.success }
        val report: String = context.promptRunner(
            llm = claudeSonnet,
            promptContributors = listOf(project, buildFailure),
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

                ${
                buildFailure?.let {
                    "Previous build failed:\n" + it.contribution()
                }
            }
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }
}
