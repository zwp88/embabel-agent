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

import com.embabel.agent.core.Agent
import com.embabel.agent.core.resultOfType

/**
 * Creates a transformation action from an agent
 */
inline fun <reified I, reified O : Any> Agent.asTransformation() = Transformation<I, O> {
    val childAgentProcess = it.agentPlatform().createChildProcess(
        agent = this,
        parentAgentProcess = it.processContext.agentProcess,
    )
    val childProcessResult = childAgentProcess.run()
    childProcessResult.resultOfType()
}


/**
 * Transformation function signature
 */
fun interface Transformation<I, O> {
    fun transform(payload: TransformationPayload<I, O>): O?
}
