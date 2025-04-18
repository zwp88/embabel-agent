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
package com.embabel.agent.event.logging

import com.embabel.agent.core.AgentProcessStatus
import com.embabel.agent.event.*
import com.embabel.agent.event.logging.personality.severance.LumonColors
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
    private val rankingChoiceRequestEventMessage: String = "Choosing {} based on {}",
    private val rankingChoiceMadeEventMessage: String = "Chose {} '{}' with confidence {} based on {}. Choices: {}",
    private val rankingChoiceNotMadeEventMessage: String = "Failed to choose {} based on {}. Choices: {}. Confidence cutoff: {}",
    private val dymamicAgentCreationMessage: String = "Created agent {}",
    private val agentProcessCreationEventMessage: String = "[{}] created",
    private val agentProcessReadyToPlanEventMessage: String = "[{}] ready to plan from {}",
    private val agentProcessPlanFormulatedEventMessage: String = "[{}] formulated plan {} from {}",
    private val processCompletionMessage: String = "[{}] completed in {}",
    private val processFailureMessage: String = "[{}] failed",
    private val objectAddedMessage: String = "{} Object added: {} to [{}]",
    private val objectBoundMessage: String = "{} Object bound: {} to {} in [{}]",
    private val functionCallRequestEventMessage: String = "[{}] calling function {} with payload {}",
    private val functionCallResponseEventMessage: String = "[{}] function {} response {} in {}ms with payload {}",
    private val llmRequestEventMessage: String = "[{}] requesting LLM transform {} from {} -> {} using {}",
    private val llmResponseEventMessage: () -> String = { "[{}] received LLM response {} of type {} from {} in {} seconds" },
    private val actionExecutionStartMessage: String = "[{}] executing action {}",
    private val actionExecutionResultMessage: String = "[{}] executed action {} in {}",
    private val progressUpdateEventMessage: String = "[{}] progress: {}",
    val logger: Logger = LoggerFactory.getLogger("Embabel"),
) : AgenticEventListener {

    init {
        welcomeMessage?.let {
            logger.info(it)
        }
    }

    override fun onPlatformEvent(event: AgentPlatformEvent) {
        when (event) {
            is RankingChoiceRequestEvent<*> -> {
                logger.info(
                    rankingChoiceRequestEventMessage,
                    event.type.simpleName,
                    event.basis,
                )
            }

            is RankingChoiceMadeEvent<*> -> {
                logger.info(
                    rankingChoiceMadeEventMessage,
                    event.type.simpleName,
                    event.choice.ranked.name,
                    event.choice.confidence,
                    event.basis,
                    event.rankings.infoString(),
                )
            }

            is RankingChoiceCouldNotBeMadeEvent<*> -> {
                logger.info(
                    rankingChoiceNotMadeEventMessage,
                    event.type.simpleName,
                    event.basis,
                    event.rankings.infoString(),
                    event.confidenceCutOff,
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
                    event.plan.infoString(verbose = event.agentProcess.processContext.processOptions.verbosity.showLongPlans),
                    event.worldState.state,
                )
            }

            is AgentProcessFunctionCallRequestEvent -> {
                logger.info(
                    functionCallRequestEventMessage,
                    event.processId,
                    event.function,
                    event.toolInput,
                )
            }

            is AgentProcessFunctionCallResponseEvent -> {
                logger.info(
                    functionCallResponseEventMessage,
                    event.processId,
                    event.function,
                    event.response,
                    event.runningTime.toMillis(),
                    event.toolInput,
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
                        logger.info("[{}] has invalid agent", event.processId)
                    }

                    else -> {
                        // Do nothing
                    }
                }
            }

            is ObjectAddedEvent -> {
                logger.info(objectAddedMessage, event.agentProcess.id, event.value, event.processId)
            }

            is ObjectBoundEvent -> {
                logger.info(objectBoundMessage, event.agentProcess.id, event.name, event.value, event.processId)
            }

            is LlmRequestEvent<*, *> -> {
                var message = llmRequestEventMessage
                if (event.agentProcess.processContext.processOptions.verbosity.showPrompts) {
                    message += "\nPrompt ${event.interaction.id}:\n${
                        event.prompt.color(AnsiColor.GREEN)
                    }\ntools: ${event.interaction.toolCallbacks.map { it.toolDefinition.name() }}"
                }
                logger.info(
                    message,
                    event.processId,
                    event.interaction.id.value,
                    event.input!!.javaClass.simpleName,
                    event.outputClass.simpleName,
                    event.interaction.llm,
                )
            }

            is LlmResponseEvent<*, *> -> {
                var message = llmResponseEventMessage()
                if (event.agentProcess.processContext.processOptions.verbosity.showLlmResponses) {
                    message += "\nResponse from prompt ${event.interaction.id}:\n${
                        ("" + event.response).color(
                            color = AnsiColor.YELLOW
                        )
                    }"
                }
                logger.info(
                    message,
                    event.processId,
                    event.interaction.id.value,
                    event.response::class.java.simpleName,
                    event.interaction.llm.model,
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

            is ProgressUpdateEvent -> {
                logger.info(
                    progressUpdateEventMessage,
                    event.processId,
                    event.createProgressBar(length = 50).color(LumonColors.MEMBRANE),
                )
            }

            else -> {
                // Do nothing
            }
        }
    }

}
