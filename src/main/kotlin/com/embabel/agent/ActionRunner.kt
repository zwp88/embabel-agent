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

import com.embabel.agent.primitive.*
import com.embabel.textio.graph.schema.NodeDefinition
import com.embabel.util.time
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Duration

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ExpressionActionRunner::class, name = "expression"),
    JsonSubTypes.Type(value = QueryActionRunner::class, name = "query"),
)
interface ActionRunner {

    /**
     * Execute an action
     * @param processContext process moment
     * @param action action we're executing under
     */
    fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, NodeDefinition>,
        action: Action,
    ): ActionStatus

    /**
     * Properties referenced from input variable
     * Say "person" is passed. Return "name" and other references
     */
    fun referencedInputProperties(variable: String): Set<String>

    companion object {

        /**
         * Execute this operation with timings and error handling
         */
        fun execute(block: () -> Unit): ActionStatus {
            val (_, ms) = time {
                block()
            }
            return ActionStatus(
                status = ActionStatusCode.COMPLETED,
                runningTime = Duration.ofMillis(ms),
            )
        }
    }

}
