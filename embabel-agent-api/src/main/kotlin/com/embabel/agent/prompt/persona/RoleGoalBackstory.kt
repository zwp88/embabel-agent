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
