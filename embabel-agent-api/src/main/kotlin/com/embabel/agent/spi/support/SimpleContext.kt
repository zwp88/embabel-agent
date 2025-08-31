package com.embabel.agent.spi.support

import com.embabel.agent.core.Blackboard
import com.embabel.agent.spi.Context
import com.embabel.common.util.indent
import com.embabel.common.util.indentLines
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SimpleContext(
    override var id: String? = null,
) : Context {

    private val _map: MutableMap<String, Any> = ConcurrentHashMap()
    private val _entries: MutableList<Any> = Collections.synchronizedList(mutableListOf())

    override fun withId(id: String): Context {
        this.id = id
        return this
    }

    override fun bind(
        key: String,
        value: Any,
    ) {
        _map[key] = value
        _entries.add(value)
    }

    override fun addObject(value: Any) {
        _entries.add(value)
    }

    override fun infoString(
        verbose: Boolean?,
        indent: Int,
    ): String {
        // Create snapshots for thread-safe iteration
        val mapSnapshot = _map.toMap()
        val objectsSnapshot = synchronized(_entries) { _entries.toList() }

        return """
            |${javaClass.simpleName}: id=$id
            |map:
            |${mapSnapshot.entries.joinToString(", ").indent(1)}
            |entries:
            |${objectsSnapshot.joinToString(", ").indent(1)}
            |"""
            .trimMargin()
            .indentLines(indent)
    }

    override fun populate(blackboard: Blackboard) {
        TODO("Not yet implemented")
    }
}