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
package com.embabel.agent.dsl

import com.embabel.agent.ProcessContext


interface PromptMaker {

    fun makePrompt(processContext: ProcessContext): String
}

/**
 * Inspired by crew.ai. Not that we think this is necessarily
 * the best way to structure team members, but it's helpful to
 * show
 */
data class Role(
    val role: String,
    val goal: String,
    val backstory: String,
) : PromptMaker {

    override fun makePrompt(processContext: ProcessContext): String {
        return """
            You are a $role.
            Your goal is to $goal.
            Your backstory is:
            $backstory
        """.trimIndent()
    }
}
