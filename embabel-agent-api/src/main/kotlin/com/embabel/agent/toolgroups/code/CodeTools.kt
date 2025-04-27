package com.embabel.agent.toolgroups.code

import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import java.nio.file.Path
import java.nio.file.Paths

class CodeTools(
    val root: String,
) {

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