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

import com.embabel.agent.domain.library.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.converter.BeanOutputConverter
import kotlin.test.assertFalse

class WithExampleConverterTest {

    @Test
    fun `should convert with example of deserializable interface - ifPossible=false`() {
        val converter = WithExampleConverter(
            BeanOutputConverter<Person>(Person::class.java),
            Person::class.java,
            ifPossible = false,
            generateExamples = true,

            )
        val result = converter.convert("{\"name\": \"foo\"}")
        assertEquals("foo", result!!.name)
    }

    @Test
    fun `should create example of deserializable interface - ifPossible=false`() {
        val converter = WithExampleConverter(
            BeanOutputConverter<Person>(Person::class.java),
            Person::class.java,
            ifPossible = false,
            generateExamples = true,

            )
        assertTrue(converter.getFormat().contains("Example"), "Example not found in format: ${converter.getFormat()}")
    }

    @Test
    fun `should not example of deserializable interface - ifPossible=false`() {
        val converter = WithExampleConverter(
            BeanOutputConverter<Person>(Person::class.java),
            Person::class.java,
            ifPossible = false,
            generateExamples = false,

            )
        assertFalse(
            converter.getFormat().contains("Example"),
            "Example erroneously found in format: ${converter.getFormat()}"
        )
    }

}
