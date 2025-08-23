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
package com.embabel.coding.tools.jvm

import com.embabel.agent.api.common.PromptRunner
import com.embabel.coding.tools.ApiReferenceTools
import org.junit.jupiter.api.Test

class ClassGraphApiReferenceExtractorTest {

    @Test
    fun `extract Embabel agent framework`() {
        val cigar = ClassGraphApiReferenceExtractor()
        val apiref = cigar.excludeFromProjectClasspath(
            acceptedPackages = setOf("com.embabel.agent"),
        )
        val tools = ApiReferenceTools(apiref)
        println("Extracted API reference: ${tools.formatAsText()}")
        println(tools.findClassSignature(PromptRunner::class.java.name))
        println(tools.findPackageSignature("com.embabel.agent.api.common"))
    }

}
