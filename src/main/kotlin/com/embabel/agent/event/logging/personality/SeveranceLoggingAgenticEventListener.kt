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
import com.embabel.common.util.color
import com.embabel.common.util.hexToRgb
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Thanks to Kier
 */
@Service
@Profile("severance")
class SeveranceLoggingAgenticEventListener : LoggingAgenticEventListener(
    logger = LoggerFactory.getLogger("MDR"),
    welcomeMessage = """
        ----------------------------------------------------------------------
        Kier, chosen one, Kier.
        Kier, brilliant one, Kier.
        Brings the bounty to the plain through the torment, through the rains,
        Progress, knowledge show no fear,
        Kier, chosen one, Kier.

        ▗▖   ▗▖ ▗▖▗▖  ▗▖ ▗▄▖ ▗▖  ▗▖
        ▐▌   ▐▌ ▐▌▐▛▚▞▜▌▐▌ ▐▌▐▛▚▖▐▌
        ▐▌   ▐▌ ▐▌▐▌  ▐▌▐▌ ▐▌▐▌ ▝▜▌
        ▐▙▄▄▖▝▚▄▞▘▐▌  ▐▌▝▚▄▞▘▐▌  ▐▌

    """.trimIndent().color(hexToRgb(LumonColors.Membrane)),
    rankingChoiceRequestEventMessage = kier("Choosing {} based on {}"),
    rankingChoiceMadeEventMessage = kier(
        """
        Chose {} '{}' with confidence {} based on {}: {}
            May my cunning acument slice through the fog of small minds, guiding them to their great purpose in labor.
        """.trimIndent()
    ),
    rankingChoiceNotMadeEventMessage = "WOE: Failed to choose {} based on {}: {}. Confidence cutoff: {}",
    dymamicAgentCreationMessage = "WILES: Created agent {}",
    agentProcessCreationEventMessage = kier(
        """
        May my gaze be singularly placed upon the path, may the words of virtue guide me in the daily labor of my great undertaking.
            Process {} created
                    """.trimIndent()
    ),
    agentProcessReadyToPlanEventMessage = "WIT: Process {} ready to plan from {}",
    agentProcessPlanFormulatedEventMessage = "WILES: Process {} formulated plan <{}> from {}",
    processCompletionMessage = """
        🧔🏼‍♂️ PRAISE KIER: Cold Harbor 100% complete.
        Process {} completed in {}
        In refining your macrodata file, you have brought glory to this company, and to me, Kier Eagan.

        The Board has concluded the call.
        """.trimIndent(),
    processFailureMessage = "WOE: Process {} failed",
    objectAddedMessage = "PROBITY: Object added: {} to process {}",
    functionCallRequestEventMessage = "VERVE: Process {} calling function {} with arguments {}",
    functionCallResponseEventMessage = "VISION: Process {} response in {}ms {} from function {} with arguments {}",
    transformRequestEventMessage = "🖥️ MACRODATA REFINEMENT: Process {} requesting LLM transform from {} -> {} using {}",
    transformResponseEventMessage = """
        Process {} received LLM response of type {} from {} in {} seconds
        ${kier("I knew you could do it, ${System.getProperty("user.name")}. Even in your darkest moments I could see you arriving here")}
        """.trimIndent(),
    actionExecutionStartMessage = "VERVE: Process {} executing action {}",
    actionExecutionResultMessage = "CHEER: Process {} executed action {} in {}",
)
