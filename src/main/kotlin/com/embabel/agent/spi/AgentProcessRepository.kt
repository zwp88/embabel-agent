package com.embabel.agent.spi

import com.embabel.agent.core.AgentProcess
import org.springframework.data.repository.Repository

interface AgentProcessRepository : Repository<AgentProcess, String> {

    fun findById(id: String): AgentProcess?

    fun save(agentProcess: AgentProcess): AgentProcess

    fun delete(agentProcess: AgentProcess)
}