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
package com.embabel.ux.form

import java.time.LocalDate
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

@Target(AnnotationTarget.PROPERTY)
annotation class FormField(val controlId: String)

/**
 * Indicates that this is not a form field
 */
@Target(AnnotationTarget.PROPERTY)
annotation class NoFormField

/**
 * Form binder system that maps form submission values to Kotlin data classes
 */
class FormBinder<T : Any>(private val targetClass: KClass<T>) {

    constructor (targetClass: Class<T>) : this(targetClass.kotlin)

    class FormBindingException(message: String) : Exception(message)

    class ValidationException(val errors: Map<String, String>) : Exception("Form validation failed")

    /**
     * Binds a FormSubmissionResult to the target data class
     * @throws FormBindingException if there's an error mapping values
     * @throws ValidationException if the form submission has validation errors
     */
    fun bind(submissionResult: FormSubmissionResult): T {
        // Check validation first
        if (!submissionResult.valid) {
            throw ValidationException(submissionResult.validationErrors)
        }

        // Get the primary constructor for the data class
        val constructor = targetClass.primaryConstructor
            ?: throw FormBindingException("Target class ${targetClass.simpleName} must be a data class with a primary constructor")

        // Map of parameter name to its corresponding type
        val parameterMap = constructor.parameters.associateBy { it.name }

        // Map to hold parameter values for the constructor
        val parameterValues = mutableMapOf<KParameter, Any?>()

        // Get properties with FormField annotation
        val formFields = getPropertiesInDeclarationOrder(targetClass)
            .filterNot { it.annotations.any { annotation -> annotation is NoFormField } }

        // Map each annotated property to its parameter and value from the form submission
        formFields.forEach { property ->
            val formFieldAnnotation = property.annotations
                .filterIsInstance<FormField>()
                .firstOrNull()

            val controlId = formFieldAnnotation?.controlId ?: property.name
            val controlValue = submissionResult.values[controlId]
                ?: throw FormBindingException("No value found for control id '$controlId' in form submission: $submissionResult")

            val parameter = parameterMap[property.name]
                ?: throw FormBindingException("No matching constructor parameter for property: ${property.name}")

            // Convert the ControlValue to the appropriate type for the parameter
            parameterValues[parameter] = convertToParameterType(controlValue, parameter)
        }

        // Check if all required parameters are provided
        constructor.parameters
            .filter { it.isOptional.not() && it.name !in parameterValues.keys.mapNotNull { param -> param.name } }
            .forEach {
                throw FormBindingException("Missing required parameter: ${it.name}")
            }

        // Create instance of the data class using the constructor
        return try {
            constructor.callBy(parameterValues)
        } catch (e: Exception) {
            throw FormBindingException("Failed to construct ${targetClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Converts a ControlValue to the appropriate type for the parameter
     */
    private fun convertToParameterType(controlValue: ControlValue, parameter: KParameter): Any? {
        return when (controlValue) {
            is ControlValue.TextValue -> convertTextValue(controlValue.value, parameter)
            is ControlValue.BooleanValue -> controlValue.value
            is ControlValue.NumberValue -> convertNumberValue(controlValue.value, parameter)
            is ControlValue.DateValue -> controlValue.value
            is ControlValue.TimeValue -> controlValue.value
            is ControlValue.OptionValue -> controlValue.value
            is ControlValue.MultiOptionValue -> controlValue.values
            is ControlValue.FileValue -> controlValue.fileIds
            is ControlValue.EmptyValue -> null
        }
    }

    /**
     * Converts text values to appropriate types based on parameter type
     */
    private fun convertTextValue(value: String, parameter: KParameter): Any? {
        return when (parameter.type.classifier) {
            String::class -> value
            Int::class -> value.toIntOrNull()
            Long::class -> value.toLongOrNull()
            Double::class -> value.toDoubleOrNull()
            Float::class -> value.toFloatOrNull()
            Boolean::class -> value.toBoolean()
            LocalDate::class -> LocalDate.parse(value)
            LocalTime::class -> LocalTime.parse(value)
            else -> value
        }
    }

    /**
     * Converts number values to appropriate numeric types
     */
    private fun convertNumberValue(value: Double, parameter: KParameter): Any? {
        return when (parameter.type.classifier) {
            Int::class -> value.toInt()
            Long::class -> value.toLong()
            Float::class -> value.toFloat()
            Double::class -> value
            String::class -> value.toString()
            else -> value
        }
    }
}

/**
 * Extension function to make binding more convenient
 */
inline fun <reified T : Any> FormSubmissionResult.bindTo(): T {
    return FormBinder(T::class).bind(this)
}

// Example usage with a domain class
data class UserRegistration(
    @FormField("name-field-id")
    val fullName: String,

    @FormField("email-field-id")
    val email: String,

    @FormField("age-field-id")
    val age: Int,

    @FormField("birth-date-id")
    val birthDate: LocalDate,

    @FormField("country-id")
    val country: String,

    @FormField("terms-accepted-id")
    val termsAccepted: Boolean,

    @FormField("bio-id")
    val biography: String? = null
)
