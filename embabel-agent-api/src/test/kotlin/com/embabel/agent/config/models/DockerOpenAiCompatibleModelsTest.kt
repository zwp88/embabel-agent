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
package com.embabel.agent.config.models

import io.mockk.mockk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DockerOpenAiCompatibleModelsTest {

    @Nested
    inner class EnvironmentVariableParsing {

        @Test
        fun `no eligible models`() {
            val env = emptyMap<String, String>()
            val models = DockerOpenAiCompatibleModels(mockk()).findModelsFromEnvironment(env)
            assert(models.isEmpty()) { "No models should be found" }
        }

        @Test
        @Disabled
        fun `one eligible model`() {
            val env = mapOf(
                "LLAMA_MODEL" to "AI_LAMA_3.2",
                "LLAMA_URL" to "http://thingwhatever/model",
            )
            val models = DockerOpenAiCompatibleModels(mockk()).findModelsFromEnvironment(env)
            assert(models.isEmpty()) { "No models should be found" }
        }
    }

}
