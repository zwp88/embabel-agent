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
package com.embabel.agent.mcpserver.support

import com.embabel.agent.api.common.autonomy.AgentProcessExecution
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.api.common.autonomy.ProcessWaitingException
import com.embabel.agent.core.Goal
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity
import com.embabel.agent.core.hitl.FormBindingRequest
import com.embabel.agent.core.hitl.ResponseImpact
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.mcpserver.McpToolExportCallbackPublisher
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import com.embabel.common.core.types.HasInfoString
import com.embabel.common.util.loggerFor
import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServerExchange
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.support.ToolCallbacks
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.util.json.schema.JsonSchemaGenerator
import org.springframework.stereotype.Service

const val FORM_SUBMIT_TOOL_NAME = "submitFormAndResumeProcess"

/**
 * Return a tool callback for each goal.
 * These will be exposed via the MCP server.
 */
@Service
class PerGoalToolCallbackProvider(
    private val autonomy: Autonomy,
    private val objectMapper: ObjectMapper,
    private val llmOperations: LlmOperations,
) : McpToolExportCallbackPublisher {

    private val logger = LoggerFactory.getLogger(PerGoalToolCallbackProvider::class.java)

    override val toolCallbacks: List<ToolCallback>
        get() {
            return autonomy.agentPlatform.goals.map { goal ->
                toolForGoal(goal)
            } + ToolCallbacks.from(this)
        }

    @Tool(
        name = FORM_SUBMIT_TOOL_NAME,
        description = "Resume a process by providing the process ID and form content",
    )
    fun submitFormAndResumeProcess(
        processId: String,
        formData: String,
    ): String {
        val agentProcess = autonomy.agentPlatform.getAgentProcess(processId)
            ?: return "No process found with ID $processId"
        logger.info("Received AgentProcess {} form input: {}", processId, formData)
        val formBindingRequest = agentProcess.lastResult() as? FormBindingRequest<Any>
            ?: return "No form binding request found for process $processId"
        val prompt = """
            Given the content below, return the given object

            # Content
            $formData
        """.trimIndent()
        val formDataObject = llmOperations.doTransform(
            prompt = prompt,
            LlmInteraction.using(LlmOptions(criteria = ModelSelectionCriteria.Auto)),
            outputClass = formBindingRequest.outputClass,
            null,
        )
        val responseImpact = formBindingRequest.bind(
            boundInstance = formDataObject,
            agentProcess
        )
        if (responseImpact != ResponseImpact.UPDATED) {
            TODO("Handle unchanged response impact")
        }
        // Resume the agent process with the form data
        agentProcess.run()
        val dp = AgentProcessExecution.fromProcessStatus(formData, agentProcess)
        return toResponseString(dp)
    }

    private fun toResponseString(agentProcessExecution: AgentProcessExecution): String {
        return when (val output = agentProcessExecution.output) {
            is String -> output
            is HasInfoString -> {
                output.infoString(verbose = true)
            }

            is HasContent -> output.content
            else -> output.toString()
        }
    }

    /**
     * Create a tool callback for the given goal.
     */
    fun toolForGoal(goal: Goal): ToolCallback {
        return GoalToolCallback(goal)
    }

    override fun infoString(verbose: Boolean?): String {
        return "${javaClass.name} with ${toolCallbacks.size} tools"
    }


    /**
     * Spring AI ToolCallback implementation for a specific goal.
     */
    internal inner class GoalToolCallback(
        private val goal: Goal,
    ) : ToolCallback {

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
                    val js = JsonSchemaGenerator.generateForType(UserInput::class.java)
                    loggerFor<PerGoalToolCallbackProvider>().debug("Generated schema for ${goal.name}: $js")
                    return js
                }
            }
        }

        override fun call(
            toolInput: String,
        ): String {
            return call(toolInput, null)
        }

        override fun call(
            toolInput: String,
            toolContext: ToolContext?,
        ): String {
            val exchange = toolContext?.context["exchange"] as? McpSyncServerExchange
            val verbosity = Verbosity(
                showPrompts = true,
            )
            val userInput = objectMapper.readValue(toolInput, UserInput::class.java)
            val processOptions = ProcessOptions(verbosity = verbosity)
            val agent = autonomy.createGoalAgent(
                userInput = userInput,
                goal = goal,
                agentScope = autonomy.agentPlatform,
                // TODO Bug workaround
                prune = false,
            )
            try {
                val agentProcessExecution = autonomy.runAgent(
                    userInput = UserInput(toolInput),
                    processOptions = processOptions,
                    agent = agent,
                )
                logger.info("Goal response: {}", agentProcessExecution)
                return toResponseString(agentProcessExecution)
            } catch (pwe: ProcessWaitingException) {
//            require(exchange != null) {
//                "ProcessWaitingException requires an exchange to handle waiting for user input."
//            }
//            exchange.createMessage()
                val formBindingRequest = pwe.awaitable as FormBindingRequest<*>
                val response = """
                You must invoke the $FORM_SUBMIT_TOOL_NAME tool to proceed with the goal "${goal.name}".
                The arguments will be
                - processId: ${pwe.agentProcess!!.id},
                - formData: English text describing the form data to submit. See below

                Before invoking this, you must obtain information from the user
                as described in this form structure.
                ${formBindingRequest.toString()}
            """.trimIndent()
                logger.info("Returning waiting response:\n$response")
                return response
            }
        }

    }
}
