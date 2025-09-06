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
package com.embabel.agent.rag

import com.embabel.agent.domain.library.HasContent

/**
 * ContentElement that exists in a hierarchy,
 * such as a document with sections and subsections.
 */
interface HierarchicalContentElement : ContentElement {

    val parentId: String?

    override fun propertiesToPersist(): Map<String, Any?> = super.propertiesToPersist() + mapOf(
        "parentId" to parentId,
    )
}

/**
 * Root of a structured document
 */
interface ContentRoot : HierarchicalContentElement {
    val title: String
    override val parentId get() = null
}

sealed interface Section : HierarchicalContentElement {
    val title: String

    override fun propertiesToPersist(): Map<String, Any?> = super.propertiesToPersist() + mapOf(
        "title" to title,
    )
}

interface MaterializedSection : Section

interface ContainerSection : Section

interface MaterializedContainerSection : ContainerSection, MaterializedSection {

    val children: List<MaterializedSection>

    fun descendants(): List<MaterializedSection> =
        children + children.filterIsInstance<MaterializedContainerSection>().flatMap { containerChild ->
            containerChild.descendants()
        }

    fun leaves(): List<LeafSection> =
        children.filterIsInstance<LeafSection>() +
                children.filterIsInstance<MaterializedContainerSection>().flatMap { containerChild ->
                    containerChild.leaves()
                }
}

data class DefaultMaterializedContainerSection(
    override val id: String,
    override val uri: String? = null,
    override val title: String,
    override val children: List<MaterializedSection>,
    override val parentId: String? = null,
    override val metadata: Map<String, Any?> = emptyMap(),
) : MaterializedContainerSection

data class MaterializedContentRoot(
    override val id: String,
    override val uri: String? = null,
    override val title: String,
    override val children: List<MaterializedSection>,
    override val metadata: Map<String, Any?> = emptyMap(),
) : MaterializedContainerSection, ContentRoot


data class LeafSection(
    override val id: String,
    override val uri: String? = null,
    override val title: String,
    val text: String,
    override val parentId: String? = null,
    override val metadata: Map<String, Any?> = emptyMap(),
) : MaterializedSection, HasContent {

    override val content get() = text

    override fun propertiesToPersist(): Map<String, Any?> = super.propertiesToPersist() + mapOf(
        "text" to content,
    )
}
