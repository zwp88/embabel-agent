package com.embabel.agent.spi.support

import com.embabel.agent.core.AgentProcess
import com.embabel.agent.spi.AgentProcessRepository

/**
 * Naive in-memory implementation of [AgentProcessRepository].
 */
class InMemoryAgentProcessRepository : AgentProcessRepository {

    private val map: MutableMap<String, AgentProcess> = mutableMapOf()

    override fun findById(id: String): AgentProcess? = map[id]

    override fun save(agentProcess: AgentProcess): AgentProcess {
        map[agentProcess.id] = agentProcess
        return agentProcess
    }

    override fun delete(agentProcess: AgentProcess) {
        map -= agentProcess.id
    }
}