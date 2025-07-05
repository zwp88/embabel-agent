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
package com.embabel.agent.core.support

import com.embabel.agent.api.annotation.support.PersonWithReverseTool
import com.embabel.agent.api.common.ToolObject
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallback
import kotlin.test.assertEquals

class SpringAiUtilsKtTest {

    @Test
    fun `safelyGetTools from empty collection`() {
        val result = safelyGetToolCallbacks(emptySet())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `safelyGetTools from single instance in set`() {
        val result = safelyGetToolCallbacks(
            setOf(
                ToolObject(PersonWithReverseTool("John Doe"))
            )
        )
        assertEquals(1, result.size)
    }

    @Test
    fun `safelyGetTools from single instance in ArrayList`() {
        val result = safelyGetToolCallbacks(arrayListOf(ToolObject.from(PersonWithReverseTool("John Doe"))))
        assertEquals(1, result.size)
    }

    @Test
    fun `safelyGetTools from single instance and tool callback`() {
        val tc = mockk<ToolCallback>()
        every { tc.toolDefinition.name() } returns "test"
        val result = safelyGetToolCallbacks(setOf(tc, PersonWithReverseTool("John Doe")).map { ToolObject.from(it) })
        assertEquals(2, result.size)
        assertEquals("test", result[1].toolDefinition.name())
    }

}
