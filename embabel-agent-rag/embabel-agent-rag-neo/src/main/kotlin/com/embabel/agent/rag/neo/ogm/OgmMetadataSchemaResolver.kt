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

import com.embabel.agent.rag.EntitySearch
import com.embabel.agent.rag.NamedEntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.agent.rag.schema.*
import org.neo4j.ogm.session.SessionFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Infers schema from OGM metadata
 */
@Service
class OgmMetadataSchemaResolver(
    private val sessionFactory: SessionFactory,
    private val ogmCypherSearch: OgmCypherSearch,
    private val properties: NeoRagServiceProperties,
) : SchemaResolver {

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO is not using name
    // TODO not filtering entities
    override fun getSchema(entitySearch: EntitySearch): KnowledgeGraphSchema? {
        val metadata = sessionFactory.metaData()
        val relationships = mutableListOf<RelationshipDefinition>()
        val entityDefinitions = metadata.persistentEntities()
            .filter { it.hasPrimaryIndexField() }
            .map { entity ->
                val labels = entity.staticLabels().toSet()
                val entityDefinition = EntityDefinition(
                    labels = labels,
                    properties = emptyList(),
                    description = labels.joinToString(","),
                )
                entity.relationshipFields().forEach { relationshipField ->
                    val targetEntity = relationshipField.typeDescriptor.split(".").last()
                    relationships.add(
                        RelationshipDefinition(
                            sourceLabel = entityDefinition.type,
                            targetLabel = targetEntity,
                            type = relationshipField.relationship(),
                            description = relationshipField.name,
                            cardinality = if (relationshipField.isArray || relationshipField.isIterable) {
                                Cardinality.MANY
                            } else {
                                Cardinality.ONE
                            },
                        )
                    )
                }
                entityDefinition
            }
        if (entityDefinitions.size == 2 && relationships.isEmpty()) {
            // Special case of superclasses only
            return null
        }
        return KnowledgeGraphSchema(
            entities = entityDefinitions,
            relationships = relationships,
        )
    }

    private fun createEntity(
        entity: NamedEntityData,
        basis: Retrievable,
    ) {
        val params = mapOf(
            "id" to entity.id,
            "name" to entity.name,
            "description" to entity.description,
            "basisId" to basis.id,
            "properties" to entity.properties,
            "chunkNodeName" to properties.chunkNodeName,
            "entityLabels" to entity.labels() + properties.entityNodeName,
        )
        val result = ogmCypherSearch.query(
            purpose = "Merge entity",
            query = "create_entity",
            params = params,
            logger = logger,
        )
        if (result.queryStatistics().nodesCreated != 1) {
            logger.warn(
                "Expected to create 1 node, but created: {}. params={}",
                result.queryStatistics().nodesCreated,
                params
            )
        }
    }

}
