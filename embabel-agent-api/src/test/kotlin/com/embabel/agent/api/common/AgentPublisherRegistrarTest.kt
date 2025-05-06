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

import com.embabel.agent.api.dsl.EvilWizardAgent
import com.embabel.agent.core.AgentPlatform
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AgentPublisherRegistrarTest {

    @Test
    fun `no agents`() {
        val mtAgentPublisher = AgentPublisher(agents = emptyList())
        val agentPublisherRegistrar = AgentPublisherRegistrar(
            agentPublishers = listOf(mtAgentPublisher),
            agentPlatform = mockk(),
            properties = AgentScanningProperties(
                annotation = false,
                publisher = true,
            ),
        )
        val allAgents = agentPublisherRegistrar.allAgents
        assert(allAgents.isEmpty())
    }

    @Test
    fun `no publication because disabled`() {
        val agentPublisher = AgentPublisher(agents = listOf(EvilWizardAgent))
        val agentPublisherRegistrar = AgentPublisherRegistrar(
            agentPublishers = listOf(agentPublisher),
            agentPlatform = mockk(),
            properties = AgentScanningProperties(
                annotation = false,
                publisher = false,
            ),
        )
        val allAgents = agentPublisherRegistrar.allAgents
        assertEquals(1, allAgents.size)
        // Should not have deployed
    }

    @Test
    fun publication() {
        val agentPublisher = AgentPublisher(agents = listOf(EvilWizardAgent))
        val mockAgentPlatform = mockk<AgentPlatform>()
        every { mockAgentPlatform.deploy(any()) } returns mockAgentPlatform
        val agentPublisherRegistrar = AgentPublisherRegistrar(
            agentPublishers = listOf(agentPublisher),
            agentPlatform = mockAgentPlatform,
            properties = AgentScanningProperties(
                annotation = false,
                publisher = true,
            ),
        )
        val allAgents = agentPublisherRegistrar.allAgents
        assertEquals(1, allAgents.size)
        // Should have deployed
        verify(exactly = 1) { mockAgentPlatform.deploy(EvilWizardAgent) }
    }

}
