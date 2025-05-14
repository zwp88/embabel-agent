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
package com.embabel.agent.core

import com.embabel.common.core.types.ZeroToOne
import com.embabel.plan.goap.ConditionDetermination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ConditionTest {

    private val mockProcessContext = mock<ProcessContext>()

    // Helper function to create test conditions
    private fun createTestCondition(
        name: String,
        cost: ZeroToOne,
        result: ConditionDetermination
    ): Condition {
        return object : Condition {
            override val name = name
            override val cost = cost
            override fun evaluate(processContext: ProcessContext) = result
        }
    }

    @Test
    fun `test not operator`() {
        val trueCondition = createTestCondition("True", 0.1, ConditionDetermination.TRUE)
        val falseCondition = createTestCondition("False", 0.2, ConditionDetermination.FALSE)
        val unknownCondition = createTestCondition("Unknown", 0.3, ConditionDetermination.UNKNOWN)

        // Test negation
        assertEquals(ConditionDetermination.FALSE, (!trueCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.TRUE, (!falseCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.UNKNOWN, (!unknownCondition).evaluate(mockProcessContext))

        // Verify name and cost
        assertEquals("!True", (!trueCondition).name)
        assertEquals(0.1, (!trueCondition).cost)
    }


    @Test
    fun `test or operator`() {
        val trueCondition = createTestCondition("True", 0.1, ConditionDetermination.TRUE)
        val falseCondition = createTestCondition("False", 0.2, ConditionDetermination.FALSE)
        val unknownCondition = createTestCondition("Unknown", 0.3, ConditionDetermination.UNKNOWN)

        // Test OR logic - TRUE cases
        assertEquals(ConditionDetermination.TRUE, (trueCondition or trueCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.TRUE, (trueCondition or falseCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.TRUE, (trueCondition or unknownCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.TRUE, (falseCondition or trueCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.TRUE, (unknownCondition or trueCondition).evaluate(mockProcessContext))

        // Test OR logic - UNKNOWN cases
        assertEquals(ConditionDetermination.UNKNOWN, (falseCondition or unknownCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.UNKNOWN, (unknownCondition or falseCondition).evaluate(mockProcessContext))
        assertEquals(
            ConditionDetermination.UNKNOWN,
            (unknownCondition or unknownCondition).evaluate(mockProcessContext)
        )

        // Test OR logic - FALSE case
        assertEquals(ConditionDetermination.FALSE, (falseCondition or falseCondition).evaluate(mockProcessContext))

        // Verify cost is minimum of the two conditions
        assertEquals(0.1, (trueCondition or falseCondition).cost)
        assertEquals(0.2, (falseCondition or unknownCondition).cost)

        // Verify name format
        assertEquals("(True OR False)", (trueCondition or falseCondition).name)
    }

    @Test
    fun `test and operator`() {
        val trueCondition = createTestCondition("True", 0.1, ConditionDetermination.TRUE)
        val falseCondition = createTestCondition("False", 0.2, ConditionDetermination.FALSE)
        val unknownCondition = createTestCondition("Unknown", 0.3, ConditionDetermination.UNKNOWN)

        // Test AND logic - FALSE cases
        assertEquals(ConditionDetermination.FALSE, (falseCondition and falseCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.FALSE, (falseCondition and trueCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.FALSE, (falseCondition and unknownCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.FALSE, (trueCondition and falseCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.FALSE, (unknownCondition and falseCondition).evaluate(mockProcessContext))

        // Test AND logic - UNKNOWN cases
        assertEquals(ConditionDetermination.UNKNOWN, (trueCondition and unknownCondition).evaluate(mockProcessContext))
        assertEquals(ConditionDetermination.UNKNOWN, (unknownCondition and trueCondition).evaluate(mockProcessContext))
        assertEquals(
            ConditionDetermination.UNKNOWN,
            (unknownCondition and unknownCondition).evaluate(mockProcessContext)
        )

        // Test AND logic - TRUE case
        assertEquals(ConditionDetermination.TRUE, (trueCondition and trueCondition).evaluate(mockProcessContext))

        // Verify cost is minimum of the two conditions
        assertEquals(0.1, (trueCondition and falseCondition).cost)
        assertEquals(0.2, (falseCondition and unknownCondition).cost)

        // Verify name format
        assertEquals("(True AND False)", (trueCondition and falseCondition).name)
    }

    @Test
    fun `test complex condition combinations`() {
        val trueCondition = createTestCondition("True", 0.1, ConditionDetermination.TRUE)
        val falseCondition = createTestCondition("False", 0.2, ConditionDetermination.FALSE)
        val unknownCondition = createTestCondition("Unknown", 0.3, ConditionDetermination.UNKNOWN)

        // Test complex combinations
        // (True OR False) AND Unknown = Unknown
        val complex1 = (trueCondition or falseCondition) and unknownCondition
        assertEquals(ConditionDetermination.UNKNOWN, complex1.evaluate(mockProcessContext))

        // (False AND Unknown) OR True = True
        val complex2 = (falseCondition and unknownCondition) or trueCondition
        assertEquals(ConditionDetermination.TRUE, complex2.evaluate(mockProcessContext))

        // !(True OR Unknown) = False
        val complex3 = !(trueCondition or unknownCondition)
        assertEquals(ConditionDetermination.FALSE, complex3.evaluate(mockProcessContext))

        // Complex nested example
        // !((True AND Unknown) OR (False AND True))
        val complex5 = !((trueCondition and unknownCondition) or (falseCondition and trueCondition))
        assertEquals(ConditionDetermination.UNKNOWN, complex5.evaluate(mockProcessContext))
    }

    @Test
    fun `test ComputedBooleanCondition`() {
        // Test condition that evaluates to true
        val trueComputedCondition = ComputedBooleanCondition(
            name = "IsPositive",
            cost = 0.5,
            evaluator = { it, condition -> true }
        )
        assertEquals(ConditionDetermination.TRUE, trueComputedCondition.evaluate(mockProcessContext))

        // Test condition that evaluates to false
        val falseComputedCondition = ComputedBooleanCondition(
            name = "IsNegative",
            cost = 0.3,
            evaluator = { it, condition -> false }
        )
        assertEquals(ConditionDetermination.FALSE, falseComputedCondition.evaluate(mockProcessContext))

        // Test combining computed conditions
        val combinedCondition = trueComputedCondition and falseComputedCondition
        assertEquals(ConditionDetermination.FALSE, combinedCondition.evaluate(mockProcessContext))

        // Test toString()
        assertEquals("ComputedBooleanCondition(name='IsPositive', cost=0.5)", trueComputedCondition.toString())
    }
}
