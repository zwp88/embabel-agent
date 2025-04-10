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
package com.embabel.agent.primitive

import com.embabel.common.core.types.HasInfoString


/**
 * Options for calling an LLM. Includes model and hyperparameters.
 * Analogous to Spring AI ChatOptions.
 */
interface LlmOptions : HasInfoString {
    val model: String
    val temperature: Double

    companion object {
        operator fun invoke(
            model: String = DEFAULT_MODEL,
            temperature: Double = DEFAULT_TEMPERATURE,
        ): BuildableLlmOptions = BuildableLlmOptions(
            model = model,
            temperature = temperature,
        )

        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_TEMPERATURE = 0.5

        val DEFAULT = LlmOptions()
    }

    override fun infoString(verbose: Boolean?): String {
        return "LlmOptions(model='$model', temperature=$temperature)"
    }
}

data class BuildableLlmOptions(
    override val model: String,
    override val temperature: Double,
) : LlmOptions {

    constructor (llm: LlmOptions) : this(
        model = llm.model,
        temperature = llm.temperature,
    )

    fun withTemperature(temperature: Double): BuildableLlmOptions {
        return copy(temperature = temperature)
    }

    fun withModel(model: String): BuildableLlmOptions {
        return copy(model = model)
    }
}
