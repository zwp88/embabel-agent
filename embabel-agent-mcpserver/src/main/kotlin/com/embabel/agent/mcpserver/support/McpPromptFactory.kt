package com.embabel.agent.mcpserver.support

import com.embabel.common.core.types.Described
import com.embabel.common.core.types.Named
import com.embabel.common.core.types.Timestamped
import com.embabel.common.util.NameUtils
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

/**
 * Create Prompt specifications for the MCP server.
 * @param excludedInterfaces Set of interfaces whose fields should be excluded from the prompt arguments.
 */
class McpPromptFactory(
    val excludedInterfaces: Set<Class<*>> = setOf(
        Timestamped::class.java,
    )
) {

    /**
     * Creates a synchronous prompt specification for a given type.
     * @param goal The goal for which the prompt is created.
     * @param inputType The class type of the
     * @param inputType The class type of the input for the prompt.
     * @param name The name of the prompt if we want to customize it
     * @param description A description of the prompt if we want to customize it
     */
    fun <G> syncPromptSpecificationForType(
        goal: G,
        inputType: Class<*>,
        name: String = goal.name,
        description: String = goal.description,
    ): McpServerFeatures.SyncPromptSpecification where G : Named, G : Described {
        return McpServerFeatures.SyncPromptSpecification(
            McpSchema.Prompt(
                name,
                description,
                argumentsFromType(inputType),
            )
        ) { syncServerExchange, getPromptRequest ->
            McpSchema.GetPromptResult(
                "$name-result",
                listOf(
                    McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        McpSchema.TextContent(
                            """
                            Use the following information to achieve goal ${goal.name}" - <${goal.description}>:
                            ${
                                getPromptRequest.arguments.entries.joinToString(separator = "\n") { "${it.key}=${it.value}" }
                            }
                        """.trimIndent()
                        )
                    )
                ),
            )
        }
    }

    /**
     * Extracts MCP prompt arguments from a given type,
     * excluding fields that match methods in the excluded interfaces.
     */
    internal fun argumentsFromType(type: Class<*>): List<McpSchema.PromptArgument> {
        val excludedFields: Iterable<Method> = excludedInterfaces.flatMap {
            it.methods.toList()
        }
        val args = mutableListOf<McpSchema.PromptArgument>()
        ReflectionUtils.doWithFields(type) { field ->
            if (field.isSynthetic) {
                return@doWithFields
            }
            if (excludedFields.any { NameUtils.beanMethodToPropertyName(it.name) == field.name }) {
                return@doWithFields
            }
            val name = field.name
            val description = field.getAnnotation(JsonPropertyDescription::class.java)?.value ?: name
            val descriptionWithType = "$description: ${field.type.simpleName}"
            args.add(McpSchema.PromptArgument(name, descriptionWithType, true))
        }
        return args
    }
}