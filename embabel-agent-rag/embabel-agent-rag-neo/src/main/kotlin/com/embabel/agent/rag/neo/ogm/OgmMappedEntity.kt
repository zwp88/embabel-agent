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
package com.embabel.agent.rag.neo.ogm

import com.embabel.agent.rag.MappedEntity
import com.embabel.common.core.types.NamedAndDescribed
import org.neo4j.ogm.annotation.Id

/**
 * Superclass for all entities that are mapped using Neo4j OGM.
 */
abstract class OgmMappedEntity(
    @Id
    override val id: String,
    override val uri: String? = null,
) : MappedEntity {

    override fun labels() =
        setOf(javaClass.simpleName) + super.labels()

    override val metadata: Map<String, Any?>
        get() = emptyMap()

    override fun toString() = infoString(verbose = false)

    override fun infoString(
        verbose: Boolean?,
        indent: Int,
    ): String {
        return "${javaClass.simpleName}:(${labels().joinToString(":")} id='$id')"
    }
}

abstract class OgmMappedNamedAndDescribedEntity(
    override val name: String,
    id: String,
) : OgmMappedEntity(id), NamedAndDescribed {

    override fun infoString(
        verbose: Boolean?,
        indent: Int,
    ): String {
        return "${javaClass.simpleName}: name=$name, description=$description, id=$id"
    }

    override fun embeddableValue(): String {
        return "${javaClass.simpleName}: name=$name, description=$description"
    }
}
