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
import com.embabel.agent.spi.support.SelfToolGroup
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import java.nio.file.Path
import java.nio.file.Paths

class CodeTools(
    val root: String,
) : SelfToolGroup {

    override val description = ToolGroup.CODE_DESCRIPTION

    private val logger = LoggerFactory.getLogger(CodeTools::class.java)

    /**
     * Resolves a relative path against the root directory
     * Prevents path traversal attacks by ensuring the resolved path is within the root
     */
    private fun resolvePath(path: String): Path {
        val basePath = Paths.get(root).toAbsolutePath().normalize()
        val resolvedPath = basePath.resolve(path).normalize().toAbsolutePath()

        if (!resolvedPath.startsWith(basePath)) {
            throw SecurityException("Path traversal attempt detected: $path")
        }
        return resolvedPath
    }

    @Tool(description = "build the project using the given command in the root")
    fun buildProject(command: String): String {
        TODO("take a command like mvn test and run in root. return output")
    }

}
