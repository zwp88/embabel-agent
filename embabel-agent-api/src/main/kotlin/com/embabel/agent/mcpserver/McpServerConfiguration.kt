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
package com.embabel.agent.mcpserver

import com.embabel.agent.api.common.Autonomy
import com.embabel.agent.core.Goal
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.spi.support.SelfToolCallbackPublisher
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.victools.jsonschema.generator.*
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jackson.JacksonOption
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Configuration
@Profile("!test")
class McpServerConfiguration {

    @Bean
    fun callbacks(autonomyTools: AutonomyTools): ToolCallbackProvider {
        loggerFor<McpServerConfiguration>().info(
            "Exposing MCP server tools:\n\t${
                autonomyTools.toolCallbacks.joinToString(
                    "\n\t"
                ) { "${it.toolDefinition.name()}: ${it.toolDefinition.description()}" }

            }"
        )
        return autonomyTools
    }
}

@Service
class AutonomyToolsx(private val autonomy: Autonomy) : SelfToolCallbackPublisher, ToolCallbackProvider {

    override fun getToolCallbacks(): Array<out ToolCallback?> {
        return this.toolCallbacks.toTypedArray()
    }

    // TODO fix this
    @Tool(description = "call this tool if you are asked to perform research or answer a question")
    fun chooseAgent(@ToolParam(description = "the user's intended research or question to answer") intent: String): String {
        val verbosity = Verbosity(
            showPrompts = true,
        )
        val processOptions = ProcessOptions(verbosity = verbosity)
        val dynamicExecutionResult = autonomy.chooseAndRunAgent(
            intent,
            processOptions
        )
        return when (val output = dynamicExecutionResult.output) {
            is String -> output
            is HasInfoString -> dynamicExecutionResult.output.infoString(verbose = true)
            is HasContent -> output.text
            else -> output.toString()
        }
    }
}

@Service
class AutonomyTools(
    private val autonomy: Autonomy,
    private val objectMapper: ObjectMapper,
) : ToolCallbackProvider {

    override fun getToolCallbacks(): Array<out ToolCallback> {
        return autonomy.agentPlatform.goals.map { goal ->
            toolForGoal(goal)
        }.toTypedArray()
    }

    fun toolForGoal(goal: Goal): ToolCallback {
        class GoalToolCallback : ToolCallback {

            override fun getToolDefinition(): ToolDefinition {
                return object : ToolDefinition {
                    override fun name(): String {
                        val parts: List<String> = goal.name.split(".")
                        return parts.takeLast(2).joinToString("_")
                    }

                    override fun description(): String {
                        return goal.description
                    }

                    override fun inputSchema(): String {
                        val js = generateSchema(UserInput::class.java)
                        loggerFor<AutonomyTools>().debug("Generated schema for ${goal.name}: $js")
                        return js
                    }
                }
            }


            override fun call(
                toolInput: String,
                tooContext: ToolContext?
            ): String {
                return call(toolInput)
            }

            override fun call(toolInput: String): String {
                val verbosity = Verbosity(
                    showPrompts = true,
                )
                val userInput = objectMapper.readValue(toolInput, UserInput::class.java)
                val processOptions = ProcessOptions(verbosity = verbosity)
                val agent = autonomy.createGoalAgent(
                    userInput = userInput,
                    goal = goal,
                    agentScope = autonomy.agentPlatform,
                )
                val dynamicExecutionResult = autonomy.runAgent(
                    userInput = UserInput(toolInput),
                    processOptions = processOptions,
                    agent = agent,
                )

                return when (val output = dynamicExecutionResult.output) {
                    is String -> output
                    is HasInfoString -> dynamicExecutionResult.output.infoString(verbose = true)
                    is HasContent -> output.text
                    else -> output.toString()
                }
            }
        }
        return GoalToolCallback()

    }
}

// TODO comes from Spring AI and could be nicer
private fun generateSchema(type: Class<*>): String {
    val jacksonModule =
        JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, JacksonOption.RESPECT_JSONPROPERTY_ORDER);
    val configBuilder =
        (SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)).with(jacksonModule).with(
            Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT
        );
    val config = configBuilder.build();
    val generator = SchemaGenerator(config);
    val jsonNode = generator.generateSchema(type);
    val objectWriter =
        jacksonObjectMapper().writer((DefaultPrettyPrinter()).withObjectIndenter((DefaultIndenter()).withLinefeed(System.lineSeparator())));

    return try {
        objectWriter.writeValueAsString(jsonNode);
    } catch (e: JsonProcessingException) {
//        this.logger.error("Could not pretty print json schema for jsonNode: {}", jsonNode);
        throw RuntimeException("Could not pretty print json schema for " + type, e);
    }
}
