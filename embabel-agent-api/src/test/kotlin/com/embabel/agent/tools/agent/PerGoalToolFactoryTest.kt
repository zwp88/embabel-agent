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
package com.embabel.agent.tools.agent

import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.api.common.autonomy.AutonomyProperties
import com.embabel.agent.api.dsl.evenMoreEvilWizard
import com.embabel.agent.api.dsl.evenMoreEvilWizardWithStructuredInput
import com.embabel.agent.api.dsl.exportedEvenMoreEvilWizard
import com.embabel.agent.api.dsl.userInputToFrogOrPersonBranch
import com.embabel.agent.testing.integration.IntegrationTestUtils
import com.embabel.agent.testing.integration.RandomRanker
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.junit.jupiter.api.Assertions.*


class PerGoalToolFactoryTest {

    @Test
    fun `test local export by default`() {
        val agentPlatform = IntegrationTestUtils.dummyAgentPlatform()
        agentPlatform.deploy(evenMoreEvilWizard())
        agentPlatform.deploy(userInputToFrogOrPersonBranch())
        val autonomy = Autonomy(agentPlatform, RandomRanker(), AutonomyProperties())

        val provider = PerGoalToolCallbackFactory(autonomy, jacksonObjectMapper(), "testApp")

        val toolCallbacks = provider.toolCallbacks(remoteOnly = false, listeners = emptyList())
        assertEquals(
            3, toolCallbacks.size,
            "Should not have 1 tool callback with no export defined: have ${toolCallbacks.map { it.toolDefinition.name() }}"
        )
    }

    @Test
    fun `test no remote export by default`() {
        val agentPlatform = IntegrationTestUtils.dummyAgentPlatform()
        agentPlatform.deploy(evenMoreEvilWizard())
        agentPlatform.deploy(userInputToFrogOrPersonBranch())
        val autonomy = Autonomy(agentPlatform, RandomRanker(), AutonomyProperties())

        val provider = PerGoalToolCallbackFactory(autonomy, jacksonObjectMapper(), "testApp")

        val toolCallbacks = provider.toolCallbacks(remoteOnly = true, listeners = emptyList())
        assertEquals(
            0,
            toolCallbacks.size,
            "Should not have any tool callbacks with no export defined: ${toolCallbacks.map { it.toolDefinition.name() }}"
        )
    }

    @Test
    fun `test explicit remote export`() {
        val agentPlatform = IntegrationTestUtils.dummyAgentPlatform()
        agentPlatform.deploy(exportedEvenMoreEvilWizard())
        agentPlatform.deploy(userInputToFrogOrPersonBranch())
        val autonomy = Autonomy(agentPlatform, RandomRanker(), AutonomyProperties())

        val provider = PerGoalToolCallbackFactory(autonomy, jacksonObjectMapper(), "testApp")

        val toolCallbacks = provider.toolCallbacks(remoteOnly = true, listeners = emptyList())
        assertEquals(
            3,
            toolCallbacks.size,
            "Should have tool callbacks with export defined: ${toolCallbacks.map { it.toolDefinition.name() }}"
        )
    }

    @Test
    fun `test user input function per goal`() {
        val agentPlatform = IntegrationTestUtils.dummyAgentPlatform()
        agentPlatform.deploy(exportedEvenMoreEvilWizard())
        agentPlatform.deploy(userInputToFrogOrPersonBranch())
        val autonomy = Autonomy(agentPlatform, RandomRanker(), AutonomyProperties())

        val provider = PerGoalToolCallbackFactory(autonomy, jacksonObjectMapper(), "testApp")

        val toolCallbacks = provider.toolCallbacks(remoteOnly = false, listeners = emptyList())

        assertNotNull(toolCallbacks)
        assertEquals(
            autonomy.agentPlatform.goals.size + 1,
            toolCallbacks.size,
            "Should have one tool callback per goal plus continue"
        )

        for (toolCallback in toolCallbacks) {
            assertFalse(
                toolCallback.toolDefinition.inputSchema().contains("timestamp"),
                "Tool callback should not have timestamp in input schema: ${toolCallback.toolDefinition.inputSchema()}"
            )
            val toolDefinition = toolCallback.toolDefinition
            if (toolCallback.toolDefinition.name()
                    .contains(FORM_SUBMISSION_TOOL_NAME) || toolCallback.toolDefinition.name()
                    .contains(CONFIRMATION_TOOL_NAME)
            ) {
                // This is a special case
                break
            }
            val goal = autonomy.agentPlatform.goals.find { toolCallback.toolDefinition.name().contains(it.name) }
            assertNotNull(
                goal,
                "Tool callback should correspond to a platform goal: Offending tool callback: $toolCallback"
            )
            assertNotNull(toolCallback.toolDefinition.inputSchema(), "Should have generated schema")
        }
    }

    @Test
    fun `test structured input type function for goal`() {
        val agentPlatform = IntegrationTestUtils.dummyAgentPlatform()
        agentPlatform.deploy(evenMoreEvilWizardWithStructuredInput())
        val autonomy = Autonomy(agentPlatform, RandomRanker(), AutonomyProperties())

        val provider = PerGoalToolCallbackFactory(autonomy, jacksonObjectMapper(), "testApp")
        val toolCallbacks = provider.toolCallbacks(remoteOnly = false, listeners = emptyList())

        assertNotNull(toolCallbacks)
        assertEquals(
            2 + 1, // 2 functions for the goal + 1 continuation
            toolCallbacks.size,
            "Should have 2 tool callback for the one goal plus continuation: Have ${toolCallbacks.map { it.toolDefinition.name() }}"
        )

        // Tool callbacks should have distinct names
        val toolNames = toolCallbacks.map { it.toolDefinition.name() }
        assertEquals(
            toolNames.toSet().size,
            toolNames.size,
            "Tool callbacks should have distinct names: $toolNames"
        )

        for (toolCallback in toolCallbacks) {
            if (toolCallback.toolDefinition.name()
                    .contains(FORM_SUBMISSION_TOOL_NAME) || toolCallback.toolDefinition.name()
                    .contains(CONFIRMATION_TOOL_NAME)
            ) {
                // This is a special case
                break
            }

            assertFalse(
                toolCallback.toolDefinition.inputSchema().contains("timestamp"),
                "Tool callback should not have timestamp in input schema: ${toolCallback.toolDefinition.inputSchema()}"
            )
            val toolDefinition = toolCallback.toolDefinition
            val goalName = toolDefinition.name()
            val goal = autonomy.agentPlatform.goals.find { toolDefinition.name().contains(it.name) }
            assertNotNull(
                goal,
                "Tool callback should correspond to a platform goal: $goalName, Offending tool callback: ${toolCallback.toolDefinition.name()}",
            )
            assertNotNull(toolCallback.toolDefinition.inputSchema(), "Should have generated schema")
        }
    }

}
