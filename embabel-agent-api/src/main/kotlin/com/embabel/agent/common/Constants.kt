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
package com.embabel.agent.common

/**
 * Help get consistency in logging
 */
object LoggingConstants {

    const val BANNER_WIDTH = 100

    /**
     * A line separator beginning with the text
     */
    fun lineSeparator(text: String, bannerChar: String, glyph: String = " ⇩  "): String {
        if (text.isBlank()) {
            return bannerChar.repeat(BANNER_WIDTH)
        }
        return text + glyph + bannerChar.repeat(BANNER_WIDTH - text.length - glyph.length)
    }
}
