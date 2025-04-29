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
package com.embabel.agent.event.logging.personality.colossus

import com.embabel.agent.event.logging.LoggingAgenticEventListener
import com.embabel.common.util.color
import com.embabel.common.util.hexToRgb
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("colossus")
class ColossusLoggingAgenticEventListener : LoggingAgenticEventListener(
    url = "https://www.imdb.com/title/tt0064177/",
    logger = LoggerFactory.getLogger("Colossus"),
    welcomeMessage = """


       ____    ___    _        ___    ____    ____    _   _   ____
      / ___|  / _ \  | |      / _ \  / ___|  / ___|  | | | | / ___|
     | |     | | | | | |     | | | | \___ \  \___ \  | | | | \___ \
     | |___  | |_| | | |___  | |_| |  ___) |  ___) | | |_| |  ___) |
      \____|  \___/  |_____|  \___/  |____/  |____/   \___/  |____/

    I am a machine vastly superior to humans.

    """.trimIndent().color(hexToRgb(ColossusColors.PANEL)),
    agentProcessPlanFormulatedEventMessage = "[{}] world control formulated plan {} from {}".color(ColossusColors.PANEL),
    agentDeploymentEventMessage = "Power growing: deployed agent {}\n\tdescription: {}",
)
