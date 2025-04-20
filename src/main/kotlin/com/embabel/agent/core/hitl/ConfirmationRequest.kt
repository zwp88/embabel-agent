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
package com.embabel.agent.core.hitl

import com.embabel.agent.core.ProcessContext
import com.embabel.common.util.kotlin.loggerFor
import java.time.Instant
import java.util.*

class ConfirmationRequest<P : Any>(
    payload: P,
    val message: String,
    persistent: Boolean = false,
) : AbstractAwaitable<P, ConfirmationResponse>(
    payload = payload,
    persistent = persistent,
) {

    override fun onResponse(
        response: ConfirmationResponse,
        processContext: ProcessContext,
    ): ResponseResponse {

        return if (response.accepted) {
            loggerFor<ConfirmationRequest<*>>().info("Accepted confirmation request: {}", payload)
            TODO()
            ResponseResponse.ACCEPTED
        } else {
            loggerFor<ConfirmationRequest<*>>().info("Rejected confirmation request: {}", payload)
            ResponseResponse.REJECTED
        }
    }

    override fun infoString(verbose: Boolean?): String {
        return "ConfirmationRequest(id=$id, payload=$payload, message='$message')"
    }
}

data class ConfirmationResponse(
    override val id: String = UUID.randomUUID().toString(),
    override val awaitedId: String,
    val accepted: Boolean,
    private val persistent: Boolean = false,
    override val timestamp: Instant = Instant.now(),
) : AwaitableResponse {

    override fun persistent() = persistent
}
