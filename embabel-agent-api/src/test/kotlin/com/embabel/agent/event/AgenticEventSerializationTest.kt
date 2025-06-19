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
package com.embabel.agent.event

import com.embabel.agent.api.dsl.evenMoreEvilWizard
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.integration.IntegrationTestUtils.dummyAgentPlatform
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Check that we can safely serialize AgentProcessEvent.
 * We don't need to deserialize it.
 */
class AgenticEventSerializationTest {

    @Test
    fun `test process events can be serialized`() {
        val om = jacksonObjectMapper().registerModule(JavaTimeModule())
        val serializingListener = object : AgenticEventListener {
            var count = 0
            override fun onProcessEvent(event: AgentProcessEvent) {
                val s = om.writeValueAsString(event)
                count++
                loggerFor<AgenticEventSerializationTest>().info("Serialized event: $s")
                assertTrue(s.contains("\"processId\""), "Process id is required")
            }
        }
        val ap = dummyAgentPlatform(listener = serializingListener)
        // If it doesn't die we're happy
        ap.runAgentWithInput(evenMoreEvilWizard(), input = UserInput("anything at all"))
        assertTrue(serializingListener.count > 0, "Events were serialized")
    }

    @Test
    fun `test platform events can be serialized`() {
        val om = jacksonObjectMapper().registerModule(JavaTimeModule())
        val serializingListener = object : AgenticEventListener {
            var count = 0
            override fun onPlatformEvent(event: AgentPlatformEvent) {
                val s = om.writeValueAsString(event)
                count++
                loggerFor<AgenticEventSerializationTest>().info("Serialized event: $s")
            }
        }
        val ap = dummyAgentPlatform(listener = serializingListener)
        // If it doesn't die we're happy
        ap.deploy(evenMoreEvilWizard())
        assertTrue(serializingListener.count > 0, "Events were serialized")
    }

}
