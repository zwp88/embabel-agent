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
package com.embabel.agent.primitive

import com.embabel.agent.*
import com.embabel.textio.graph.schema.NodeDefinition
import com.embabel.util.time
import java.time.Duration

/**
 * Action that evaluates an expression, which is expected to have side effects
 */
data class ExpressionActionRunner(
    override val language: ExpressionLanguage = ExpressionLanguage.KTS,
    override val expression: String,
) : ActionRunner, Expression {

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action,
    ): ActionStatus {
//        // TODO resilience against bad formats in entities
//        val (_, ms) = time {
//            processContext.platformServices.scriptEvaluationService.evaluateExpression<Any>(
//                expression = expression,
//                context = processContext.blackboard.expressionEvaluationModel(),
//            )
//        }
//        return ActionStatus(
//            status = ActionStatusCode.COMPLETED,
//            runningTime = Duration.ofMillis(ms),
//        )
        TODO()
    }

    override fun referencedInputProperties(variable: String): Set<String> {
        return emptySet()
    }
}
