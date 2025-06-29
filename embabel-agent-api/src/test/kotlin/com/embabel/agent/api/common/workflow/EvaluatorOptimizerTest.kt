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
package com.embabel.agent.api.common.workflow

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.asSubProcess
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.integration.IntegrationTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

data class Report(
    val content: String,
)

@Agent(description = "evaluator test")
class EvaluationFlowDoesNotTerminate {

    @Action
    fun toFeedback(
        userInput: UserInput,
        context: ActionContext,
    ): ScoredResult<Report, SimpleFeedback> {
        var count = 0
        return context.asSubProcess(
            EvaluatorOptimizer.generateUntilAcceptable(
                generator = {
                    count++
                    Report("thing-$count")
                },
                evaluator = {
                    SimpleFeedback(.5, "feedback")
                },
                maxIterations = 3,
            )
        )
    }

    @AchievesGoal(description = "Creating a person")
    @Action
    fun done(scoredResult: ScoredResult<Report, SimpleFeedback>): Report {
        assertEquals(0.5, scoredResult.feedback.score)
        return scoredResult.result
    }
}

@Agent(description = "evaluator test")
class EvaluationFlowTerminatesWithBestLast {

    @Action
    fun toFeedback(
        userInput: UserInput,
        context: ActionContext,
    ): ScoredResult<Report, SimpleFeedback> {
        var count = 0
        return context.asSubProcess(
            EvaluatorOptimizer.generateUntilAcceptable(
                generator = {
                    count++
                    Report("thing-$count")
                },
                evaluator = {
                    SimpleFeedback(.3 * count, "feedback")
                },
                maxIterations = 5,
            )
        )
    }

    @AchievesGoal(description = "Creating a person")
    @Action
    fun done(scoredResult: ScoredResult<Report, SimpleFeedback>): Report {
        assertTrue(scoredResult.feedback.score > .9)
        return scoredResult.result
    }
}

@Agent(description = "evaluator test")
class EvaluationFlowTerminatesWithRandomBest {

    // TODO this being stateful isn't OK for production but this is a test
    private var allFeedbacks: MutableList<SimpleFeedback> = mutableListOf()

    @Action
    fun toFeedback(
        userInput: UserInput,
        context: ActionContext,
    ): ScoredResult<Report, SimpleFeedback> {
        val random = Random()
        var count = 0
        return context.asSubProcess(
            EvaluatorOptimizer.generateUntilAcceptable(
                generator = {
                    count++
                    Report("thing-$count")
                },
                evaluator = {
                    val f = SimpleFeedback(score = random.nextDouble(1.0), "feedback")
                    allFeedbacks.add(f)
                    f
                },
                acceptanceCriteria = { it.score > 0.7 },
                maxIterations = 20,
            )
        )
    }

    @AchievesGoal(description = "Creating a person")
    @Action
    fun done(
        scoredResult: ScoredResult<Report, SimpleFeedback>,
        context: OperationContext,
    ): Report {
        assertTrue(allFeedbacks.isNotEmpty(), "Must have feedbacks:\n${context.infoString(true)}")
        assertEquals(
            allFeedbacks.maxOfOrNull { it.score },
            scoredResult.feedback.score,
            "Should have the best feedback score of ${allFeedbacks.size}",
        )
        return scoredResult.result
    }
}

class EvaluatorOptimizerTest {

    @Test
    fun `does not terminate`() {
        val reader = AgentMetadataReader()
        val agent = reader.createAgentMetadata(EvaluationFlowDoesNotTerminate()) as com.embabel.agent.core.Agent
        val ap = IntegrationTestUtils.dummyAgentPlatform()
        val result = ap.runAgentFrom(
            agent = agent,
            processOptions = ProcessOptions(),
            bindings = mapOf(
                "it" to UserInput("input"),
            ),
        )
        assertEquals(AgentProcessStatusCode.COMPLETED, result.status)
        kotlin.test.assertTrue(result.lastResult() is Report)
    }

    @Test
    fun `terminates due to best being last`() {
        val reader = AgentMetadataReader()
        val agent = reader.createAgentMetadata(EvaluationFlowTerminatesWithBestLast()) as com.embabel.agent.core.Agent
        val ap = IntegrationTestUtils.dummyAgentPlatform()
        val result = ap.runAgentFrom(
            agent = agent,
            processOptions = ProcessOptions(),
            bindings = mapOf(
                "it" to UserInput("input"),
            ),
        )
        assertEquals(AgentProcessStatusCode.COMPLETED, result.status)
        assertTrue(result.lastResult() is Report)
        val report = result.lastResult() as Report
        assertTrue(report.content.startsWith("thing"))
    }

    @Test
    fun `terminates with best being random`() {
        val reader = AgentMetadataReader()
        val agent = reader.createAgentMetadata(EvaluationFlowTerminatesWithRandomBest()) as com.embabel.agent.core.Agent
        val ap = IntegrationTestUtils.dummyAgentPlatform()
        val result = ap.runAgentFrom(
            agent = agent,
            processOptions = ProcessOptions(),
            bindings = mapOf(
                "it" to UserInput("input"),
            ),
        )
        assertEquals(AgentProcessStatusCode.COMPLETED, result.status)
        assertTrue(result.lastResult() is Report)
        val report = result.lastResult() as Report
        assertTrue(report.content.startsWith("thing"))
    }

}
