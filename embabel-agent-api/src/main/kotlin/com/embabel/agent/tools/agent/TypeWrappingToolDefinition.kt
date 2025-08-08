package com.embabel.agent.tools.agent

import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.util.json.schema.JsonSchemaGenerator

/**
 * Tool definition that wraps an input type, generating a JSON schema for it.
 */
data class TypeWrappingToolDefinition(
    private val name: String,
    private val description: String,
    private val type: Class<*>,
) : ToolDefinition {

    override fun name(): String = name
    override fun description(): String = description

    override fun inputSchema(): String = JsonSchemaGenerator.generateForType(type)
}