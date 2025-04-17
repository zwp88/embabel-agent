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
package com.embabel.agent.shell

import com.embabel.agent.core.*
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.event.logging.personality.LumonColors
import com.embabel.common.util.color
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.text.WordUtils
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.shell.jline.PromptProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption


/**
 * Vanilla prompt provider
 */
class DefaultPromptProvider : PromptProvider {
    override fun getPrompt() = AttributedString(
        "embabel> ",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
    )
}

@ShellComponent
class ShellCommands(
    private val agentPlatform: AgentPlatform,
    private val environment: ConfigurableEnvironment,
) {

    private val logger: Logger = LoggerFactory.getLogger(ShellCommands::class.java)

    private val agentProcessStatuses = mutableListOf<AgentProcessStatus>()

    @ShellMethod(value = "List all active Spring profiles")
    fun profiles(): String {
        val profiles = environment.activeProfiles
        return "Active profiles: ${profiles.joinToString()}"
    }

    @ShellMethod("List agents")
    fun agents(): String {
        return agentPlatform.infoString(verbose = true)
    }

    @ShellMethod("Information about the AgentPlatform")
    fun platform(): String {
        return """
            AgentPlatform: ${agentPlatform.name}
        """.trimIndent()
    }

    // Saves typing during test iterations
    @ShellMethod("Run a demo command")
    fun demo(): String {
        val intent = "Lynda is a scorpio. Find news for her"
        logger.info("Demo executing intent: '$intent'")
        val output = executeIntent(
            intent = intent, open = false,
            verbosity = Verbosity(
                debug = false,
                showPrompts = true,
                showLlmResponses = false,
                showPlanning = true,
            )
        )
        logger.info("Execute your own intent via the 'execute' command. Enclose the intent in quotes. For example:")
        logger.info("execute \"$intent\"".color(LumonColors.Green))
        return output
    }

    @ShellMethod(
        "Show last blackboard: The final state of a previous operation",
        key = ["blackboard", "bb"],
    )
    fun blackboard(): String {
        if (agentProcessStatuses.isEmpty()) {
            return "No blackboard available, as no agent process has run. Please run a command first."
        }
        val ap = agentProcessStatuses.last().agentProcess
        return ap.processContext.blackboard.infoString(verbose = true)
    }

    @ShellMethod("List available tool groups")
    fun tools(): String {
        return agentPlatform.toolGroupResolver.availableToolGroups()
            .map {
                val tgr = agentPlatform.toolGroupResolver.resolveToolGroup(it.role)
                return tgr.resolvedToolGroup?.let {
                    "${it.metadata}:\n\t${
                        it.toolCallbacks.joinToString("\n\t") { tc -> "${tc.toolDefinition.name()}: ${tc.toolDefinition.description()}" }
                    }"
                } ?: "Failure: ${tgr.failureMessage}"
            }
            .joinToString(
                separator = "\n",
                prefix = "\t",
                postfix = "\n",
            )
    }

    @ShellMethod(
        "Execute a task. Put the task in double quotes. For example:\n\tx \"Lynda is a scorpio. Find news for her\" -p",
        key = ["execute", "x"],
    )
    fun execute(
        @ShellOption(help = "what the agent system should do") intent: String,
        @ShellOption(
            value = ["-o", "--open"],
            help = "run in open mode, choosing a goal and using all actions that can help achieve it",
        ) open: Boolean = false,
        @ShellOption(value = ["-t", "--test"], help = "run in help mode") test: Boolean = false,
        @ShellOption(value = ["-p", "--showPrompts"], help = "show prompts to LLMs") showPrompts: Boolean,
        @ShellOption(value = ["-r", "--showResponses"], help = "show LLM responses") showLlmResponses: Boolean = false,
        @ShellOption(value = ["-d", "--debug"], help = "show debug info") debug: Boolean = false,
        @ShellOption(
            value = ["-s", "--showPlanning"],
            help = "show detailed planning info",
        ) showPlanning: Boolean = true,
    ): String {
        return executeIntent(
            open = open,
            test = test,
            intent = intent,
            verbosity = Verbosity(
                debug = debug,
                showPrompts = showPrompts,
                showLlmResponses = showLlmResponses,
                showPlanning = showPlanning,
            ),
        )
    }

    private fun executeIntent(
        open: Boolean,
        test: Boolean = false,
        verbosity: Verbosity,
        intent: String,
    ): String {
        val processOptions = ProcessOptions(
            test = test,
            verbosity = verbosity,
        )
        logger.info("Created process options: $processOptions".color(LumonColors.Membrane))
        val result = if (open) {
            logger.info("Executing in open mode: Trying to find appropriate goal and using all actions known to platform that can help achieve it")
            agentPlatform.chooseAndAccomplishGoal(
                intent = intent,
                processOptions = processOptions
            )
        } else {
            logger.info("Executing in closed mode: Trying to find appropriate agent")
            agentPlatform.chooseAndRunAgent(
                intent = intent,
                processOptions = processOptions
            )
        }

        if (result is DynamicExecutionResult.Success) {
            agentProcessStatuses.add(result.agentProcessStatus)
        }

        logger.debug("Result: {}\n", result)
        when (result) {
            is DynamicExecutionResult.NoGoalFound -> {
                if (verbosity.debug) {
                    logger.info(
                        """
                    Failed to choose goal:
                        Rankings were: [${result.goalRankings.infoString()}]
                        Cutoff was ${agentPlatform.properties.goalConfidenceCutOff}
                    """.trimIndent().color(0xbfb8b8)
                    )
                }
                return "I'm sorry. I don't know how to do that.\n"
            }

            is DynamicExecutionResult.NoAgentFound -> {
                if (verbosity.debug) {
                    logger.info(
                        """
                    Failed to choose agent:
                        Rankings were: [${result.agentRankings.infoString()}]
                        Cutoff was ${agentPlatform.properties.agentConfidenceCutOff}
                    """.trimIndent().color(0xbfb8b8)
                    )
                }
                return "I'm sorry. I don't know how to do that.\n"
            }

            is DynamicExecutionResult.Success -> {
                if (result.output is HasContent) {
                    // TODO naive Markdown test
                    if (result.output.text.contains("#")) {
                        return "\n" + markdownToConsole(result.output.text)
                            .color(LumonColors.Green) + "\n"
                    }
                    return WordUtils.wrap(result.output.text, 140).color(
                        LumonColors.Green,
                    ) + "\n"
                }

                return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                    result.output
                ).color(
                    LumonColors.Green
                )
            }
        }
    }
}
