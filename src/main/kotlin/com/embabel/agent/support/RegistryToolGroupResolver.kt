/*
                                * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.agent.support

import com.embabel.agent.ToolGroup
import com.embabel.agent.ToolGroupMetadata
import com.embabel.agent.ToolGroupResolution
import com.embabel.agent.spi.ToolGroupResolver
import org.slf4j.LoggerFactory

/**
 * Resolves
 */
class RegistryToolGroupResolver(
    override val name: String,
    val toolGroups: List<ToolGroup>,
) : ToolGroupResolver {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info(
            "{}: {} available tool groups: {}",
            name,
            availableToolGroups().size,
            "\n\t" + availableToolGroups().joinToString("\n"),
        )
    }

    override fun availableToolGroups(): List<ToolGroupMetadata> = toolGroups.map { it.metadata }

    override fun resolveToolGroup(role: String): ToolGroupResolution {
        val group = toolGroups.find { it.metadata.role == role }
        if (group == null) {
            return ToolGroupResolution(
                resolvedToolGroup = null,
                failureMessage = "No tool group matching role '$role'",
            )
        } else {
            return ToolGroupResolution(
                resolvedToolGroup = group,
            )
        }
    }
}
