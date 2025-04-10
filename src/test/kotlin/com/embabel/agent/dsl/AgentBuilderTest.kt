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
package com.embabel.agent.dsl

import com.embabel.agent.annotation.support.Person
import com.embabel.agent.support.Dog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

val emptyAgent = agent(name = "foo", description = "bar") {}

val oneAction = agent(
    name = "oneAction",
    description = "one action agent",
) {
    action {
        transformer<Person, Person>(name = "reverser") { payload ->
            payload.input.copy(name = "foo")
        }
    }
}

val oneActionAndOneGoal = agent(
    name = "oneActionAndOneGoal",
    description = "one action agent",
) {
    action {
        transformer<Person, Dog>(name = "reverser") { payload ->
            Dog(name = payload.input.name)
        }
    }

    goal(
        name = "turnedIntoDog",
        description = "the person has been turned into a dog",
        satisfiedBy = Dog::class
    )
}

class AgentBuilderTest {

    @Nested
    inner class Errors {

        @Test
        fun `empty agent`() {
            val agent = emptyAgent
            assert(agent.name == "foo")
            assert(agent.description == "bar")
            assert(agent.version == "0.1.0-SNAPSHOT")
            assert(agent.toolGroups.isEmpty())
            assert(agent.actions.isEmpty())
            assert(agent.goals.isEmpty())
        }
    }

    @Nested
    inner class HappyPath {

        @Test
        fun `one action`() {
            val agent = oneAction
            assert(agent.name == "oneAction")
            assert(agent.description == "one action agent")
            assert(agent.version == "0.1.0-SNAPSHOT")
            assert(agent.toolGroups.isEmpty())
            assertEquals(1, agent.actions.size)
            assert(agent.goals.isEmpty())
        }

        @Test
        fun `one action and one goal`() {
            val agent = oneActionAndOneGoal
            assert(agent.name == "oneActionAndOneGoal")
            assert(agent.toolGroups.isEmpty())
            assertEquals(1, agent.actions.size)
            assertEquals(1, agent.goals.size)
            assertEquals("turnedIntoDog", agent.goals.first().name)
        }
    }

}
