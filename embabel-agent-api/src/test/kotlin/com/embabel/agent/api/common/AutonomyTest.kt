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
package com.embabel.agent.api.common

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Please refer to comprehensive tests [AutonomyGoalSelectionTest] and [AutonomyAgentSelectionTest]
 */
class AutonomyTest {

    @Nested
    inner class ChooseAndAccomplishGoalTests {

        @Test
        @Disabled
        fun `test choose and accomplish goal with valid input`() {
            /**
             * please refer to [AutonomyGoalSelectionTest.testChooseAndAccomplishGoalWithValidInput]
             */
        }

        @Test
        @Disabled
        fun `test choose and accomplish goal with invalid input`() {
            /**
             * please refer to [AutonomyGoalSelectionTest.testChooseAndAccomplishGoalWithInvalidInput]
             */
        }
    }

    @Nested
    inner class ChooseAndRunAgentTests {

        @Test
        @Disabled
        fun `test choose and run agent with valid input`() {
            /**
             * please refer to [AutonomyAgentSelectionTest.testChooseAndRunAgentWithValidInput]
             */
        }

        @Test
        @Disabled
        fun `test choose and run agent with invalid input`() {
            /**
             *  please refer to [AutonomyAgentSelectionTest.testChooseAndRunAgentWithInvalidInput]
             */
        }
    }

    @Nested
    inner class CreateGoalSeekerTests {

        @Test
        @Disabled
        fun `test create goal seeker excludes irrelevant goals`() {
            // TODO: Implement test
        }

    }
}
