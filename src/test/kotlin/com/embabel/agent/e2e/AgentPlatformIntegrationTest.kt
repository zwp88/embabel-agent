/*
 * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.agent.e2e

import com.embabel.agent.AgentPlatform
import com.embabel.agent.ProcessOptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration tests
 */
@SpringBootTest
class AgentPlatformIntegrationTest(
    @Autowired
    private val agentPlatform: AgentPlatform,
) {

    @Test
    fun `agent starts up`() {
        // Nothing to test
    }

    @Test
    @Disabled("Not yet ready")
    fun `run star finder agent`() {
        agentPlatform.chooseAndAccomplishGoal(
            "Lynda is a Scorpio, find some news for her",
            ProcessOptions(test = true),
        )
    }
}
