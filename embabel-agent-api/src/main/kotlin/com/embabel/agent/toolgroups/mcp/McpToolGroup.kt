package com.embabel.agent.toolgroups.mcp

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.common.util.loggerFor
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback

class McpToolGroup(
    override val metadata: ToolGroupMetadata,
    private val clients: List<McpSyncClient>,
) : ToolGroup {

    override val toolCallbacks: Collection<ToolCallback> = run {
        val provider = SyncMcpToolCallbackProvider(
            clients,
        )
        val toolCallbacks = provider.toolCallbacks.toList()
        loggerFor<McpToolGroup>().info(
            "ToolCallbacks: {}",
            toolCallbacks.map { it.toolDefinition.name() })
        toolCallbacks
    }
}