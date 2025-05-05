package com.embabel.agent.toolgroups.code

class MavenBuildSystemIntegration : BuildSystemIntegration {

    override fun parseBuildOutput(root: String, rawOutput: String): BuildStatus? {

        // TODO messy test
        if (!rawOutput.contains("[INFO]")) {
            // Not a Maven build
            return null
        }
        val success = rawOutput.contains("BUILD SUCCESS")
        val warnings = rawOutput.lines().filter { it.contains("[WARNING]") }.joinToString("\n")
        val errors = rawOutput.lines().filter { it.contains("[ERROR]") }.joinToString("\n")
        val relevantOutput = "$warnings\n$errors"
        return BuildStatus(
            success = success,
            relevantOutput = relevantOutput,
        )
    }
}