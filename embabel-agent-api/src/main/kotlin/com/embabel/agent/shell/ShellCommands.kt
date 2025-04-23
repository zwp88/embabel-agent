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

import com.embabel.agent.api.common.*
import com.embabel.agent.core.*
import com.embabel.agent.core.hitl.ConfirmationRequest
import com.embabel.agent.core.hitl.ConfirmationResponse
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.event.logging.personality.severance.LumonColors
import com.embabel.agent.experimental.form.Button
import com.embabel.agent.experimental.form.FormSubmission
import com.embabel.agent.experimental.form.TextField
import com.embabel.agent.experimental.hitl.FormBindingRequest
import com.embabel.agent.experimental.hitl.FormResponse
import com.embabel.common.util.AnsiColor
import com.embabel.common.util.bold
import com.embabel.common.util.color
import com.embabel.common.util.kotlin.loggerFor
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.text.WordUtils
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption


@ShellComponent
class ShellCommands(
    private val agentPlatform: AgentPlatform,
    private val environment: ConfigurableEnvironment,
    private val terminal: Terminal,
    private val objectMapper: ObjectMapper,
) {

    private val logger: Logger = LoggerFactory.getLogger(ShellCommands::class.java)

    private val agentProcesses = mutableListOf<AgentProcess>()

    private var blackboard: Blackboard? = null

    @ShellMethod(value = "Clear blackboard")
    fun clear(): String {
        blackboard = null
        return "Blackboard cleared"
    }

    @ShellMethod(value = "List all active Spring profiles")
    fun profiles(): String {
        val profiles = environment.activeProfiles
        return "Active profiles: ${profiles.joinToString()}"
    }

    @ShellMethod("List agents")
    fun agents(): String {
        return "${"Agents:".bold()}\n${
            agentPlatform.agents()
                .joinToString(separator = "\n${"-".repeat(120)}\n") { "\t" + it.infoString(verbose = true) }
        }"
    }

    @ShellMethod("List actions")
    fun actions(): String {
        return "${"Actions:".bold()}\n${
            agentPlatform.actions
                .joinToString(separator = "\n") { "\t" + it.infoString(verbose = true) }
        }"
    }

    @ShellMethod("List goals")
    fun goals(): String {
        return "${"Goals:".bold()}\n${
            agentPlatform.goals
                .joinToString(separator = "\n") { "\t" + it.infoString(verbose = true) }
        }"
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
        val verbosity = Verbosity(
            debug = false,
            showPrompts = true,
            showLlmResponses = false,
            showPlanning = true,
        )
        val output = executeIntent(
            intent = intent,
            open = false,
            processOptions = ProcessOptions(
                test = false,
                verbosity = verbosity,
            )
        )
        logger.info("Execute your own intent via the 'execute' command. Enclose the intent in quotes. For example:")
        logger.info("execute \"$intent\"".color(LumonColors.GREEN))
        return output
    }

    @ShellMethod(
        "Show last blackboard: The final state of a previous operation",
        key = ["blackboard", "bb"],
    )
    fun blackboard(): String {
        return if (blackboard == null) {
            "No blackboard available. Please run a command first."
        } else blackboard!!.infoString(verbose = true)
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
        @ShellOption(value = ["-s", "--state"], help = "Use existing blackboard") state: Boolean = false,

        @ShellOption(
            value = ["-s", "--showPlanning"],
            help = "show detailed planning info",
            defaultValue = "true",
        ) showPlanning: Boolean = true,
    ): String {
        val verbosity = Verbosity(
            debug = debug,
            showPrompts = showPrompts,
            showLlmResponses = showLlmResponses,
            showPlanning = showPlanning,
        )
        return executeIntent(
            open = open,
            intent = intent,
            processOptions = ProcessOptions(
                test = test,
                blackboard = if (state) blackboard else null,
                verbosity = verbosity,
                allowGoalChange = true,
                maxActions = 40,
            )
        )
    }

    private fun executeIntent(
        open: Boolean,
        processOptions: ProcessOptions,
        intent: String,
    ): String {
        logger.info("Created process options: $processOptions".color(LumonColors.MEMBRANE))

        return runProcess(verbosity = processOptions.verbosity, basis = intent) {
            if (open) {
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
        }

    }

    private fun recordAgentProcess(agentProcess: AgentProcess) {
        agentProcesses.add(agentProcess)
        blackboard = agentProcess.processContext.blackboard
    }

    fun runProcess(
        verbosity: Verbosity,
        basis: Any,
        run: () -> DynamicExecutionResult
    ): String {
        try {
            val result = run()
            logger.debug("Result: {}\n", result)
            recordAgentProcess(result.agentProcess)
            if (result.output is HasContent) {
                // TODO naive Markdown test
                if (result.output.text.contains("#")) {
                    return "\n" + markdownToConsole(result.output.text)
                        .color(LumonColors.GREEN) + "\n"
                }
                return WordUtils.wrap(result.output.text, 140).color(
                    LumonColors.GREEN,
                ) + "\n"
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                result.output
            ).color(
                LumonColors.GREEN
            )
        } catch (ngf: NoGoalFound) {
            if (verbosity.debug) {
                logger.info(
                    """
                    Failed to choose goal:
                        Rankings were: [${ngf.goalRankings.infoString()}]
                        Cutoff was ${agentPlatform.properties.goalConfidenceCutOff}
                    """.trimIndent().color(0xbfb8b8)
                )
            }
            return "I'm sorry. I don't know how to do that.\n"
        } catch (naf: NoAgentFound) {
            if (verbosity.debug) {
                logger.info(
                    """
                    Failed to choose agent:
                        Rankings were: [${naf.agentRankings.infoString()}]
                        Cutoff was ${agentPlatform.properties.agentConfidenceCutOff}
                    """.trimIndent().color(0xbfb8b8)
                )
            }
            return "I'm sorry. I don't know how to do that.\n"
        } catch (pese: ProcessExecutionStuckException) {
            return "I'm sorry. I don't know how to proceed.\n"
        } catch (pwe: ProcessWaitingException) {
            val awaitableResponse = when (pwe.awaitable) {
                is ConfirmationRequest<*> ->
                    confirmationResponseFromUserInput(
                        confirmationRequest = pwe.awaitable,
                        terminal = terminal,
                    )

                is FormBindingRequest<*> -> {
                    formBindingResponseFromUserInput(
                        formBindingRequest = pwe.awaitable,
                        terminal = terminal,
                    )
                }

                else -> {
                    TODO("Unhandled awaitable: ${pwe.awaitable.infoString()}")
                }
            }
            pwe.awaitable.onResponse(
                response = awaitableResponse,
                processContext = pwe.agentProcess!!.processContext
            )
            return runProcess(verbosity, basis) {
                DynamicExecutionResult.fromProcessStatus(
                    basis = basis,
                    agentProcess = pwe.agentProcess.run()
                )
            }
        }
    }


}

