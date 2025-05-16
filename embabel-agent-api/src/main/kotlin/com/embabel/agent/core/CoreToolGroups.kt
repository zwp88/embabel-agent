package com.embabel.agent.core

/**
 * Core tool groups exposed by the platform
 */
object CoreToolGroups {

    const val WEB = "web"

    val WEB_DESCRIPTION = ToolGroupDescription(
        description = "Tools for web search and scraping",
        role = WEB,
    )

    const val RAG = "rag"

    val RAG_DESCRIPTION = ToolGroupDescription(
        description = "RAG query tools",
        role = RAG,
    )

    const val MATH = "math"

    val MATH_DESCRIPTION = ToolGroupDescription(
        description = "Math tools: use when you need to perform calculations",
        role = MATH,
    )

    const val GITHUB = "github"

    val GITHUB_DESCRIPTION = ToolGroupDescription(
        description = "Integration with GitHub APIs",
        role = GITHUB,
    )

    const val BROWSER_AUTOMATION = "browser_automation"

    val BROWSER_AUTOMATION_DESCRIPTION = ToolGroupDescription(
        description = "Browser automation tools",
        role = BROWSER_AUTOMATION,
    )
}