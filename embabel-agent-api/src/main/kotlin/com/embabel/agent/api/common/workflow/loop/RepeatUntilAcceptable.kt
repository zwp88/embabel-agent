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
package com.embabel.agent.api.common.workflow.loop

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.support.SupplierAction
import com.embabel.agent.api.common.support.TransformationAction
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.core.Action
import com.embabel.agent.core.ComputedBooleanCondition
import com.embabel.agent.core.Goal
import com.embabel.agent.core.last
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.types.Timed
import com.embabel.common.core.types.Timestamped
import com.embabel.common.core.types.ZeroToOne
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

data class Attempt<RESULT : Any, FEEDBACK : Feedback>(
    val result: RESULT,
    val feedback: FEEDBACK,
) : Timestamped {

    override val timestamp: Instant = Instant.now()
}

/**
 * Mutable object. We only bind this once
 */
data class AttemptHistory<RESULT : Any, FEEDBACK : Feedback>(
    private val _attempts: MutableList<Attempt<RESULT, FEEDBACK>> = mutableListOf(),
    private var lastResult: RESULT? = null,
    override val timestamp: Instant = Instant.now(),
) : Timestamped, Timed {

    fun attemptCount(): Int = _attempts.size

    override val runningTime: Duration
        get() = Duration.between(timestamp, Instant.now())

    fun attempts(): List<Attempt<RESULT, FEEDBACK>> = _attempts.toList()

    fun lastAttempt(): Attempt<RESULT, FEEDBACK>? = _attempts.lastOrNull()

    /**
     * Evaluator can use this to access the last result.
     */
    fun resultToEvaluate(): RESULT? = lastResult

    fun lastFeedback(): FEEDBACK? = lastAttempt()?.feedback

    fun bestSoFar(): Attempt<RESULT, FEEDBACK>? = _attempts.maxByOrNull { it.feedback.score }

    internal fun recordResult(
        result: RESULT,
    ) {
        lastResult = result
    }

    internal fun recordAttempt(
        result: RESULT,
        feedback: FEEDBACK,
    ): Attempt<RESULT, FEEDBACK> {
        val attempt = Attempt(result, feedback)
        _attempts.add(attempt)
        return attempt
    }

}


/**
 * Primitive for building repeat until acceptable workflows.
 * See https://www.anthropic.com/engineering/building-effective-agents
 * This is the Evaluator Optimizer pattern
 */
data class RepeatUntilAcceptable(
    val maxIterations: Int = 3,
    val scoreThreshold: ZeroToOne = 0.9,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    inline fun <reified RESULT : Any, reified FEEDBACK : Feedback> build(
        noinline task: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        noinline evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        noinline acceptanceCriteria: (FEEDBACK) -> Boolean = { it.score >= scoreThreshold },
    ): AgentScopeBuilder<RESULT> =
        build(
            task = task,
            evaluator = evaluator,
            acceptanceCriteria = acceptanceCriteria,
            resultClass = RESULT::class.java,
            feedbackClass = FEEDBACK::class.java,
        )


    fun <RESULT : Any, FEEDBACK : Feedback> build(
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
                    logger.info("Bound new AttemptHistory")
                    ah
                }
        }

        val taskAction = SupplierAction(
            name = "=>${resultClass.name}",
            description = "Generate $resultClass",
            post = listOf(RESULT_WAS_BOUND_LAST_CONDITION),
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
            // Allow the evaluator to access the last result
            attemptHistory.recordResult(result)
            logger.info(
                "Generated result {}: {}",
                attemptHistory.attempts().size + 1,
                result,
            )
            result
        }

        val evaluationAction = TransformationAction(
            name = "${resultClass.name}=>${feedbackClass.name}",
            description = "Evaluate $resultClass to $feedbackClass",
            pre = listOf(RESULT_WAS_BOUND_LAST_CONDITION),
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
            val bestSoFar = attemptHistory.bestSoFar()
            if (bestSoFar == null) {
                logger.info(
                    "First feedback computed: {}",
                    feedback,
                )
            } else if (feedback.score > bestSoFar.feedback.score) {
                logger.info(
                    "New best feedback computed: {} (previously {})",
                    feedback,
                    bestSoFar,
                )
            } else {
                logger.info("Not better than we've seen: Feedback is {}", feedback)
            }
            attemptHistory.recordAttempt(context.input, feedback)
            logger.info("Recorded attempt: {} with feedback: {}", context.input, feedback)
            feedback
        }

        val resultWasBoundLastCondition = ComputedBooleanCondition(
            name = RESULT_WAS_BOUND_LAST_CONDITION,
            evaluator = { context, _ ->
                val result = context.lastResult()
                result != null && result::class.java == resultClass
            },
        )

        val acceptableCondition = ComputedBooleanCondition(
            name = ACCEPTABLE_CONDITION,
            evaluator = { context, _ ->
                val attemptHistory = context.last<AttemptHistory<RESULT, FEEDBACK>>()
                if (attemptHistory?.lastAttempt() == null) {
                    false
                } else if (attemptHistory.attempts().size >= maxIterations) {
                    logger.info(
                        "Condition '{}': Giving up after {} iterations",
                        ACCEPTABLE_CONDITION,
                        attemptHistory.attempts().size,
                    )
                    true
                } else {
                    val lastFeedback = attemptHistory.lastAttempt()!!.feedback
                    val isAcceptable = acceptanceCriteria(lastFeedback)
                    logger.info(
                        "Condition '{}', iterations={}: Feedback acceptable={}: {}",
                        ACCEPTABLE_CONDITION,
                        attemptHistory.attempts().size,
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
            val bestResult = context.input.bestSoFar()?.result as? RESULT
                ?: throw IllegalStateException("No result available in AttemptHistory")
            logger.info("Consolidating results, final (best) result: {}", bestResult)
            bestResult
        }

        val resultGoal = Goal(
            "final-${resultClass.name}",
            "Satisfied with the final ${resultClass.name}",
            satisfiedBy = resultClass,
        ).withPreconditions(
            ACCEPTABLE_CONDITION,
            // TODO why is this needed? Should not the satisfiedBy condition be enough?
            RESULT_WAS_BOUND_LAST_CONDITION,
        )
        logger.info("Created goal: {}", resultGoal.infoString(verbose = true, indent = 2))

        return AgentScopeBuilder(
            name = MobyNameGenerator.generateName(),
            actions = listOf(
                taskAction,
                evaluationAction,
                consolidateAction,
            ),
            conditions = setOf(acceptableCondition, resultWasBoundLastCondition),
            goals = setOf(resultGoal),
            opaque = true,
        )
    }

    private companion object {
        private val ACCEPTABLE_CONDITION = "${RepeatUntilAcceptable::class.simpleName}_acceptable"
        private val RESULT_WAS_BOUND_LAST_CONDITION = "${RepeatUntilAcceptable::class.simpleName}_resultWasBoundLast"
    }

}