/**
 * Get further input
 */
private fun <T> doWithReader(
    terminal: Terminal,
    callback: (LineReader) -> T
): T {
    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    return callback(lineReader)
}

private fun confirmationResponseFromUserInput(
    confirmationRequest: ConfirmationRequest<*>,
    terminal: Terminal
): ConfirmationResponse {
    val confirmed = doWithReader(terminal) {
        it.readLine("${confirmationRequest.message} (y/n): ".color(AnsiColor.YELLOW))
            .equals("y", ignoreCase = true)
    }
    return ConfirmationResponse(
        awaitableId = confirmationRequest.id,
        accepted = confirmed,
    )
}

private fun formBindingResponseFromUserInput(
    formBindingRequest: FormBindingRequest<*>,
    terminal: Terminal
): FormResponse {
    val form = formBindingRequest.payload
    val values = mutableMapOf<String, Any>()

    return doWithReader(terminal) { lineReader ->
        loggerFor<ShellCommands>().info("Form: ${form.infoString()}")
        lineReader.printAbove(form.title)

        for (control in form.controls) {
            when (control) {
                is TextField -> {
                    var input: String
                    var isValid = false

                    while (!isValid) {
                        val prompt = "${control.label}${if (control.required) " *" else ""}: ".color(AnsiColor.YELLOW)
                        input = lineReader.readLine(prompt)//, control.value, null)

                        // Handle empty input for required fields
                        if (control.required && input.isBlank()) {
                            lineReader.printAbove("This field is required.")
                            continue
                        }

                        // Validate max length
                        if (control.maxLength != null && input.length > control.maxLength) {
                            lineReader.printAbove("Input exceeds maximum length of ${control.maxLength} characters.")
                            continue
                        }

                        // Validate pattern if specified
                        if (control.validationPattern != null && input.isNotBlank()) {
                            val regex = control.validationPattern.toRegex()
                            if (!input.matches(regex)) {
                                lineReader.printAbove(
                                    control.validationMessage ?: "Input doesn't match required format."
                                )
                                continue
                            }
                        }

                        values[control.id] = input
                        isValid = true
                    }
                }
                // Add handling for other control types here as needed
                // For example: Checkbox, RadioButton, Select, etc.
                is Button -> {
                    // Handle submit button click
                    // TODO finish this
                }

                else -> {
                    // Handle unsupported control type
                    lineReader.printAbove("Unsupported control type: ${control.type}")
                }
            }
        }

        val confirmSubmit = lineReader.readLine("Submit form? (y/n): ".color(AnsiColor.YELLOW))
            .equals("y", ignoreCase = true)

        if (!confirmSubmit) {
            TODO("Handle form submission cancellation")
        } else {
            FormResponse(
                awaitableId = formBindingRequest.id,
                formSubmission = FormSubmission(
                    formId = form.id,
                    values = values,
                )
            )
        }
    }
}
