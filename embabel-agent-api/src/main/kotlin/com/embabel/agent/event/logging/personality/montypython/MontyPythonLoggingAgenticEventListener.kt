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
package com.embabel.agent.event.logging.personality.montypython

import com.embabel.agent.event.logging.LoggingAgenticEventListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * And now for something completely different
 */
@Component
@Profile("montypython")
class MontyPythonLoggingAgenticEventListener : LoggingAgenticEventListener(
    logger = LoggerFactory.getLogger("FlyingCircus"),
    welcomeMessage = """

         /\                                                 /\
        /  \      NOBODY EXPECTS THE SPANISH INQUISITION!  /  \
       /    \                                             /    \
      /      \                                           /      \
     /        \                                         /        \
    /          \                                       /          \
    |    ||    |                                       |    ||    |
    |    ||    |                                       |    ||    |
    |    ||    |                                       |    ||    |
    |    ||    |                                       |    ||    |
    |    ||    |                                       |    ||    |
    |    \\___ |                                       | ___//    |
    |          |                                       |          |
    |  MONTY   |                                       |  PYTHON  |
    |  FLYING  |                                       |  CIRCUS  |
    |__________|                                       |__________|

    """.trimIndent(),
    agentDeploymentEventMessage = "Found a parrot: {}\n\tdescription: {}",
    rankingChoiceMadeEventMessage = "You don't vote for kings. We have chosen {} with {} certainty based on {}",
    dynamicAgentCreationMessage = "It's not dead yet: Created agent {}",
    agentProcessCreationEventMessage = "And now for something completely different: {}",
    agentProcessReadyToPlanEventMessage = "My brain hurts! Process {} ready to plan from {}",
    agentProcessPlanFormulatedEventMessage = "We've found a witch! Process {} formulated plan <{}> from {}",
    processCompletionMessage = "Tis but a scratch: Process {} completed in {}",
    processFailureMessage = "It's just a flesh wound: Process {} failed",
    objectAddedMessage = "Bring out your dead! Object added: {} to process {}",
    llmRequestEventMessage = "Strange women lying in ponds: Process {} requesting LLM transform from {} -> {}",
    actionExecutionStartMessage = "Run away! Run away! Process {} executing action {}",
    actionExecutionResultMessage = "I fart in your general direction! Process {} executed action {} in {}",
)
