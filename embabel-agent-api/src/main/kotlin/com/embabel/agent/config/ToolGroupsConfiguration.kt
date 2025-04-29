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
import com.embabel.agent.core.ToolGroupMetadata
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.toolgroups.code.CiTools
import com.embabel.agent.toolgroups.file.FileTools
import com.embabel.agent.toolgroups.web.crawl.JSoupWebCrawler
import com.embabel.agent.toolgroups.web.domain.WebScraperTools
import com.embabel.agent.toolgroups.web.search.brave.BraveNewsSearchService
import com.embabel.agent.toolgroups.web.search.brave.BraveVideoSearchService
import com.embabel.agent.toolgroups.web.search.brave.BraveWebSearchService
import com.embabel.agent.toolgroups.web.search.brave.braveSearchTools
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ToolGroupsConfiguration(
) {

    private val logger = LoggerFactory.getLogger(ToolGroupsConfiguration::class.java)

    @ConditionalOnBean(BraveWebSearchService::class)
    @Bean
    fun webtoolsGroup(
        braveWebSearchService: BraveWebSearchService,
        braveNewsSearchService: BraveNewsSearchService,
        braveVideoSearchService: BraveVideoSearchService,
    ): ToolGroup {
        logger.info("Brave search is available. Creating web tools group.")
        val braveSearchTools = braveSearchTools(
            braveWebSearchService,
            braveNewsSearchService,
            braveVideoSearchService,
        )
        val scraper = ToolCallbacks.from(WebScraperTools(JSoupWebCrawler(maxDepth = 3))).toList()
        return ToolGroup(
            metadata = ToolGroupMetadata(
                description = ToolGroup.WEB_DESCRIPTION,
                artifact = "embabel-web",
                provider = "Embabel",
                permissions = setOf(
                    ToolGroupPermission.INTERNET_ACCESS,
                )
            ),
            toolCallbacks = scraper + braveSearchTools
        )
    }

    @Bean
    fun fileToolsGroup(): ToolGroup =
        FileTools.toolGroup(root = System.getProperty("user.dir") + "/embabel-agent-api")

    @Bean
    fun ciToolsGroup(): ToolGroup =
        CiTools.toolGroup(root = System.getProperty("user.dir") + "/embabel-agent-api")

}
