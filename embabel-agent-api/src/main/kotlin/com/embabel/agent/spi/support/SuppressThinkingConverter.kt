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
 * A decorator for Spring AI's [StructuredOutputConverter] that cleans up LLM outputs by removing "thinking" blocks.
 *
 * ## Purpose
 * When working with LLMs that use chain-of-thought reasoning, the model may include its reasoning process
 * in the output, often enclosed in <think> tags. While this reasoning is valuable for understanding how
 * the model arrived at its conclusion, it can interfere with structured output parsing.
 *
 * ## Problem Solved
 * Spring AI's [StructuredOutputConverter] is designed to parse structured formats (like JSON) from
 * LLM outputs, but it can fail if the output contains additional text like reasoning blocks.
 * For example, if an LLM returns:
 *
 * ```
 * <think>
 * Let me think about what information to include in this person object.
 * The name should be "John Doe" and the age should be 30.
 * </think>
 * {"name": "John Doe", "age": 30}
 * ```
 *
 * A standard converter would fail to parse this as valid JSON.
 *
 * ## Solution
 * This decorator sanitizes the input by removing any content enclosed in <think> tags before
 * passing it to the delegate converter, allowing reasoning models to be used with Spring AI's
 * structured output functionality.
 *
 * ## Usage
 * Wrap any existing [StructuredOutputConverter] with this class:
 * ```
 * val originalConverter = BeanOutputConverter(Person::class.java)
 * val thinkingAwareConverter = SuppressThinkingConverter(originalConverter)
 * ```
 *
 * @param T The target type that the delegate converter produces
 */
class SuppressThinkingConverter<T>(
    /**
     * The underlying converter that will process the sanitized output.
     * This delegate handles the actual conversion from the cleaned string to the target type T.
     */
    private val delegate: StructuredOutputConverter<T>,
) : StructuredOutputConverter<T> {
    private val logger = LoggerFactory.getLogger(SuppressThinkingConverter::class.java)

    /**
     * Converts the source string to the target type after removing any thinking blocks.
     *
     * This method performs the following steps:
     * 1. Calls [removeThinkBlock] to sanitize the input by removing thinking blocks
     * 2. Logs any detected thinking blocks (for debugging/analysis purposes)
     * 3. Delegates the actual conversion to the wrapped converter
     *
     * @param source The raw string output from the LLM, potentially containing thinking blocks
     * @return The converted object of type T, or null if conversion fails
     */
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

    /**
     * Returns the format description from the delegate converter.
     *
     * This method is part of the [StructuredOutputConverter] interface's [FormatProvider] functionality.
     * The format string provides instructions to the LLM about how to structure its response.
     * This implementation simply forwards to the delegate's format, maintaining the decorator pattern.
     *
     * @return The format description string from the delegate, or null if the delegate doesn't provide one
     */
    override fun getFormat(): String? = delegate.format
}

/**
 * Data class representing the result of sanitizing an input string to remove thinking blocks.
 *
 * This class encapsulates all relevant information about the sanitization process:
 * - The original input (for reference/debugging)
 * - The extracted thinking block (if any was found)
 * - The cleaned output with thinking blocks removed
 *
 * @property input The original, unmodified input string
 * @property thinkBlock The extracted thinking block, or null if none was found
 * @property cleaned The sanitized input with thinking blocks removed
 */
data class ThinkBlockSanitization(
    val input: String,
    val thinkBlock: String?,
    val cleaned: String,
)

/**
 * Removes thinking blocks enclosed in <think> tags from the input string.
 *
 * This function uses a regex pattern to identify and extract content between <think> and </think> tags,
 * then removes these sections from the input string. The regex uses the DOT_MATCHES_ALL option to ensure
 * that newlines within the thinking blocks are properly handled.
 *
 * Example:
 * ```
 * Input: "Hello <think>This is my reasoning</think> World"
 * Output: ThinkBlockSanitization(
 *   input = "Hello <think>This is my reasoning</think> World",
 *   thinkBlock = "<think>This is my reasoning</think>",
 *   cleaned = "Hello  World"
 * )
 * ```
 *
 * @param input The string potentially containing thinking blocks
 * @return A [ThinkBlockSanitization] object containing the original input, extracted thinking block, and cleaned output
 */
fun removeThinkBlock(input: String): ThinkBlockSanitization {
    val regex = "<think>.*?</think>".toRegex(RegexOption.DOT_MATCHES_ALL)
    return ThinkBlockSanitization(
        input = input,
        thinkBlock = regex.find(input)?.value,
        cleaned = input.replace(regex, ""),
    )
}
