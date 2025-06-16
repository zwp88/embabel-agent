package com.embabel.agent.prompt.persona

import com.embabel.common.ai.prompt.PromptContributor

/**
 * Crew AI style backstory prompt.
 * Included for users migrating from Crew AI.
 * In Embabel, such structures aren't core to the framework,
 * but merely a PromptContributor that can be used
 * in any action implementation.
 */
data class RoleGoalBackstory(
    override val role: String,
    val goal: String,
    val backstory: String,
) : PromptContributor {

    override fun contribution(): String = """
        Role: $role
        Goal: $goal
        Backstory: $backstory
    """.trimIndent()
}