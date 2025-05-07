package com.embabel.agent.toolgroups.code

import java.time.Duration

data class BuildStatus(
    val success: Boolean = false,
    val relevantOutput: String = ""
)

data class BuildResult(
    val status: BuildStatus? = null,
    val output: String = "",
    val duration: Duration = Duration.ZERO
)
