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
package com.embabel.agent.domain.mixin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

interface Animal {
    val name: String
}

interface Dog : Animal {
    val breed: String

    companion object {

        operator fun invoke(name: String, breed: String): Dog = DogImpl(name, breed)
    }
}

private data class DogImpl(override val name: String, override val breed: String) : Dog

interface Cat : Animal {
    val color: String

    companion object {

        operator fun invoke(name: String, color: String): Cat = CatImpl(name, color)
    }
}

private data class CatImpl(override val name: String, override val color: String) : Cat

interface Insured {
    val policyNumber: String

    companion object {

        operator fun invoke(policyNumber: String): Insured = InsuredImpl(policyNumber)
    }
}

private data class InsuredImpl(override val policyNumber: String) : Insured

interface InsuredDog : Dog, Insured {
}


class MixinManagerTest {

    @Test
    fun `leaves root type alone`() {
        val dog = Dog("Rex", "Labrador")
        val mixinManager = MixinManager(dog)
        mixinManager.instance() is Dog
        assertEquals(dog.name, mixinManager.instance().name)
    }

    @Test
    fun `adds insured with reification and interface detection`() {
        val dog = Dog("Rex", "Labrador")
        val insured = Insured("123456")
        val mixinManager = MixinManager(dog)
        mixinManager.instance() is Dog
        assertFalse(mixinManager.instance() is Insured)
        mixinManager.becomeFirstInterface(insured)
        val impl = mixinManager.instance()
        impl as Insured
        assertEquals(dog.name, impl.name)
        assertEquals(insured.policyNumber, impl.policyNumber)
        assertEquals(setOf(Dog::class.java, Insured::class.java), mixinManager.allInterfaces().toSet())
    }

    @Test
    fun `adds insured with explicit interface`() {
        val dog = Dog("Rex", "Labrador")
        val insured = Insured("123456")
        val mixinManager = MixinManager(dog)
        mixinManager.instance() is Dog
        assertFalse(mixinManager.instance() is Insured)
        mixinManager.become(insured, Insured::class.java)
        val impl = mixinManager.instance()
        impl as Insured
        assertEquals(dog.name, impl.name)
        assertEquals(insured.policyNumber, impl.policyNumber)
        assertEquals(setOf(Dog::class.java, Insured::class.java), mixinManager.allInterfaces().toSet())
    }

    @Test
    fun `refuses to add no interface`() {
        val dog = Dog("Rex", "Labrador")
        val nif = NoInterfaceInSight()
        val mixinManager = MixinManager(dog)
        assertThrows<IllegalArgumentException> {
            mixinManager.become(nif, NoInterfaceInSight::class.java)
        }
        assertThrows<IllegalArgumentException> {
            mixinManager.becomeFirstInterface(nif)
        }
    }

}

data class NoInterfaceInSight(val bad: Boolean = true)
