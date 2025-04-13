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
package com.embabel.agent.core.support

import com.embabel.agent.core.Blackboard

/**
 * Simple in memory blackboard implementation
 * backed by a map.
 */
class InMemoryBlackboard : Blackboard {
    private val _map: MutableMap<String, Any> = mutableMapOf()
    private val _entries: MutableList<Any> = mutableListOf()

    override fun spawn(): Blackboard {
        return InMemoryBlackboard().apply {
            _map.putAll(this@InMemoryBlackboard._map)
            _entries.addAll(this@InMemoryBlackboard._entries)
        }
    }

    override val entries: List<Any> get() = _entries

    override fun get(name: String): Any? = _map[name]

    override fun bind(key: String, value: Any): Blackboard {
        _map[key] = value
        _entries.add(value)
        return this
    }

    override fun setCondition(key: String, value: Boolean): Blackboard {
        _map[key] = value
        _entries.add(value)
        return this
    }

    override fun getCondition(key: String): Boolean? =
        _map[key] as? Boolean

    override fun addEntry(value: Any): Blackboard {
        _entries.add(value)
        return this
    }

    override fun expressionEvaluationModel(): Map<String, Any> {
        return _map.mapValues { entry ->
            when (val v = entry.value) {
                // TODO used to have entity data branch

                else -> entry.value
            }
        }
    }

    override fun infoString(verbose: Boolean?): String {
        val entriesString = if (verbose == true) entries else entries.map { "${it::class.simpleName}" }
        val mapString =
            if (verbose == true) _map else _map.entries.joinToString(", ") { "${it.key}=${it.value::class.simpleName}" }
        return "${javaClass.simpleName}(map=$mapString, entries=$entriesString)"
    }
}
