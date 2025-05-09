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