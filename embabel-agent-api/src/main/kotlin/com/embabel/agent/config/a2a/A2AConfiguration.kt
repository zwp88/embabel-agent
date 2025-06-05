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
package com.embabel.agent.config.a2a

import com.embabel.agent.a2a.server.AgentCardHandler
import com.embabel.agent.a2a.server.DefaultAgentCardHandler
import com.embabel.agent.a2a.server.support.AutonomyA2AMessageHandler
import com.embabel.agent.core.AgentPlatform
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("a2a")
class A2AConfiguration {

    @Bean
    fun a2AMessageHandler(
        agentPlatform: AgentPlatform,
        a2aMessageHandler: AutonomyA2AMessageHandler,
    ): AgentCardHandler {
        return DefaultAgentCardHandler(
            path = "a2a",
            agentPlatform = agentPlatform,
            a2aMessageHandler = a2aMessageHandler,
        )
    }
}
