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
package com.embabel.examples.dogfood.presentation

import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.IntegrationTestUtils.dummyAgentPlatform
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import kotlin.test.assertEquals

class PresentationMakerTest {

    @Test
    fun `agent runs`() {
        val mockEnvironment = mockk<Environment>()
        val mockFilePersister = mockk<FilePersister>()
        every { mockFilePersister.saveFile(any(), any(), any()) } returns Unit
        val mockSlideFormatter = mockk<SlideFormatter>()
        every { mockSlideFormatter.createHtmlSlides(any(), any()) } returns "presentation.html"
        every { mockEnvironment.activeProfiles } returns arrayOf("test")
        val agent: Agent = AgentMetadataReader().createAgentMetadata(
            PresentationMaker(
                properties = PresentationMakerProperties(
                    slideCount = 10,
                ),
                filePersister = mockFilePersister,
                slideFormatter = mockSlideFormatter,
            ),
        ) as Agent
        val ap = dummyAgentPlatform()
        val processOptions = ProcessOptions()
        val result = ap.runAgentWithInput(
            agent = agent,
            processOptions = processOptions,
            input = UserInput("do something"),
        )
        assertEquals(AgentProcessStatusCode.COMPLETED, result.status)
//        assertEquals(
//            3,
//            result.processContext.agentProcess.history.size,
//            "Expected history:\nActual:\n${result.processContext.agentProcess.history.joinToString("\n")}"
//        )
        assertTrue(result.lastResult() is Deck)
        assertTrue(
            result.objects.filterIsInstance<ResearchTopics>().isNotEmpty(),
            "Should have ResearchTopics"
        )
    }

}
