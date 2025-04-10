/*
                                * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.agent

import com.embabel.agent.primitive.ExpressionCondition
import com.embabel.agent.primitive.PromptCondition
import com.embabel.plan.goap.ConditionDetermination
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

typealias ZeroToOne = Double

/**
 * A Condition is a named, well known predicate that can be evaluated
 * and reused across multiple Actions.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ExpressionCondition::class),
    JsonSubTypes.Type(value = PromptCondition::class),
)
interface Condition {
    val name: String

    /**
     * Cost of evaluating the condition. 0 is cheap, 1 is expensive.
     * Helps in planning.
     */
    val cost: ZeroToOne

    /**
     * Evaluate the condition in the context of the process.
     * This may be expensive, so the cost is provided.
     * The infrastructure will attempt to call this function infrequently on expensive conditions,
     * so there's no urgency to optimize here.
     */
    fun evaluate(
        processContext: ProcessContext,
    ): ConditionDetermination

    operator fun not(): Condition = NotCondition(this)

    operator fun inv(): Condition = UnknownCondition(this)
}

class BooleanCondition(
    override val name: String,
    override val cost: ZeroToOne = 0.0,
    private val evaluator: (ProcessContext) -> Boolean,
) : Condition {
    override fun evaluate(processContext: ProcessContext): ConditionDetermination =
        ConditionDetermination(evaluator(processContext))
}


private class NotCondition(private val condition: Condition) : Condition {
    override val name = "!${condition.name}"
    override val cost = condition.cost
    override fun evaluate(processContext: ProcessContext) = when (condition.evaluate(processContext)) {
        ConditionDetermination.TRUE -> ConditionDetermination.FALSE
        ConditionDetermination.FALSE -> ConditionDetermination.TRUE
        ConditionDetermination.UNKNOWN -> ConditionDetermination.UNKNOWN
    }
}

private class UnknownCondition(private val condition: Condition) : Condition {
    override val name = "!${condition.name}"
    override val cost = condition.cost
    override fun evaluate(processContext: ProcessContext) = when (condition.evaluate(processContext)) {
        ConditionDetermination.TRUE -> ConditionDetermination.FALSE
        ConditionDetermination.FALSE -> ConditionDetermination.FALSE
        ConditionDetermination.UNKNOWN -> ConditionDetermination.TRUE
    }
}
