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
package com.embabel.agent

/**
 * Implemented by types that reference data types
 */
interface DataDictionary {

    /**
     * Schema types referenced by this component.
     * These are not backed by JVM objects.
     */
    val schemaTypes: Collection<SchemaType>

    /**
     * Referenced domain types.
     * These are normally data classes in Kotlin.
     * They may have methods annotated with the Spring @Tool annotation.
     */
    val domainTypes: Collection<Class<*>>
}
