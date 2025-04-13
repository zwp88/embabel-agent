package com.embabel.agent

import java.util.*

/**
 * Id of a context.
 */
@JvmInline
value class ContextId(val value: String) {
    init {
        require(value.isNotBlank()) { "ContextId must not be blank" }
    }

    operator fun invoke(id: String) = ContextId(id)

    companion object {
        fun create(): ContextId = ContextId(UUID.randomUUID().toString())
    }
}
