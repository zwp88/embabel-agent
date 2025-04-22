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

interface PersonEntity : Person, Entity<String>

data class LocalPerson(override val name: String, override val id: String? = null) : PersonEntity

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
    fun `can add interface`() {
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
    }

    @Test
    fun `not found by root entity`() {
        val repository = InMemoryMixinRepository()
        val loaded = repository.findById("abcd", PersonEntity::class.java)
        assertNull(loaded)
    }

}