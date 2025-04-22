package com.embabel.agent.experimental.hitl

import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.hitl.AbstractAwaitable
import com.embabel.agent.core.hitl.AwaitableResponse
import com.embabel.agent.core.hitl.ResponseImpact
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