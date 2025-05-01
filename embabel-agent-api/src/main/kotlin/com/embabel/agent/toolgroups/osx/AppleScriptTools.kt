package com.embabel.agent.toolgroups.osx

import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.spi.support.SelfToolGroup
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service

@Service
class AppleScriptTools : SelfToolGroup {

    override val description: ToolGroupDescription
        get() = ToolGroupDescription(role = "AppleScript", description = "Run AppleScript commands")

    override val permissions: Set<ToolGroupPermission>
        get() = setOf(ToolGroupPermission.HOST_ACCESS)

    @Tool(description = "Run AppleScript command")
    fun runAppleScript(script: String) {
        val runtime = Runtime.getRuntime()
        val process = runtime.exec(arrayOf("osascript", "-e", script))
        // You can handle output/errors if needed
        val exitCode = process.waitFor()
        println("Script executed with exit code: $exitCode")
    }
}