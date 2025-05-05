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
package com.embabel.agent.toolgroups.mcp

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.common.util.loggerFor
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback

class McpToolGroup(
    description: ToolGroupDescription,
    provider: String,
    artifact: String,
    permissions: Set<ToolGroupPermission>,
    private val clients: List<McpSyncClient>,
    filter: ((ToolCallback) -> Boolean),
) : ToolGroup {

    override val metadata: ToolGroupMetadata = ToolGroupMetadata(
        description = description,
        artifact = artifact,
        provider = provider,
        version = "0.0.1",
        permissions = permissions,
    )

    override val toolCallbacks: Collection<ToolCallback> = run {
        val provider = SyncMcpToolCallbackProvider(
            clients,
        )
        val toolCallbacks = provider.toolCallbacks.filter(filter)
        loggerFor<McpToolGroup>().debug(
            "ToolGroup role={}: {}",
            description.role,
            toolCallbacks.map { it.toolDefinition.name() })
        toolCallbacks
    }
}
