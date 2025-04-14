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

import com.embabel.agent.shell.MessageGeneratorPromptProvider
import com.embabel.common.util.RandomFromFileMessageGenerator
import com.embabel.common.util.bold
import com.embabel.common.util.color
import com.embabel.common.util.italic
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

fun kier(text: String) = "🧔🏼‍♂️ ${"Kier".bold()} ${text.italic().color(LumonColors.Membrane)}"

fun character(name: String, text: String): String {
    val namePart = if (name.isNotBlank()) {
        "${name.bold()}: "
    } else {
        ""
    }
    return "$namePart${text.italic().color(LumonColors.Membrane)}"
}

object LumonColors {
    const val Membrane: Int = 0xbeb780
    const val Green: Int = 0x7da17e

}

@Component
@Profile("severance")
class SeverancePromptProvider : MessageGeneratorPromptProvider(
    color = LumonColors.Membrane,
    messageGenerator = RandomFromFileMessageGenerator(
        url = "logging/severance.txt",
    )
)