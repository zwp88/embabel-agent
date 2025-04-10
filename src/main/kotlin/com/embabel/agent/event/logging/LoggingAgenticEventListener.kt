/*
 * Copyright 2025 Embabel Software, Inc.
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
package com.embabel.agent.event.logging

import com.embabel.agent.AgentProcessStatus
import com.embabel.agent.event.*
import com.embabel.common.util.AnsiColor
import com.embabel.common.util.color
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Default implementation of the AgenticEventListener
 * with vanilla messages.
 * Subclasses can pass in format Strings for messages they wish to override
 * Messages must respect format variables
 */
open class LoggingAgenticEventListener(
    welcomeMessage: String? = null,
    private val goalChoiceRequestEventMessage: String = "Choosing goal based on {}",
    private val goalChoiceMadeEventMessage: String = "Chose goal '{}' with confidence {} based on {}",
    private val goalChoiceNotMadeEventMessage: String = "Failed to choose goal based on {}: {}",
    private val dymamicAgentCreationMessage: String = "Created agent {}",
    private val agentProcessCreationEventMessage: String = "Process {} created",
    private val agentProcessReadyToPlanEventMessage: String = "Process {} ready to plan from {}",
    private val agentProcessPlanFormulatedEventMessage: String = "Process {} formulated plan <{}> from {}",
    private val processCompletionMessage: String = "Process {} completed in {}",
    private val processFailureMessage: String = "Process {} failed",
    private val objectAddedMessage: String = "Object added: {} to process {}",
    private val functionCallRequestEventMessage: String = "Process {} calling function {} with arguments {}",
    private val functionCallResponseEventMessage: String = "Process {} response in {}ms {} from function {} with arguments {}",
    private val transformRequestEventMessage: String = "Process {} requesting LLM transform from {} -> {} using {}",
    private val transformResponseEventMessage: String = "Process {} received LLM response of type {} from {} in {} seconds",
    private val actionExecutionStartMessage: String = "Process {} executing action {}",
    private val actionExecutionResultMessage: String = "Process {} executed action {} in {}",
) : AgenticEventListener {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        welcomeMessage?.let {
            logger.info(it)
        }
    }

    override fun onPlatformEvent(event: AgentPlatformEvent) {
        when (event) {
            is GoalChoiceRequestEvent -> {
                logger.info(
                    goalChoiceRequestEventMessage,
                    event.basis.javaClass.simpleName,
                )
            }

            is GoalChoiceMadeEvent -> {
                logger.info(
                    goalChoiceMadeEventMessage,
                    event.goalChoice.goal.name,
                    event.goalChoice.confidence,
                    event.basis.javaClass.simpleName,
                    event.goalRankings.infoString(),
                )
            }

            is GoalChoiceCouldNotBeMadeEvent -> {
                logger.info(
                    goalChoiceNotMadeEventMessage,
                    event.basis.javaClass.simpleName,
                    event.goalRankings.infoString(),
                )
            }

            is DynamicAgentCreationEvent -> {
                logger.info(dymamicAgentCreationMessage, event.agent.infoString())
            }

            else -> {
                // Do nothing
            }
        }
    }

    override fun onProcessEvent(event: AgentProcessEvent) {
        when (event) {

            is AgentProcessCreationEvent -> {
                logger.info(
                    agentProcessCreationEventMessage,
                    event.processId,
                )
            }

            is AgentProcessReadyToPlanEvent -> {
                logger.info(agentProcessReadyToPlanEventMessage, event.processId, event.worldState.state)
            }

            is AgentProcessPlanFormulatedEvent -> {
                logger.info(
                    agentProcessPlanFormulatedEventMessage,
                    event.processId,
                    event.plan.infoString(),
                    event.worldState.state,
                )
            }

            is AgentProcessFunctionCallRequestEvent -> {
                logger.info(
                    functionCallRequestEventMessage,
                    event.processId,
                    event.function,
                    event.arguments,
                )
            }

            is AgentProcessFunctionCallResponseEvent -> {
                logger.info(
                    functionCallResponseEventMessage,
                    event.processId,
                    event.response,
                    event.runningTime.toMillis(),
                    event.function,
                    event.arguments,
                )
            }

            is AgentProcessTerminationEvent -> {
                when (event.agentProcessStatus) {
                    is AgentProcessStatus.Completed -> {
                        logger.info(
                            processCompletionMessage,
                            event.processId,
                            event.agentProcessStatus.runningTime,
                        )
                    }

                    is AgentProcessStatus.Failed -> {
                        logger.info(processFailureMessage, event.agentProcessStatus.agentProcess.id)
                    }

                    is AgentProcessStatus.InvalidAgent -> {
                        logger.info("Process {} has invalid agent", event.processId)
                    }

                    else -> {
                        // Do nothing
                    }
                }
            }

            is ObjectAddedEvent -> {
                logger.info(objectAddedMessage, event.value, event.processId)
            }

            is LlmTransformRequestEvent<*, *> -> {
                var message = transformRequestEventMessage
                if (event.agentProcess.processContext.processOptions.verbosity.showPrompts) {
                    message += "\nPrompt: ${
                        event.prompt.color(AnsiColor.GREEN)
                    }\ntools: ${event.tools.map { it.toolDefinition.name() }}"
                }
                logger.info(
                    message,
                    event.processId,
                    event.input!!.javaClass.simpleName,
                    event.outputClass.simpleName,
                    event.llmOptions,
                )
            }

            is LlmTransformResponseEvent<*, *> -> {
                var message = transformResponseEventMessage
                if (event.agentProcess.processContext.processOptions.verbosity.showLlmResponses) {
                    message += "\nResponse: ${
                        ("" + event.response).color(
                            color = AnsiColor.YELLOW
                        )
                    }"
                }
                logger.info(
                    message,
                    event.processId,
                    event.response::class.java.simpleName,
                    event.llmOptions.model,
                    event.runningTime.seconds,
                )
            }

            is ActionExecutionStartEvent -> {
                logger.info(actionExecutionStartMessage, event.processId, event.action.name)
            }

            is ActionExecutionResultEvent -> {
                logger.info(
                    actionExecutionResultMessage,
                    event.processId,
                    event.action.name,
                    event.actionStatus.runningTime,
                )
            }
        }
    }

}
