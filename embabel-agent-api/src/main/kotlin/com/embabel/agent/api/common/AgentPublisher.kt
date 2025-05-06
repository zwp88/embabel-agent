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

import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentPlatform
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * A Spring bean of this type will be discovered
 * and used to publish agents.
 */
interface AgentPublisher {

    /**
     * Produce a list of agents
     */
    fun agents(): Collection<Agent>

    companion object {
        operator fun invoke(agents: Collection<Agent>): AgentPublisher =
            FixedAgentPublisher(agents)
    }
}

private class FixedAgentPublisher(
    private val agents: Collection<Agent>,
) : AgentPublisher {
    override fun agents(): Collection<Agent> = agents
}

/**
 * Automatically registers all [AgentPublisher] beans
 * with Spring
 */
@Service
class AgentPublisherRegistrar(
    val agentPublishers: List<AgentPublisher>,
    private val agentPlatform: AgentPlatform,
    properties: AgentScanningProperties,
) {

    private val logger = LoggerFactory.getLogger(AgentPublisherRegistrar::class.java)

    val allAgents = agentPublishers.flatMap { it.agents() }

    init {
        logger.debug("Properties: {}", properties)
        if (!properties.publisher) {
            logger.info("AgentPublisher scanning disabled: skipping")
        } else {
            logger.info(
                "AgentPublisher scanning enabled: deploying {} agents from {} AgentPublishers",
                allAgents.size,
                agentPublishers.size,
            )

            allAgents.forEach { agent ->
                agentPlatform.deploy(agent)
            }
        }
    }


}
