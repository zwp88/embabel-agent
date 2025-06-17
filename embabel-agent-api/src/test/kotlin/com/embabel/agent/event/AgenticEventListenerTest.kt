package com.embabel.agent.event

import com.embabel.agent.api.dsl.evenMoreEvilWizard
import com.embabel.agent.testing.IntegrationTestUtils.dummyAgentPlatform
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AgenticEventListenerTest {

    @Nested
    inner class MulticastAgenticEventListenerTest {

        @Test
        fun `can recover from errors in platform event`() {
            val l1 = object : AgenticEventListener {
                override fun onPlatformEvent(event: AgentPlatformEvent) {
                    TODO()
                }
            }
            val ml = AgenticEventListener.of(l1)
            ml.onPlatformEvent(
                AgentDeploymentEvent(
                    agentPlatform = dummyAgentPlatform(),
                    agent = evenMoreEvilWizard(),
                )
            )
        }

        @Test
        fun `can recover from errors in process event`() {
            val l1 = object : AgenticEventListener {
                override fun onProcessEvent(event: AgentProcessEvent) {
                    TODO()
                }
            }
            val ml = AgenticEventListener.of(l1)
            ml.onProcessEvent(
                AgentProcessCreationEvent(
                    agentProcess = mockk(relaxed = true),
                )
            )
        }
    }

}