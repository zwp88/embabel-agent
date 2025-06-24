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
package com.embabel.agent.api.common

import com.embabel.agent.core.Action
import com.embabel.agent.core.Agent
import com.embabel.common.core.types.ZeroToOne

/**
 * For use from Java
 */
object AgentUtils {

    /**
     * Wrap the given agent as an action that can be used in a plan.
     */
    @JvmStatic
    @JvmOverloads
    fun <I, O : Any> agentAction(
        agent: Agent,
        inputClass: Class<I>,
        outputClass: Class<O>,
        pre: List<String> = emptyList(),
        post: List<String> = emptyList(),
        cost: ZeroToOne = 0.0,
        value: ZeroToOne = 0.0,
        canRerun: Boolean = false,
    ): Action =
        agentTransformer(
            agent = agent,
            pre = pre,
            post = post,
            cost = cost,
            value = value,
            canRerun = canRerun,
            inputClass = inputClass,
            outputClass = outputClass,
        )

    @JvmStatic
    @JvmOverloads
    fun <I, O : Any> agentAction(
        agentName: String,
        inputClass: Class<I>,
        outputClass: Class<O>,
        pre: List<String> = emptyList(),
        post: List<String> = emptyList(),
        cost: ZeroToOne = 0.0,
        value: ZeroToOne = 0.0,
        canRerun: Boolean = false,
    ): Action = asAction(
        agentName = agentName,
        inputClass = inputClass,
        outputClass = outputClass,
        pre = pre,
        post = post,
        cost = cost,
        value = value,
        canRerun = canRerun,
    )

}
