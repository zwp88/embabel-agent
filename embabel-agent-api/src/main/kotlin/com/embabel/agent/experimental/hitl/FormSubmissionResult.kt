package com.embabel.agent.experimental.hitl

import com.embabel.common.core.types.Timestamped
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

/**
 * Raw data from the user
 */
data class FormSubmission(
    val formId: String,
    val values: Map<String, Any>,
    val submissionId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
) : Timestamped


data class FormSubmissionResult(
    val submission: FormSubmission,
    val values: Map<String, ControlValue>,
    val valid: Boolean = true,
    val validationErrors: Map<String, String> = emptyMap(),
)

sealed class ControlValue {
    data class TextValue(val value: String) : ControlValue()
    data class BooleanValue(val value: Boolean) : ControlValue()
    data class NumberValue(val value: Double) : ControlValue()
    data class DateValue(val value: LocalDate) : ControlValue()
    data class TimeValue(val value: LocalTime) : ControlValue()
    data class OptionValue(val value: String) : ControlValue()
    data class MultiOptionValue(val values: List<String>) : ControlValue()
    data class FileValue(val fileIds: List<String>, val fileNames: List<String>) : ControlValue()
    object EmptyValue : ControlValue()
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

interface FormProcessor {
    fun processSubmission(form: Form, submission: FormSubmission): FormSubmissionResult
}

