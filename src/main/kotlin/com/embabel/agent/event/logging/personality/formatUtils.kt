package com.embabel.agent.event.logging.personality

import com.embabel.common.util.bold
import com.embabel.common.util.color
import com.embabel.common.util.italic

/**
 * Format a saying of a character
 */
fun character(name: String, text: String, color: Int): String {
    val namePart = if (name.isNotBlank()) {
        "${name.bold()}: "
    } else {
        ""
    }
    return "$namePart${text.italic().color(color)}"
}