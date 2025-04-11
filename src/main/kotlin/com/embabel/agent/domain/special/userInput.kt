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
package com.embabel.agent.domain.special

import com.embabel.agent.dsl.Transformer
import com.embabel.agent.primitive.LlmOptions

/**
 * Represents a single user input
 */
data class UserInput(
    val content: String,
)

/**
 * Interface implemented by objects that can be extracted from text using an LLM.
 */
interface Extractable {
    val companion: ExtractableCompanion
}

interface ExtractableCompanion {
    fun extractionAction(llmOptions: LlmOptions): Transformer<UserInput, out Extractable>

}
