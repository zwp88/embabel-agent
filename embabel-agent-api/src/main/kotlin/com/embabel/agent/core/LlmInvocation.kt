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
package com.embabel.agent.core

import com.embabel.common.ai.model.Llm
import com.embabel.common.core.types.Timed
import com.embabel.common.core.types.Timestamped
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.metadata.Usage
import java.time.Duration
import java.time.Instant

interface LlmInvocationHistory {

    val llmInvocations: List<LlmInvocation>

    fun cost(): Double {
        return llmInvocations.sumOf { it.cost() }
    }

    /**
     * Note that this is not apples to apples: The usage
     * may be across different LLMs, and the cost may be different.
     * Cost will correctly reflect this.
     * Look in the list for more details about what tokens were spent where.
     */
    fun usage(): Usage {
        val promptTokens = llmInvocations.sumOf { it.usage.promptTokens }
        val completionTokens = llmInvocations.sumOf { it.usage.completionTokens }
        return DefaultUsage(promptTokens, completionTokens)
    }
}

/**
 * @param agentName name of the agent, if known
 */
data class LlmInvocation(

    val llm: Llm,
    val usage: Usage,
    val agentName: String? = null,
    override val timestamp: Instant,
    override val runningTime: Duration,
) : Timestamped, Timed {

    /**
     * Dollar cost of this interaction.
     */
    fun cost(): Double = llm.pricingModel?.costOf(usage) ?: 0.0
}
