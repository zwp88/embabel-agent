/*
                                * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.agent.e2e

import com.embabel.agent.AgentPlatform
import com.embabel.agent.GoalResult
import com.embabel.agent.ProcessOptions
import com.embabel.agent.UserInput
import com.embabel.examples.dogfood.FunnyWriteup
import com.embabel.examples.dogfood.HoroscopeService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import kotlin.test.assertNotNull

@TestConfiguration
class FakeHoroscopeConfig {
    @Bean
    @Primary
    fun testHoroscopeService() = HoroscopeService {
        """"
            |On Monday, try to avoid being eaten by wolves
            |On Tuesday, you will be chased by a bear
            |On Wednesday, you will be bitten by a snake
            |On Thursday, you will be chased by a lion
            |On Friday, you will be chased by a tiger
            |On Saturday, you will be chased by a crocodile
            |On Sunday, relax if you're still alive.
        """.trimMargin()
    }
}

/**
 * Integration tests
 */
@SpringBootTest
@Import(FakeHoroscopeConfig::class)
class AgentPlatformIntegrationTest(
    @Autowired
    private val agentPlatform: AgentPlatform,
) {

    @Test
    fun `agent starts up`() {
        // Nothing to test
    }

    @Test
    fun `run star finder as transform`() {
        val funnyWriteup = agentPlatform.asFunction<UserInput, FunnyWriteup>(
            processOptions = ProcessOptions(test = true),
            outputClass = FunnyWriteup::class.java,
        ).apply(
            UserInput("Lynda is a Scorpio, find some news for her"),
        )
        assertNotNull(funnyWriteup)
        assertNotNull(funnyWriteup.text)
    }

    @Test
    @Disabled("not yet working")
    fun `run star finder agent`() {
        val goalResult = agentPlatform.chooseAndAccomplishGoal(
            "Lynda is a Scorpio, find some news for her",
            ProcessOptions(test = true),
        )
        when (goalResult) {
            is GoalResult.Success -> {
                assertNotNull(goalResult.output)
                assertTrue(
                    goalResult.output is FunnyWriteup,
                    "Expected FunnyWriteup, got ${goalResult.output?.javaClass?.name}"
                )
            }

            is GoalResult.NoGoalFound -> {
                fail("Goal not found: $goalResult")
            }
        }
    }
}
