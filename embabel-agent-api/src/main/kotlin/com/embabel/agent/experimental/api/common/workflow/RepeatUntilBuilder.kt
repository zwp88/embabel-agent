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

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.common.workflow.Feedback
import com.embabel.agent.api.dsl.AgentScopeBuilder

/**
 * Java friendly builder
 */
data class RepeatUntilBuilder<RESULT : Any, FEEDBACK : Feedback>(
    private val resultClass: Class<RESULT>,
    private val feedbackClass: Class<FEEDBACK> = Feedback::class.java as Class<FEEDBACK>,
    private val maxIterations: Int = 5,
    private val scoreThreshold: Double = 0.9,
) {

    companion object {

        @JvmStatic
        fun <RESULT : Any> returning(resultClass: Class<RESULT>): RepeatUntilBuilder<RESULT, Feedback> {
            return RepeatUntilBuilder(resultClass = resultClass)
        }
    }

    fun <F : Feedback> withFeedbackClass(feedbackClass: Class<F>): RepeatUntilBuilder<RESULT, F> =
        RepeatUntilBuilder(
            resultClass = resultClass,
            feedbackClass = feedbackClass,
            maxIterations = maxIterations,
            scoreThreshold = scoreThreshold,
        )

    fun withMaxIterations(maxIterations: Int): RepeatUntilBuilder<RESULT, FEEDBACK> =
        copy(maxIterations = maxIterations)

    fun withScoreThreshold(scoreThreshold: Double): RepeatUntilBuilder<RESULT, FEEDBACK> =
        copy(scoreThreshold = scoreThreshold)

    fun repeating(
        what: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
    ): Critiquer {
        return Critiquer(generator = what)
    }

    inner class Critiquer(
        private val generator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
    ) {

        fun withEvaluator(
            evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        ): Evaluator {
            return Evaluator(generator = generator, evaluator = evaluator)
        }
    }

    inner class Evaluator(
        private val generator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        private val evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
    ) {

        fun withAcceptanceCriteria(
            accept: (f: FEEDBACK) -> Boolean,
        ): Accepter {
            return Accepter(generator, evaluator, accept)
        }
    }

    inner class Accepter(
        private val generator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        private val evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        private val accept: (f: FEEDBACK) -> Boolean,
    ) {

        fun build(): AgentScopeBuilder<RESULT> {
            return RepeatUntil(maxIterations = maxIterations, scoreThreshold = scoreThreshold)
                .repeatUntilAcceptable(
                    task = generator,
                    evaluator = evaluator,
                    acceptanceCriteria = accept,
                    resultClass = resultClass,
                    feedbackClass = feedbackClass,
                )
        }
    }
}
