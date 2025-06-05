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