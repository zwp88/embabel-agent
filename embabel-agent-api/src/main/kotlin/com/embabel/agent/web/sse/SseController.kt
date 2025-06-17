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
package com.embabel.agent.web.sse

import com.embabel.agent.event.AgentProcessEvent
import com.embabel.agent.event.AgenticEventListener
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@RestController
class SSEController : AgenticEventListener {

    private val logger = LoggerFactory.getLogger(SSEController::class.java)

    init {
        logger.info("SSEController initialized, ready to stream AgentProcessEvents...")
    }

    // Map from processId to a list of SseEmitters
    private val processEmitters = ConcurrentHashMap<String, MutableList<SseEmitter>>()

    override fun onProcessEvent(event: AgentProcessEvent) {
        val processId = event.processId
        val emitters = processEmitters[processId]

        emitters?.removeIf { emitter ->
            try {
                logger.debug("Sending SSE event for process {}: {}", processId, event)
                emitter.send(
                    SseEmitter.event()
                        .name("agent-process-event")
                        .data(event)
                )
                false // Keep this emitter
            } catch (_: IOException) {
                logger.debug("Disconnecting emitter for process {}", processId)
                true // Remove broken emitter
            } catch (t: Throwable) {
                logger.warn("Error sending event to emitter for process $processId", t)
                true
            }
        }
    }

    @GetMapping(value = ["/events/process/{processId}"], produces = [TEXT_EVENT_STREAM_VALUE])
    fun streamEventsForId(@PathVariable processId: String): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)

        // Add emitter to the map for this process
        processEmitters.computeIfAbsent(processId) {
            Collections.synchronizedList(mutableListOf())
        }.add(emitter)

        // Clean up when connection closes
        emitter.onCompletion {
            removeEmitter(processId, emitter)
        }
        emitter.onTimeout {
            removeEmitter(processId, emitter)
        }
        emitter.onError {
            removeEmitter(processId, emitter)
        }

        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data(mapOf("message" to "Connected to stream for process ID: $processId"))
            )
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }

        return emitter
    }

    private fun removeEmitter(processId: String, emitter: SseEmitter) {
        processEmitters[processId]?.remove(emitter)
        if (processEmitters[processId]?.isEmpty() == true) {
            processEmitters.remove(processId)
        }
    }

}
