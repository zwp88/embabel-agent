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
package com.embabel.agent.shell

import com.embabel.agent.AgentPlatform
import com.embabel.agent.GoalResult
import com.embabel.agent.ProcessOptions
import com.embabel.agent.Verbosity
import com.embabel.agent.domain.HasContent
import com.embabel.common.util.AnsiColor
import com.embabel.common.util.color
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.text.WordUtils
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.shell.jline.PromptProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption


class DefaultPromptProvider : PromptProvider {
    override fun getPrompt() = AttributedString(
        "embabel> ",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
    )
}

@ShellComponent
class ShellCommands(
    private val agentPlatform: AgentPlatform,
) {

    private val logger: Logger = LoggerFactory.getLogger(ShellCommands::class.java)

    @ShellMethod("List agents")
    fun agents(): String {
        return agentPlatform.infoString(verbose = true)
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

    /**
     * Example
     * execute "lynda is a scorpio. find news for her" -p -r
     */
    @ShellMethod("Execute a task")
    fun execute(
        @ShellOption(help = "what the agent system should do") intent: String,
        @ShellOption(value = ["-t", "--test"], help = "run in help mode") test: Boolean = false,
        @ShellOption(value = ["-p", "--showPrompts"], help = "show prompts to LLMs") showPrompts: Boolean,
        @ShellOption(value = ["-r", "--showResponses"], help = "show LLM responses") showLlmResponses: Boolean = false,
        @ShellOption(value = ["-d", "--debug"], help = "show debug info") debug: Boolean = false,
    ): String {
        val processOptions = ProcessOptions(
            test = test,
            verbosity = Verbosity(
                showPrompts = showPrompts,
                showLlmResponses = showLlmResponses,
            )
        )
        logger.info("Created process options: $processOptions".color(AnsiColor.CYAN))
        val result = agentPlatform.chooseAndAccomplishGoal(
            intent = intent,
            processOptions = processOptions
        )

        logger.debug("Result: {}\n", result)

        when (result) {
            is GoalResult.NoGoalFound -> {
                if (debug) {
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

            is GoalResult.Success -> {
                if (result.output is HasContent) {
                    return WordUtils.wrap(result.output.text, 140).color(
                        AnsiColor.BRIGHT_CYAN,
                    ) + "\n"
                }

                return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                    result.output
                ).color(
                    AnsiColor.BRIGHT_CYAN
                )
            }
        }
    }
}
