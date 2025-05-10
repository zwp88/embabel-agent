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
package com.embabel.agent.spi.support

import com.embabel.common.core.util.DummyInstanceCreator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.ai.converter.StructuredOutputConverter

/**
 * Add few shot examples to the output converter.
 */
class WithExampleConverter<T>(
    private val delegate: StructuredOutputConverter<T>,
    private val outputClass: Class<T>,
    private val ifPossible: Boolean,
) : StructuredOutputConverter<T> {

    override fun convert(source: String): T? = delegate.convert(source)

    override fun getFormat(): String {
        val example = DummyInstanceCreator.Companion.LoremIpsum.createDummyInstance(outputClass)
        return if (ifPossible) {
            """|
        |Examples:
        |   success:
        |   ${jacksonObjectMapper().writeValueAsString(MaybeReturn(success = example))}
        |
        |   failure:
        |   ${jacksonObjectMapper().writeValueAsString(MaybeReturn<T>(failure = "Insufficient context to create this structure"))}
        |
        |${delegate.format}
        """.trimMargin()
        } else {
            """|
        |Example:
        |${jacksonObjectMapper().writeValueAsString(example)}
        |
        |${delegate.format}
        """.trimMargin()
        }
    }
}
