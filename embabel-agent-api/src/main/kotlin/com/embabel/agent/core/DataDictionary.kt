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
package com.embabel.agent.core

/**
 * Implemented by types that reference data types
 */
interface DataDictionary {

    /**
     * Schema types referenced by this component.
     * These may or may not be backed by JVM objects.
     */
    val embabelTypes: Collection<EmbabelType>

    val schemaTypes: Collection<SchemaType>
        get() =
            embabelTypes.filterIsInstance<SchemaType>().toSet()

    val domainTypes: Collection<DomainType>
        get() =
            embabelTypes.filterIsInstance<DomainType>().toSet()

}

class DataDictionaryImpl(
    override val embabelTypes: Collection<EmbabelType>,
) : DataDictionary {

    constructor (
        vararg embabelTypes: Class<*>,
    ) : this(embabelTypes.map { DomainType(it) })
}
