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
package com.embabel.agent.config


import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.toolgroups.code.CiTools
import com.embabel.agent.toolgroups.file.FileTools
import com.embabel.agent.toolgroups.mcp.McpToolGroup
import io.modelcontextprotocol.client.McpSyncClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ToolGroupsConfiguration(
    private val mcpSyncClients: List<McpSyncClient>,
) {

    private val logger = LoggerFactory.getLogger(ToolGroupsConfiguration::class.java)

    init {
        logger.info(
            "MCP is available. Found {} clients: {}",
            mcpSyncClients.size,
            mcpSyncClients.map { it.serverInfo }.joinToString("\n"),
        )
    }

    @Bean
    fun fileToolsGroup(): ToolGroup =
        FileTools.toolGroup(root = System.getProperty("user.dir") + "/embabel-agent-api")

    @Bean
    fun ciToolsGroup(): ToolGroup =
        CiTools.toolGroup(root = System.getProperty("user.dir") + "/embabel-agent-api")

    @Bean
    fun mcpWebToolsGroup(): ToolGroup {
        val wikipediaTools = setOf(
            "get_related_topics",
            "get_summary",
            "get_article",
            "search_wikipedia",
        )
        return McpToolGroup(
            description = ToolGroup.WEB_DESCRIPTION,
            artifact = "docker-web",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            filter = {
                it.toolDefinition.name().contains("brave") || it.toolDefinition.name().contains("fetch") ||
                        wikipediaTools.any { wt -> it.toolDefinition.name().contains(wt) }
            },
        )
    }


    private val githubTools = listOf(
        "add_issue_comment",
    )

    @Bean
    fun browserAutomationWebToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = ToolGroup.BROWSER_AUTOMATION_DESCRIPTION,
            artifact = "docker-puppeteer",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            // TODO : add filter for GitHub tools
            filter = { it.toolDefinition.name().contains("puppeteer") },
        )
    }

}
