package com.embabel.agent.e2e

import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.core.AgentPlatform
import com.embabel.examples.dogfood.coding.CodeWriter
import com.embabel.examples.dogfood.coding.CodingProperties
import org.junit.jupiter.api.BeforeEach
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
        val amd = agentMetadataReader.createAgentMetadata(
            CodeWriter(CodingProperties())
        )
        assertNotNull(amd)
        agentPlatform.deploy(amd)
    }

    @Test
    fun noGoao() {
    }

}