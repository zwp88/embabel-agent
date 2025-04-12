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

import com.embabel.common.util.*
import org.jline.utils.AttributedString
import org.springframework.context.annotation.Profile
import org.springframework.shell.jline.PromptProvider
import org.springframework.stereotype.Service

fun kier(text: String) = "🧔🏼‍♂️ ${"Kier".bold()} ${text.italic().color(LumonColors.Membrane)}"

fun character(name: String, text: String) = "${name.bold()}: ${text.italic().color(LumonColors.Membrane)}"

object LumonColors {
    const val Membrane: Int = 0xbeb780
    const val Green: Int = 0x7da17e

}

@Service
@Profile("severance")
class SeverancePromptProvider(
    private val messageGenerator: MessageGenerator = RandomFromFileMessageGenerator("logging/severance.txt")
) : PromptProvider {

    override fun getPrompt(): AttributedString {
        val msg = messageGenerator.generate()
        val (character, text) = if (":" in msg) {
            msg.split(":", limit = 2).map { it.trim() }
        } else {
            listOf("", msg.trim())
        }
        return AttributedString(
            character(character, text).color(LumonColors.Membrane) + "\nLumon> ".color(LumonColors.Membrane),
//        AttributedStyle.DEFAULT.foregroundRgb(LumonMembrane)
        )
    }

}
