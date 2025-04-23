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
package com.embabel.agent.experimental.hitl

import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.hitl.AbstractAwaitable
import com.embabel.agent.core.hitl.AwaitableResponse
import com.embabel.agent.core.hitl.ResponseImpact
import com.embabel.agent.experimental.form.DefaultFormProcessor
import com.embabel.agent.experimental.form.Form
import com.embabel.agent.experimental.form.FormSubmission
import java.time.Instant
import java.util.*

/**
 * Present the user with a form
 * and bind it to the given class
 */
class FormBindingRequest<O>(
    form: Form,
    val outputClass: Class<O>,
    persistent: Boolean = false,
) : AbstractAwaitable<Form, FormResponse>(
    payload = form,
    persistent = persistent,
) {

    override fun onResponse(
        response: FormResponse,
        processContext: ProcessContext,
    ): ResponseImpact {
        val formSubmissionResult = DefaultFormProcessor().processSubmission(payload, response.formSubmission)
        if (!formSubmissionResult.valid) {
            throw IllegalStateException("Form submission is not valid: ${formSubmissionResult.validationErrors}")
        }
        val theO: Any = TODO()
        processContext.blackboard += theO
        return ResponseImpact.UPDATED
    }

    override fun infoString(verbose: Boolean?): String {
        return "InformationRequest(id=$id, payload=$payload, form='$payload')"
    }

    override fun toString(): String = infoString(verbose = false)
}

data class FormResponse(
    override val id: String = UUID.randomUUID().toString(),
    override val awaitableId: String,
    val formSubmission: FormSubmission,
    private val persistent: Boolean = false,
    override val timestamp: Instant = Instant.now(),
) : AwaitableResponse {

    override fun persistent() = persistent
}
