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
package com.embabel.agent.experimental.form

import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * Simple form generator that uses FormField annotations
 */
object SimpleFormGenerator : FormGenerator {

    /**
     * Generate a form from any class with FormField annotations
     */
    override fun <T : Any> generateForm(dataClass: KClass<T>, formTitle: String): Form {
        val controls = mutableListOf<Control>()

        // Get all properties from the class
        val properties = getPropertiesInDeclarationOrder(dataClass)

        // Process each property
        properties.forEach { property ->
            // Try to create a control for this property
            createControlForProperty(property)?.let { control ->
                controls.add(control)
            }
        }

        // Add submit button
        controls.add(
            Button(
                id = UUID.randomUUID().toString(),
                label = "Submit"
            )
        )

        return Form(
            title = formTitle,
            controls = controls
        )
    }

    /**
     * Create an appropriate control for a property
     */
    private fun createControlForProperty(property: KProperty<*>): Control? {
        // Check if property has a FormField annotation
        val formField = property.findAnnotation<FormField>()

        // Get control ID from annotation or generate a new one
        val controlId = formField?.controlId ?: return null // Skip properties without annotation

        val textField = property.findAnnotation<Text>()
        // Generate a user-friendly label from the property name
        val label = textField?.label ?: property.name
            .replace(Regex("([A-Z])"), " $1") // Add spaces before capitals
            .trim()
            .replaceFirstChar { it.uppercase() } // Capitalize first letter

        // Create appropriate control based on property type
        return when (property.returnType.jvmErasure) {
            String::class -> TextField(
                id = controlId,
                label = label,
                placeholder = "Enter ${label.lowercase()}",
                required = !property.returnType.isMarkedNullable
            )

            Int::class -> TextField(
                id = controlId,
                label = label,
                placeholder = "Enter a number",
                validationPattern = "^\\d+$",
                validationMessage = "Please enter a valid integer",
                required = !property.returnType.isMarkedNullable
            )

            Long::class -> TextField(
                id = controlId,
                label = label,
                placeholder = "Enter a number",
                validationPattern = "^\\d+$",
                validationMessage = "Please enter a valid number",
                required = !property.returnType.isMarkedNullable
            )

            Double::class, Float::class -> TextField(
                id = controlId,
                label = label,
                placeholder = "Enter a decimal number",
                validationPattern = "^\\d+(\\.\\d+)?$",
                validationMessage = "Please enter a valid decimal number",
                required = !property.returnType.isMarkedNullable
            )

            Boolean::class -> Checkbox(
                id = controlId,
                label = label,
                checked = false,
                required = !property.returnType.isMarkedNullable
            )

            LocalDate::class -> DatePicker(
                id = controlId,
                label = label,
                required = !property.returnType.isMarkedNullable
            )

            LocalTime::class -> TimePicker(
                id = controlId,
                label = label,
                required = !property.returnType.isMarkedNullable
            )

            List::class -> {
                // For simplicity, we'll use a text area for lists
                TextArea(
                    id = controlId,
                    label = label,
                    placeholder = "Enter comma-separated values",
                    required = !property.returnType.isMarkedNullable
                )
            }

            else -> {
                // Default to text field for unknown types
                TextField(
                    id = controlId,
                    label = label,
                    placeholder = "Enter value",
                    required = !property.returnType.isMarkedNullable
                )
            }
        }
    }
}

// TODO move to reflection utils
fun <T : Any> getPropertiesInDeclarationOrder(kClass: KClass<T>): List<KProperty<*>> {
    // Get Java fields in declaration order
    val javaFields = kClass.java.declaredFields

    val kotlinProperties = kClass.memberProperties.associateBy { it.name }

    // Return Kotlin properties in the same order as Java fields
    return javaFields
        .map { field -> kotlinProperties[field.name] }
        .filterNotNull()
}
