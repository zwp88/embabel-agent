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
package com.embabel.agent.support

import com.embabel.agent.UserInput
import com.embabel.agent.annotation.support.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.Point
import kotlin.test.assertNotNull

class BlackboardTest {

    @Nested
    inner class Aggregation {
        @Test
        fun `empty blackboard`() {
            val bb = InMemoryBlackboard()
            assertNull(
                bb.getValue(
                    "it",
                    "AllOfTheAbove",
                    listOf(AllOfTheAbove::class.java, UserInput::class.java, Person::class.java),
                )
            )
        }

        @Test
        fun `not satisfied`() {
            val bb = InMemoryBlackboard()
            bb += UserInput("John is a man")
            assertNull(
                bb.getValue(
                    "it",
                    "AllOfTheAbove",
                    listOf(AllOfTheAbove::class.java, UserInput::class.java, Person::class.java),
                )
            )
        }

        @Test
        fun satisfied() {
            val bb = InMemoryBlackboard()
            bb += UserInput("John is a man")
            bb += Person("John")
            val aota = bb.getValue(
                "it",
                "AllOfTheAbove",
                listOf(AllOfTheAbove::class.java, UserInput::class.java, Person::class.java),
            )
            assertNotNull(aota)
            aota as AllOfTheAbove
            assertEquals("John", aota.person.name)
            assertEquals("John is a man", aota.userInput.content)

        }

    }

    @Nested
    inner class TypeResolution {

        @Test
        fun `empty blackboard, no domain objects`() {
            val bb = InMemoryBlackboard()
            assertNull(bb.getValue("it", "Person", emptyList()))
        }

        @Test
        fun `empty blackboard, relevant domain object`() {
            val bb = InMemoryBlackboard()
            assertNull(bb.getValue("it", "Person", listOf(Person::class.java)))
        }

        @Test
        fun `exact type match on it`() {
            val bb = InMemoryBlackboard()
            val john = Person("John")
            bb += john
            assertEquals(john, bb.getValue("it", "Person", listOf(Person::class.java)))
        }

        @Test
        fun `exact type match on variable name`() {
            val bb = InMemoryBlackboard()
            val duke = Dog("Duke")
            bb += duke
            assertEquals(duke, bb.getValue("it", "Dog", listOf(Dog::class.java)))
        }


        @Test
        fun `interface type match on variable name`() {
            val bb = InMemoryBlackboard()
            val duke = Dog("Duke")
            bb += duke
            assertEquals(duke, bb.getValue("it", "Organism", listOf(Dog::class.java)))
        }

        @Test
        fun `superclass type match on variable name`() {
            val bb = InMemoryBlackboard()
            val duke = Dog("Duke")
            bb += duke
            assertEquals(duke, bb.getValue("it", "Animal", listOf(Dog::class.java)))
        }

        @Test
        fun `no type match`() {
            val bb = InMemoryBlackboard()
            val john = Person("John")
            bb += ("it" to john)
            assertNull(bb.getValue("person", "Point", listOf(Person::class.java, Point::class.java)))
        }


    }
}

interface Organism {
}

open class Animal(
    val name: String,
) : Organism

class Dog(name: String) : Animal(name)
