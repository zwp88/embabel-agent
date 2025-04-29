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
package com.embabel.agent.e2e.coding

import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.api.annotation.support.deployAnnotatedInstances
import com.embabel.agent.api.common.transform
import com.embabel.agent.api.common.typedOps
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.examples.dogfood.coding.CodeExplanation
import com.embabel.examples.dogfood.coding.Coder
import com.embabel.examples.dogfood.coding.CodingProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
//@Import(
//    value = [
//        FakeConfig::class,
//        FakeAiConfiguration::class,
//    ]
//)
class CodeWriterTest(
    @Autowired private val agentPlatform: AgentPlatform,
    @Autowired private val agentMetadataReader: AgentMetadataReader,
) {

    @BeforeEach
    fun setup() {
        val codingProperties = CodingProperties()
        val coder = Coder(codingProperties)
        agentPlatform.deployAnnotatedInstances(
            agentMetadataReader,
            coder,
            TestProjectCreator(),
        )
    }

    @Test
    @Disabled("not yet working")
    fun `create and build`() {
        val codeExplanation = agentPlatform.typedOps().transform<ProjectRecipe, CodeExplanation>(
            input = ProjectRecipe("foo"),
            processOptions = ProcessOptions(
                verbosity = Verbosity(showPlanning = true),
            ),
        )
        assertNotNull(codeExplanation)
    }

}
