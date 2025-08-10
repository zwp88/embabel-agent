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
package com.embabel.agent.api.common.workflow.control

import com.embabel.agent.api.common.SupplierActionContext
import com.embabel.agent.api.common.support.SupplierAction
import com.embabel.agent.api.common.workflow.WorkFlowBuilderReturning
import com.embabel.agent.api.common.workflow.WorkflowBuilder
import com.embabel.agent.api.dsl.AgentScopeBuilder
import com.embabel.agent.core.Goal
import com.embabel.common.core.MobyNameGenerator

class SimpleAgentBuilder<RESULT : Any>(
    private val resultClass: Class<RESULT>,
) {

    companion object : WorkFlowBuilderReturning {

        /**
         * Creates a simple agent builder that can be used to build agents with a single action.
         * This is useful for quick prototyping or when you need a simple agent without complex workflows.
         */
        @JvmStatic
        override fun <RESULT : Any> returning(resultClass: Class<RESULT>): SimpleAgentBuilder<RESULT> {
            return SimpleAgentBuilder(resultClass)
        }
    }

    fun running(
        generator: (SupplierActionContext<RESULT>) -> RESULT,
    ): WorkflowBuilder<RESULT> {
        return SimpleAgentEmitter(generator)
    }

    inner class SimpleAgentEmitter(
        private val generator: (SupplierActionContext<RESULT>) -> RESULT,
    ) : WorkflowBuilder<RESULT>(resultClass) {

        override fun build(): AgentScopeBuilder<RESULT> {
            val action = SupplierAction(
                name = "Generate ${resultClass.simpleName}",
                description = "Generates a result of type ${resultClass.simpleName}",
                cost = 0.0,
                value = 0.0,
                canRerun = true,
                outputClass = resultClass,
                toolGroups = emptySet(),
            ) { context ->
                val supplierContext = SupplierActionContext(
                    processContext = context.processContext,
                    outputClass = resultClass,
                    action = context.action,
                )
                generator(supplierContext)
            }
            val goal = Goal(
                name = "${resultClass.simpleName} Goal",
                description = "Goal to generate a result of type ${resultClass.simpleName}",
                satisfiedBy = resultClass,
            )
            return AgentScopeBuilder(
                name = MobyNameGenerator.generateName(),
                actions = listOf(action),
                goals = setOf(goal),
            )
        }
    }
}
