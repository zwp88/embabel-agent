package com.embabel.agent.a2a.server

import com.embabel.agent.a2a.spec.AgentCard
import com.embabel.agent.a2a.spec.JSONRPCRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@ConfigurationProperties(prefix = "embabel.a2a")
data class A2aConfig(
    val host: String = "host.docker.internal",
    val port: Int = 8080,
)

/**
 * Registers A2A endpoints for the agent-to-agent communication protocol.
 */
@Component
@Profile("a2a")
class A2AEndpointRegistrar(
    private val agentCardHandlers: List<AgentCardHandler>,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val a2aConfig: A2aConfig,
) {

    private val logger = LoggerFactory.getLogger(A2AEndpointRegistrar::class.java)

    @EventListener
    fun onApplicationReady(event: ApplicationReadyEvent) {
        logger.info("Registering ${agentCardHandlers.size} A2A endpoints")

        agentCardHandlers.forEach { endpoint ->
            registerWebEndpoints(endpoint)
        }
    }

    private fun registerWebEndpoints(agentCardHandler: AgentCardHandler) {
        val endpointPath = "/${agentCardHandler.path}/.well-known/agent.json"
        logger.info(
            "Registering web endpoint under {} for {}",
            endpointPath,
            agentCardHandler.infoString(verbose = true),
        )
        val agentCardGetMapping = RequestMappingInfo.paths(endpointPath)
            .methods(RequestMethod.GET)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build()
        val ach = AgentCardHandlerHolder(
            agentCardHandler.agentCard(
                scheme = "http", // or "https" based on your configuration
                host = a2aConfig.host,
                port = a2aConfig.port,
            )
        )
        requestMappingHandlerMapping.registerMapping(
            agentCardGetMapping,
            ach,
            ach::class.java.getMethod("agentCard"),
        )

        // Register POST mapping for JSON-RPC
        val jsonRpcPostMethod = agentCardHandler.javaClass.getMethod(
            "handleJsonRpc",
            JSONRPCRequest::class.java,
        )
        val jsonRpcPostMapping = RequestMappingInfo.paths(agentCardHandler.path)
            .methods(RequestMethod.POST)
            .consumes(MediaType.APPLICATION_JSON_VALUE)
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .build()
        requestMappingHandlerMapping.registerMapping(jsonRpcPostMapping, agentCardHandler, jsonRpcPostMethod)
    }
}

private class AgentCardHandlerHolder(
    val agentCard: AgentCard,
) {

    @ResponseBody
    fun agentCard(): ResponseEntity<AgentCard> {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(agentCard)
    }
}