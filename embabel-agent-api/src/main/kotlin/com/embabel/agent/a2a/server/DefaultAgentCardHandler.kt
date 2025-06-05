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
package com.embabel.agent.a2a.server

import com.embabel.agent.a2a.server.support.FromGoalsAgentSkillFactory
import com.embabel.agent.a2a.spec.*
import com.embabel.agent.core.AgentPlatform
import com.embabel.common.core.types.Semver.Companion.DEFAULT_VERSION
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestBody

/**
 * Expose A2A endpoints for the agent-to-agent communication protocol.
 */
class DefaultAgentCardHandler(
    override val path: String,
    private val agentPlatform: AgentPlatform,
    private val a2aMessageHandler: A2AMessageHandler,
) : AgentCardHandler {

    private val logger = LoggerFactory.getLogger(DefaultAgentCardHandler::class.java)

    override fun agentCard(
        scheme: String,
        host: String,
        port: Int,
    ): AgentCard {

        val hostingUrl = "$scheme://$host:$port/$path"

        val agentCard = AgentCard(
            name = agentPlatform.name,
            description = agentPlatform.description,
            url = hostingUrl,
            provider = AgentProvider("Embabel", "https://embabel.com"),
            version = DEFAULT_VERSION,
            documentationUrl = "https://embabel.com/docs",
            capabilities = AgentCapabilities(
                streaming = false,
                pushNotifications = false,
                stateTransitionHistory = false,
            ),
            securitySchemes = null,
            security = null,
            defaultInputModes = listOf("application/json", "text/plain"),
            defaultOutputModes = listOf("application/json", "text/plain"),
            skills = FromGoalsAgentSkillFactory(agentPlatform.goals).skills(agentPlatform.name),
            supportsAuthenticatedExtendedCard = false,
        )
        logger.info("Returning agent card: {}", agentCard)
        return agentCard
    }

    /**
     * Handle JSON-RPC requests for A2A messages.
     */
    override fun handleJsonRpc(
        @RequestBody @Schema(description = "A2A message send params")
        request: JSONRPCRequest,
    ): JSONRPCResponse {
        return a2aMessageHandler.handleJsonRpc(request)
    }

    override fun infoString(verbose: Boolean?): String {
        return "DefaultAgentCardHandler(path='$path', agentPlatform=${agentPlatform.name})"
    }
}
