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
package com.embabel.agent.testing

import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.support.DefaultAgentPlatform
import com.embabel.agent.rag.RagService
import com.embabel.agent.spi.PlatformServices
import io.mockk.mockk

fun createAgentPlatform(): AgentPlatform {
    val llmOperations = DummyObjectCreatingLlmOperations.Companion.LoremIpsum
    return DefaultAgentPlatform(
        llmOperations = llmOperations,
        eventListeners = listOf(EventSavingAgenticEventListener()),
        toolGroupResolver = mockk(),
        ragService = RagService.empty(),
    )
}

fun dummyPlatformServices(): PlatformServices {
    return PlatformServices(
        agentPlatform = createAgentPlatform(),
        llmOperations = DummyObjectCreatingLlmOperations.Companion.LoremIpsum,
        eventListener = EventSavingAgenticEventListener(),
        operationScheduler = mockk(),
        ragService = RagService.empty(),
    )
}
