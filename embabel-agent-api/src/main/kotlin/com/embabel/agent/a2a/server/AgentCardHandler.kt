package com.embabel.agent.a2a.server

import com.embabel.agent.a2a.spec.AgentCard
import com.embabel.agent.a2a.spec.JSONRPCRequest
import com.embabel.agent.a2a.spec.JSONRPCResponse
import com.embabel.common.core.types.HasInfoString

/**
 * Exposes an A2A AgentCard and handles JSON-RPC requests routed to its path
 */
interface AgentCardHandler : HasInfoString {

    /**
     * Relative path below root
     */
    val path: String


    /**
     * Returns the agent card for the A2A server.
     */
    fun agentCard(scheme: String, host: String, port: Int): AgentCard

    /**
     * Handles JSON-RPC requests for A2A messages.
     */
    fun handleJsonRpc(request: JSONRPCRequest): JSONRPCResponse
}