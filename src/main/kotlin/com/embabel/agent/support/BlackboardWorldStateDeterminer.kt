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
package com.embabel.agent.support

import com.embabel.agent.ProcessContext
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.WorldState
import com.embabel.plan.goap.WorldStateDeterminer
import org.slf4j.LoggerFactory

class BlackboardWorldStateDeterminer(
    private val processContext: ProcessContext,
) : WorldStateDeterminer {

    private val logger = LoggerFactory.getLogger(BlackboardWorldStateDeterminer::class.java)

    private val knownConditions = processContext.agentProcess.agent.goapSystem.knownConditions()

    override fun determineWorldState(): WorldState {
        val map = mutableMapOf<String, ConditionDetermination>()
        knownConditions.forEach { condition ->
            map[condition] = determineCondition(condition)
        }
        return WorldState(map)
    }

    override fun determineCondition(condition: String): ConditionDetermination {
        return when {
            // Well known conditions, defined for reuse, with their own evaluation function
            processContext.agentProcess.agent.conditions.any { it.name == condition } -> {
                val conditions = processContext.agentProcess.agent.conditions.filter { it.name == condition }
                if (conditions.size != 1) {
                    throw IllegalStateException("Condition $condition is not unique in agent ${processContext.agentProcess.agent.name}: ${conditions.size} found")
                }
                conditions.single().evaluate(processContext)
            }

            // Data binding condition
            condition.contains(":") -> {
                val (variable, type) = condition.split(":")
                val value = processContext.getValue(variable, type)

                val determination = when {
                    type == "List" ->
                        value != null && (value is List<*>)

                    variable == "all" -> true // TODO fix this
                    else -> {
                        val determination =
                            value != null && value::class.simpleName == type
                        logger.debug(
                            "Determined condition {}={}: variable={}, type={}, value={}",
                            condition,
                            determination,
                            variable,
                            type,
                            value,
                        )
                        determination
                    }
                }
                logger.debug(
                    "Determined binding condition {}={}: bindings={}",
                    condition,
                    determination,
                    processContext.blackboard.infoString(),
                )
                ConditionDetermination(determination)
            }

            // Maybe the condition was explicitly set
            else -> ConditionDetermination(processContext.blackboard.getCondition(condition))
        }
    }
}
