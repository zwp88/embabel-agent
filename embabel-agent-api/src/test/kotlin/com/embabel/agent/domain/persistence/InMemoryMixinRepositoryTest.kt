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
package com.embabel.agent.domain.persistence

import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.persistence.support.InMemoryMixinRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


interface Address {
    val street: String
    val city: String

    companion object {
        operator fun invoke(street: String, city: String): Address = AddressImpl(street, city)
    }
}

private data class AddressImpl(override val street: String, override val city: String) : Address

interface PersonEntity : Person, MixinEnabledEntity<String>

data class LocalPerson(override val name: String, override val id: String? = null) : PersonEntity

interface PersonWithAddress : PersonEntity, Address

class InMemoryMixinRepositoryTest {

    @Test
    fun `save creates id`() {
        val repository = InMemoryMixinRepository()
        val person = LocalPerson("John Doe")
        assertNull(person.id)
        val savedPerson = repository.save(person)
        assertNotNull(savedPerson.id)
    }

    @Test
    fun `can add interface and load`() {
        val repository = InMemoryMixinRepository()
        val person = LocalPerson("John Doe")
        assertNull(person.id)
        val savedPerson = repository.save(person)
        assertNotNull(savedPerson.id)
        val mixin = repository.become(savedPerson, Address("123 Main St", "Springfield"), Address::class.java)
        repository.save(mixin)

        val loadedMixin = repository.findById(savedPerson.id!!, PersonEntity::class.java)
        assertNotNull(loadedMixin)
        loadedMixin as Address
        assertEquals("123 Main St", loadedMixin.street)
        loadedMixin as Person
        assertEquals("John Doe", loadedMixin.name)
    }

    @Test
    fun `not found by root entity`() {
        val repository = InMemoryMixinRepository()
        val loaded = repository.findById("abcd", PersonEntity::class.java)
        assertNull(loaded)
    }

    @Test
    fun `find by subinterface defined at point of use`() {
        val repository = InMemoryMixinRepository()
        val person = LocalPerson("John Doe")
        assertNull(person.id)
        val savedPerson = repository.save(person)
        assertNotNull(savedPerson.id)
        val mixin = repository.become(savedPerson, Address("123 Main St", "Springfield"), Address::class.java)
        repository.save(mixin)

        val loadedMixin = repository.findById<PersonEntity, PersonWithAddress, String>(savedPerson.id!!)
        assertNotNull(loadedMixin)
        assertEquals("123 Main St", loadedMixin.street)
        assertEquals("John Doe", loadedMixin.name)
    }

}
