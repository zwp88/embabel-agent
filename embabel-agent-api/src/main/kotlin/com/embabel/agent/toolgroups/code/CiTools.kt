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
package com.embabel.agent.toolgroups.code

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.spi.support.SelfToolCallbackPublisher
import com.embabel.agent.spi.support.SelfToolGroup
import com.embabel.agent.toolgroups.DirectoryBased
import org.springframework.ai.tool.annotation.Tool

interface CiTools : SelfToolCallbackPublisher, DirectoryBased {

    @Tool(description = "build the project using the given command in the root")
    fun buildProject(command: String): String {
        return Ci(root).build(command)
    }

    companion object {
        fun toolGroup(root: String): ToolGroup = object : CiTools, SelfToolGroup {
            override val root: String = root

            override val description
                get() = ToolGroup.CI_DESCRIPTION

            override val permissions get() = setOf(ToolGroupPermission.HOST_ACCESS)

        }
    }
}
