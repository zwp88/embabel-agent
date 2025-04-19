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
package com.embabel.agent.api.dsl

import com.embabel.agent.core.AgentProcess
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.support.InMemoryBlackboard
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PromptTransformerKtTest {

    @Nested
    inner class PromptTransformer {

        @Test
        fun `test prompt`() {
            val transformer = promptTransformer<MagicVictim, Frog>(name = "frogger") {
                "Turn the person named ${it.input.name} into a frog"
            }
            val magicVictim = MagicVictim(name = "Marmaduke")
            val blackboard = InMemoryBlackboard()
            val frog = Frog(name = "Marmaduke")
            val mockAgentProcess = mockk<AgentProcess>()
            val processContext = mockk<ProcessContext>()
            every { mockAgentProcess.processContext } returns processContext
            every { processContext.blackboard } returns blackboard
            every { processContext.agentProcess } returns mockAgentProcess
            every { processContext.getValue("it", MagicVictim::class.java.name) } returns magicVictim
            val promptSlot = slot<String>()
            every {
                processContext.createObject(
                    capture(promptSlot),
                    any(),
                    Frog::class.java,
                    mockAgentProcess,
                    transformer
                )
            } returns frog
            transformer.execute(processContext, outputTypes = emptyMap(), action = transformer)
        }
    }

}
