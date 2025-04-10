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
package com.embabel.agent.support

import com.embabel.agent.Agent
import com.embabel.agent.ProcessOptions
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.NameGenerator
import org.springframework.stereotype.Service

interface ProcessIdGenerator {
    fun createProcessId(agent: Agent, processOptions: ProcessOptions): String

}

@Service
class DefaultProcessIdGenerator(
    private val nameGenerator: NameGenerator = MobyNameGenerator,
    private val includeVersion: Boolean = false,
) : ProcessIdGenerator {

    override fun createProcessId(agent: Agent, processOptions: ProcessOptions): String {
        val version = if (includeVersion) {
            "-${agent.version}"
        } else {
            ""
        }
        val randomPart = nameGenerator.generateName()
        return "${agent.name}$version-$randomPart"
    }
}
