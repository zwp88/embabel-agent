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
package com.embabel.agent.event.logging.personality

import com.embabel.agent.event.logging.LoggingAgenticEventListener

/**
 * Thanks to Kier
 */
class YodaLoggingAgenticEventListener : LoggingAgenticEventListener(
    welcomeMessage = """

 _______  _______  _______  __         .___________. __    __   _______     _______   ______   .______        ______  _______  __
|   ____||   ____||   ____||  |        |           ||  |  |  | |   ____|   |   ____| /  __  \  |   _  \      /      ||   ____||  |
|  |__   |  |__   |  |__   |  |        `---|  |----`|  |__|  | |  |__      |  |__   |  |  |  | |  |_)  |    |  ,----'|  |__   |  |
|   __|  |   __|  |   __|  |  |            |  |     |   __   | |   __|     |   __|  |  |  |  | |      /     |  |     |   __|  |  |
|  |     |  |____ |  |____ |  `----.       |  |     |  |  |  | |  |____    |  |     |  `--'  | |  |\  \----.|  `----.|  |____ |__|
|__|     |_______||_______||_______|       |__|     |__|  |__| |_______|   |__|      \______/  | _| `._____| \______||_______|(__)
                                                                                                                                                                                                                                                                                                                                                                                               |/
    """.trimIndent(),
    rankingChoiceMadeEventMessage = "Chosen goal {} I have with confidence {} based on {}",
    dymamicAgentCreationMessage = "You will find only what you bring in: Created agent {}",
    agentProcessCreationEventMessage = "Created a process I have: {}",
    agentProcessReadyToPlanEventMessage = "Difficult to see. Always in motion is the future: Process {} ready to plan from {}",
    agentProcessPlanFormulatedEventMessage = "Control, control, you must learn control! Process {} formulated plan <{}> from {}",
    processCompletionMessage = "Feel the force: Process {} completed in {}",
    processFailureMessage = "Powerful the dark side is: Process {} failed",
    objectAddedMessage = "A little more knowledge lights our way: Object added: {} to process {}",
    llmRequestEventMessage = "Ask LLM we will: Process {} requesting LLM transform from {} -> {}",
    actionExecutionStartMessage = "Do or do not. There is no try: Process {} executing action {}",
    actionExecutionResultMessage = "Powerful you have become: Process {} executed action {} in {}",
)
