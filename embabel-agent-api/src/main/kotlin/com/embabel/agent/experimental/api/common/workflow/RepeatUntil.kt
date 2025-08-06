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
package com.embabel.agent.experimental.api.common.workflow

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.support.SupplierAction
import com.embabel.agent.api.common.support.TransformationAction
import com.embabel.agent.api.common.workflow.Feedback
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.core.Action
import com.embabel.agent.core.ComputedBooleanCondition
import com.embabel.agent.core.Goal
import com.embabel.agent.core.last
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.types.Timestamped
import com.embabel.common.core.types.ZeroToOne
import org.slf4j.LoggerFactory
import java.time.Instant

data class Attempt<RESULT : Any, FEEDBACK : Feedback>(
    val result: RESULT,
    val feedback: FEEDBACK,
) : Timestamped {

    override val timestamp: Instant = Instant.now()
}

/**
 * We only bind this once
 */
data class AttemptHistory<RESULT : Any, FEEDBACK : Feedback>(
    private val _attempts: MutableList<Attempt<RESULT, FEEDBACK>> = mutableListOf(),
) {

    val attempts: List<Attempt<RESULT, FEEDBACK>> = _attempts.toList()

    val last: Attempt<RESULT, FEEDBACK>? = attempts.lastOrNull()

    val bestSoFar: Attempt<RESULT, FEEDBACK>? = attempts.maxByOrNull { it.feedback.score }

    fun recordAttempt(
        result: RESULT,
        feedback: FEEDBACK,
    ): Attempt<RESULT, FEEDBACK> {
        val attempt = Attempt(result, feedback)
        _attempts.add(attempt)
        return attempt
    }

}


/**
 * See https://www.anthropic.com/engineering/building-effective-agents
 * This is the Evaluator Optimizer pattern
 */
data class RepeatUntil(
    val maxIterations: Int = 3,
    val scoreThreshold: ZeroToOne = 0.9,
) {

    private val logger = LoggerFactory.getLogger(RepeatUntil::class.java)

    inline fun <reified RESULT : Any, reified FEEDBACK : Feedback> repeatUntilAcceptable(
        noinline task: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        noinline evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        noinline acceptanceCriteria: (FEEDBACK) -> Boolean = { it.score >= scoreThreshold },
    ): AgentScopeBuilder<RESULT> =
        repeatUntilAcceptable(
            task = task,
            evaluator = evaluator,
            acceptanceCriteria = acceptanceCriteria,
            resultClass = RESULT::class.java,
            feedbackClass = FEEDBACK::class.java,
        )


    fun <RESULT : Any, FEEDBACK : Feedback> repeatUntilAcceptable(
        task: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        acceptanceCriteria: (FEEDBACK) -> Boolean,
        resultClass: Class<RESULT>,
        feedbackClass: Class<FEEDBACK>,
    ): AgentScopeBuilder<RESULT> {

        fun findOrBindAttemptHistory(context: OperationContext): AttemptHistory<RESULT, FEEDBACK> {
            return context.last<AttemptHistory<RESULT, FEEDBACK>>()
                ?: run {
                    val ah = AttemptHistory<RESULT, FEEDBACK>()
                    context += ah
                    ah
                }
        }

        val taskAction = SupplierAction(
            name = "=>${resultClass.name}",
            description = "Generate $resultClass",
            post = listOf(RESULT_WAS_LAST_ACTION_CONDITION),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            outputClass = resultClass,
            toolGroups = emptySet(),
        ) { context ->
            val attemptHistory = findOrBindAttemptHistory(context)

            val tac = (context as TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>).copy(
                input = attemptHistory,
            )
            val result = task.invoke(tac)
            logger.info("Generated result: {}", result)
            result
        }


        val evaluationAction = TransformationAction(
            name = "${resultClass.name}=>${feedbackClass.name}",
            description = "Evaluate $resultClass to $feedbackClass",
            pre = listOf(RESULT_WAS_LAST_ACTION_CONDITION),
            post = listOf(ACCEPTABLE_CONDITION),
            cost = 0.0,
            value = 0.0,
            canRerun = true,
            inputClass = resultClass,
            outputClass = feedbackClass,
            toolGroups = emptySet(),
        ) { context ->
            val attemptHistory = findOrBindAttemptHistory(context)
            val tac = (context as TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>).copy(
                input = attemptHistory,
            )
            val feedback = evaluator(tac)
            if (attemptHistory.bestSoFar == null || feedback.score > attemptHistory.bestSoFar.feedback.score) {
                logger.info(
                    "New best feedback found: {} (was {})",
                    feedback,
                    attemptHistory.bestSoFar ?: "none",
                )
            } else {
                logger.debug("Not better than we've seen: Feedback is {}", feedback)
            }
            attemptHistory.recordAttempt(context.input, feedback)
            feedback
        }

        val resultWasLastActionCondition = ComputedBooleanCondition(
            name = RESULT_WAS_LAST_ACTION_CONDITION,
            evaluator = { context, _ ->
                val result = context.lastResult()
                result != null && result::class.java == resultClass
            },
        )

        val acceptableCondition = ComputedBooleanCondition(
            name = ACCEPTABLE_CONDITION,
            evaluator = { context, _ ->
                val attemptHistory = context.last<AttemptHistory<RESULT, FEEDBACK>>()
                if (attemptHistory == null || attemptHistory.last == null) {
                    false
                } else if (attemptHistory.attempts.size >= maxIterations) {
                    logger.info(
                        "Condition '{}': Giving up after {} iterations",
                        ACCEPTABLE_CONDITION,
                        attemptHistory.attempts.size,
                    )
                    true
                } else {
                    val lastFeedback = attemptHistory.last.feedback
                    val isAcceptable = acceptanceCriteria(lastFeedback)
                    logger.debug(
                        "Condition '{}', iterations={}: Feedback acceptable={}: {}",
                        ACCEPTABLE_CONDITION,
                        attemptHistory.attempts.size,
                        isAcceptable,
                        lastFeedback,
                    )
                    isAcceptable
                }
            }
        )

        val consolidateAction: Action = TransformationAction(
            name = "consolidate-${resultClass.name}-${feedbackClass.name}",
            description = "Consolidate results and feedback",
            pre = listOf(ACCEPTABLE_CONDITION),
            cost = 0.0,
            value = 0.0,
            toolGroups = emptySet(),
            inputClass = AttemptHistory::class.java,
            outputClass = resultClass,
        ) { context ->
            context.input.last?.result as? RESULT
                ?: throw IllegalStateException("No result available in AttemptHistory")
        }

        val resultGoal = Goal(
            "final-${resultClass.name}",
            "Satisfied with the final ${resultClass.name}",
            satisfiedBy = resultClass,
        ).withPrecondition(ACCEPTABLE_CONDITION)

        return AgentScopeBuilder(
            name = MobyNameGenerator.generateName(),
            actions = listOf(
                taskAction,
                evaluationAction,
                consolidateAction,
            ),
            conditions = setOf(acceptableCondition, resultWasLastActionCondition),
            goals = setOf(resultGoal)
        )
    }

    private companion object {
        private const val ACCEPTABLE_CONDITION = "acceptable"
        private const val RESULT_WAS_LAST_ACTION_CONDITION = "resultWasLastAction"
    }

}
