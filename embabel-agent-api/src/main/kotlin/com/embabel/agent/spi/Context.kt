package com.embabel.agent.spi

import com.embabel.agent.core.Blackboard
import com.embabel.common.core.types.HasInfoString

/**
 * Longer-lived interface than a blackboard.
 */
interface Context : HasInfoString {

    /**
     * May be null for a new context not yet saved.
     */
    val id: String?

    fun withId(id: String): Context

    fun bind(
        key: String,
        value: Any,
    )

    fun addObject(value: Any)

    /**
     * Populate the given blackboard from the context.
     */
    fun populate(blackboard: Blackboard)

}

/**
 * Load a context
 */
interface ContextRepository {

    fun save(context: Context): Context

    fun findById(id: String): Context?

    fun delete(context: Context)

}