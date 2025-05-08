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

import com.embabel.agent.event.logging.personality.ColorPalette
import com.embabel.agent.shell.MessageGeneratorPromptProvider
import com.embabel.common.util.RandomFromFileMessageGenerator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("montypython")
object MontyPythonColors : ColorPalette {
    // Holy Grail-inspired colors
    const val BRIGHT_RED = 0xE50000     // Knight who says Ni! red
    const val ROYAL_BLUE = 0x0038A8     // King Arthur's blue

    override val highlight: Int
        get() = BRIGHT_RED
    override val color2: Int
        get() = ROYAL_BLUE
}


@Component
@Profile("montypython")
class MontyPythonPromptProvider : MessageGeneratorPromptProvider(
    color = MontyPythonColors.BRIGHT_RED,
    prompt = "pythons",
    messageGenerator = RandomFromFileMessageGenerator(
        url = "logging/montypython.txt"
    ),
)
