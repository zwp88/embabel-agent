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
package com.embabel.agent.core.support

import com.embabel.agent.core.Action
import com.embabel.agent.core.ActionQos
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.Transition
import com.embabel.common.core.types.ZeroToOne
import com.embabel.plan.goap.ConditionDetermination
import com.embabel.plan.goap.EffectSpec
import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback

const val HAS_RUN_CONDITION_PREFIX = "hasRun_"

/**
 * Abstract action implementation
 * @param name the name of the action
 * @param description a description of the action
 * @param pre a list of preconditions. These are additional to the input
 * @param post a list of expected effects. These are additional to the output
 * @param cost the cost of the action
 * @param inputs the input bindings
 * @param outputs the output bindings
 * @param canRerun can we rerun this action?
 * @param transitions any transitions
 * @param qos quality of service requirements
 */
abstract class AbstractAction(
    override val name: String,
    override val description: String = name,
    val pre: List<String> = emptyList(),
    val post: List<String> = emptyList(),
    override val cost: ZeroToOne = 0.0,
    override val value: ZeroToOne = 0.0,
    override val inputs: Set<IoBinding> = emptySet(),
    override val outputs: Set<IoBinding> = emptySet(),
    override val transitions: List<Transition> = emptyList(),
    override val toolCallbacks: Collection<ToolCallback>,
    override val toolGroups: Collection<String>,
    override val canRerun: Boolean,
    override val qos: ActionQos = ActionQos(),
) : Action {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    @JsonIgnore
    override val preconditions: EffectSpec =
        run {
            val conditions = pre.associate { it to ConditionDetermination(true) }.toMutableMap()
            inputs.forEach { input ->
                conditions[input.value] = ConditionDetermination(true)
            }
            if (!canRerun) {
                outputs.filter {
                    !inputs.contains(it)
                }.forEach { output ->
                    conditions[output.value] = ConditionDetermination(false)
                }
                conditions["$HAS_RUN_CONDITION_PREFIX$name"] = ConditionDetermination(false)
            }
            conditions
        }

    // TODO possible effects can include effects of any nodes that have a transition
    @JsonIgnore
    override val effects: EffectSpec = run {
        val conditions = post.associate { it to ConditionDetermination(true) }.toMutableMap()
        outputs.forEach { output ->
            conditions[output.value] = ConditionDetermination(true)
        }
        conditions += (HAS_RUN_CONDITION_PREFIX + this.name) to ConditionDetermination(true)
        conditions
    }
}
