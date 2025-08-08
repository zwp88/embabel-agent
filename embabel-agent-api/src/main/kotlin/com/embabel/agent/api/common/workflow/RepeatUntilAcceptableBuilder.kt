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

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.common.Constants.EMBABEL_PROVIDER
import com.embabel.agent.core.Agent

/**
 * Java friendly builder for RepeatUntil workflow.
 */
data class RepeatUntilAcceptableBuilder<RESULT : Any, FEEDBACK : Feedback>(
    private val resultClass: Class<RESULT>,
    private val feedbackClass: Class<FEEDBACK> = Feedback::class.java as Class<FEEDBACK>,
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val scoreThreshold: Double = DEFAULT_SCORE_THRESHOLD,
) {

    companion object {

        const val DEFAULT_MAX_ITERATIONS = 5

        const val DEFAULT_SCORE_THRESHOLD = 0.9

        /**
         * Create a RepeatUntilBuilder for a specific result type and default TextFeedback.
         */
        @JvmStatic
        fun <RESULT : Any> returning(resultClass: Class<RESULT>): RepeatUntilAcceptableBuilder<RESULT, TextFeedback> {
            return RepeatUntilAcceptableBuilder(resultClass = resultClass, feedbackClass = TextFeedback::class.java)
        }
    }

    /**
     * Customize the feedback class for this RepeatUntil workflow.
     */
    fun <F : Feedback> withFeedbackClass(feedbackClass: Class<F>): RepeatUntilAcceptableBuilder<RESULT, F> =
        RepeatUntilAcceptableBuilder(
            resultClass = resultClass,
            feedbackClass = feedbackClass,
            maxIterations = maxIterations,
            scoreThreshold = scoreThreshold,
        )

    fun withMaxIterations(maxIterations: Int): RepeatUntilAcceptableBuilder<RESULT, FEEDBACK> =
        copy(maxIterations = maxIterations)

    fun withScoreThreshold(scoreThreshold: Double): RepeatUntilAcceptableBuilder<RESULT, FEEDBACK> =
        copy(scoreThreshold = scoreThreshold)

    /**
     * Define the task to be repeated until an acceptable result is achieved.
     */
    fun repeating(
        what: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
    ): Critiquer {
        return Critiquer(generator = what)
    }

    inner class Critiquer(
        private val generator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
    ) {

        /**
         * Provide the evaluation function that will assess the generated results.
         */
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

        /**
         * Define the acceptance criteria for the feedback.
         * This will determine when the generated result is considered acceptable.
         */
        fun withAcceptanceCriteria(
            accept: (f: FEEDBACK) -> Boolean,
        ): Accepter {
            return Accepter(generator, evaluator, accept)
        }

        /**
         * Build an instance with default acceptance criteria,
         * based on threshold score
         */
        fun build(): AgentScopeBuilder<RESULT> {
            return withAcceptanceCriteria { it.score >= scoreThreshold }
                .build()
        }

        /**
         * Build an agent on this RepeatUntil workflow
         * with default acceptance criteria.
         */
        fun buildAgent(
            name: String,
            description: String,
        ): Agent {
            return build()
                .build()
                .createAgent(
                    name = name,
                    provider = EMBABEL_PROVIDER,
                    description = description,
                )
        }
    }

    inner class Accepter(
        private val generator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, RESULT>) -> RESULT,
        private val evaluator: (TransformationActionContext<AttemptHistory<RESULT, FEEDBACK>, FEEDBACK>) -> FEEDBACK,
        private val accept: (f: FEEDBACK) -> Boolean,
    ) {

        /**
         * Build the workflow so it can be included in agents
         */
        fun build(): AgentScopeBuilder<RESULT> {
            return RepeatUntilAcceptable(maxIterations = maxIterations, scoreThreshold = scoreThreshold)
                .build(
                    task = generator,
                    evaluator = evaluator,
                    acceptanceCriteria = accept,
                    resultClass = resultClass,
                    feedbackClass = feedbackClass,
                )
        }

        /**
         * Build an agent on this RepeatUntil workflow.
         */
        fun buildAgent(
            name: String,
            description: String,
        ): Agent {
            return build()
                .build()
                .createAgent(
                    name = name,
                    provider = EMBABEL_PROVIDER,
                    description = description,
                )
        }
    }
}
