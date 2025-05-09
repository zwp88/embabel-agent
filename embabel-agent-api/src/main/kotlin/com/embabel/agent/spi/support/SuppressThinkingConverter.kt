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

import org.slf4j.LoggerFactory
import org.springframework.ai.converter.StructuredOutputConverter

/**
 * Clean up input strings to remove think blocks.
 * Allows us to use reasoning models for structured return
 */
class SuppressThinkingConverter<T>(
    private val delegate: StructuredOutputConverter<T>,
) : StructuredOutputConverter<T> {
    private val logger = LoggerFactory.getLogger(SuppressThinkingConverter::class.java)

    override fun convert(source: String): T? {
        val sanitization = removeThinkBlock(source)
        sanitization.thinkBlock?.let {
            logger.info(
                "Think block detected in input: {}",
                it,
            )
        }
        return delegate.convert(sanitization.cleaned)
    }

    override fun getFormat(): String? = delegate.format
}

data class ThinkBlockSanitization(
    val input: String,
    val thinkBlock: String?,
    val cleaned: String,
)

fun removeThinkBlock(input: String): ThinkBlockSanitization {
    val regex = "<think>.*?</think>".toRegex(RegexOption.DOT_MATCHES_ALL)
    return ThinkBlockSanitization(
        input = input,
        thinkBlock = regex.find(input)?.value,
        cleaned = input.replace(regex, ""),
    )
}
