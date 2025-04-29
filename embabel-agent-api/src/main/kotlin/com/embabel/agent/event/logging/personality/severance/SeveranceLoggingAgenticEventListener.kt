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
package com.embabel.agent.event.logging.personality.severance

import com.embabel.agent.event.logging.LoggingAgenticEventListener
import com.embabel.common.util.color
import com.embabel.common.util.hexToRgb
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

val MdrFiles = listOf(
    "Allentown", "Dranesville", "Wellington",
    "Tumwater", "Lucknow", "Sopchoppy", "Astoria",
    "Loveland", "St Pierre", "Zurich", "Cairns",
)

val TransformSuccessResponses = MdrFiles.map {
    "$it 100% Complete"
} +
        kier("I knew you could do it. Even in your darkest moments I could see you arriving here")

val CompletionMessages = listOf(
    """
        🧔🏼‍♂️ PRAISE KIER: Cold Harbor 100% complete.
        In refining your macrodata file, you have brought glory to this company, and to me, Kier Eagan.
    """.trimIndent(),
    "May I introduce choreography and merriment",
    "The Founder wished to witness the historic completion of your 25th file.",
    "Goodly splendors await upon your victory. Love, Mr. Milchick",
    "It's truly special to host a man so illustrious, so sapient, so magnanimous...",
    "See you at the Equator",
    "The barrier is holding. She feels nothing. It's beautiful.",
    "Mammalians Nurturable brings an offering.",
    """
        In completing your 25th Macrodata file, you have drawn my grand agendum nearer to fulfillment,
        thus making you one of the most important people in history.
    """.trimIndent()
)

/**
 * Thanks to Kier
 */
@Service
@Profile("severance")
class SeveranceLoggingAgenticEventListener : LoggingAgenticEventListener(
    url = "https://www.imdb.com/title/tt11280740/",
    logger = LoggerFactory.getLogger("MDR"),
    welcomeMessage = """

        Kier, chosen one, Kier.
        Kier, brilliant one, Kier.
        Brings the bounty to the plain through the torment, through the rains,
        Progress, knowledge show no fear,
        Kier, chosen one, Kier.

        ▗▖   ▗▖ ▗▖▗▖  ▗▖ ▗▄▖ ▗▖  ▗▖
        ▐▌   ▐▌ ▐▌▐▛▚▞▜▌▐▌ ▐▌▐▛▚▖▐▌
        ▐▌   ▐▌ ▐▌▐▌  ▐▌▐▌ ▐▌▐▌ ▝▜▌
        ▐▙▄▄▖▝▚▄▞▘▐▌  ▐▌▝▚▄▞▘▐▌  ▐▌

    """.trimIndent().color(hexToRgb(LumonColors.MEMBRANE)),
    agentDeploymentEventMessage = "WILES: Deployed agent {}\n\tdescription: {}",
    rankingChoiceRequestEventMessage = kier("Choosing {} based on {}"),
    rankingChoiceMadeEventMessage = kier(
        """
        Chose {} '{}' with confidence {} based on {}. Choices: {}
            May my cunning acument slice through the fog of small minds, guiding them to their great purpose in labor.
        """.trimIndent()
    ),
    rankingChoiceNotMadeEventMessage = "WOE: Failed to choose {} based on {}. Choices: {}. Confidence cutoff: {}",
    dynamicAgentCreationMessage = "WILES: Created agent {}",
    agentProcessCreationEventMessage = kier(
        """
        May my gaze be singularly placed upon the path, may the words of virtue guide me in the daily labor of my great undertaking.
            [{}] created
                    """.trimIndent()
    ),
    agentProcessReadyToPlanEventMessage = "[{}] WIT  ready to plan from {}",
    agentProcessPlanFormulatedEventMessage = "[{}] WILES: formulated plan {} from {}".color(LumonColors.MEMBRANE),
    processCompletionMessage = """
        [{}] completed in {}
        ${CompletionMessages.random()}

        ${"The Board has concluded the call.".color(LumonColors.MEMBRANE)}

        """.trimIndent(),
    processFailureMessage = "[{}] WOE: failed",
    objectAddedMessage = "[{}] Perpetuity wing: object added: {}",
    objectBoundMessage = "[{}] Perpetuity wing: object bound: {} to {}",
    functionCallRequestEventMessage = "[{}] VERVE: tool {}({})",
    functionCallResponseEventMessage = "[{}] VISION: tool {} -> {} in {}ms with payload {}",
    llmRequestEventMessage = "[{}] 🖥️ MACRODATA REFINEMENT: requesting LLM transform {} from {} -> {} using {}",
    llmResponseEventMessage = {
        """
        [{}] received LLM response {} of type {} from {} in {} seconds
        ${TransformSuccessResponses.random()}
        """.trimIndent()
    },
    actionExecutionStartMessage = "[{}] VERVE: executing action {}",
    actionExecutionResultMessage = "[{}] CHEER: completed action {} in {}",
    progressUpdateEventMessage = "[{}] Industry: {}",
)
