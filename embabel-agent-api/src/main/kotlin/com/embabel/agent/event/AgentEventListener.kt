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
package com.embabel.agent.event

/**
 * Listen to events related to processes and the platform itself
 */
interface AgenticEventListener {

    /**
     * An event relating to the platform or leading to the creation
     * of an AgentProcess, such as the choice of a goal.
     * No process is available at this point.
     */
    fun onPlatformEvent(event: AgentPlatformEvent) {}

    /**
     * Listen to an event during the execution of an AgentProcess
     */
    fun onProcessEvent(event: AgentProcessEvent) {}

    companion object {
        fun of(vararg listeners: AgenticEventListener): AgenticEventListener =
            from(listeners.toList())

        fun from(listeners: List<AgenticEventListener>): AgenticEventListener =
            MulticastAgenticEventListener(listeners)

        /**
         * EventListener that does nothing
         */
        val DevNull: AgenticEventListener = object : AgenticEventListener {
        }
    }

}

private class MulticastAgenticEventListener(
    private val eventListeners: List<AgenticEventListener>,
) : AgenticEventListener {

    override fun onPlatformEvent(event: AgentPlatformEvent) {
        eventListeners.forEach { it.onPlatformEvent(event) }
    }

    override fun onProcessEvent(event: AgentProcessEvent) {
        eventListeners.forEach { it.onProcessEvent(event) }
    }
}
