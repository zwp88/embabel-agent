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
package com.embabel.agent.testing

import com.embabel.agent.api.common.CreateObjectPromptException
import com.embabel.agent.api.common.LlmCallRequest


object UnitTestUtils {

    /**
     * Test an @Agent method that returns a prompt.
     * Allows making assertions about the generated prompt string and LLM options.
     * @param block The block to execute.
     * Should be a call to the agent method with the appropriate arguments.
     */
    @JvmStatic
    fun captureLlmCall(block: () -> Unit): LlmCallRequest {
        try {
            block()
            error("Expected an LLM call but none was made")
        } catch (epe: CreateObjectPromptException) {
            return epe
        }
    }
}
