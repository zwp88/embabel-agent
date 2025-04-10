/*
                                * Copyright 2025 Embabel Software, Inc.
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

import com.embabel.common.util.bold
import com.embabel.common.util.color
import com.embabel.common.util.italic
import org.jline.utils.AttributedString
import org.springframework.context.annotation.Profile
import org.springframework.shell.jline.PromptProvider
import org.springframework.stereotype.Service

fun tyya(text: String) = "📙 ${"The You You Are".bold()} ${text.italic().color(LumonColors.Membrane)}"

fun kier(text: String) = "🧔🏼‍♂️ ${"Kier".bold()} ${text.italic().color(LumonColors.Membrane)}"

fun character(name: String, text: String) = "${name.bold()}: ${text.italic().color(LumonColors.Membrane)}"

val LumonQuotes = listOf(
    character("Ms Casey", "Your outie enjoys coding in Python"),
    character("Ms Casey", "Your outie loves YML"),
    character("Ms Casey", "Your outie once wrote a 1,000 line shell script"),
    character("Ms Casey", "Your outie revels in writing documentation"),
    kier("️May my cunning acument slice through the fog of small minds, guiding them to their great purpose in labor."),
    kier("Keep a merry humor ever in your heart."),
    kier(
        """
        Let not weakness live in your veins.
        Cherished workers, drown it inside you.
        Rise up from your deathbed and sally forth, more perfect for the struggle.
        """.trimIndent()
    ),
    kier("Render not my creation in miniature."),
    kier("Be content in my words, and dally not in the scholastic pursuits of lesser men."),
    kier("️No workplace shall be repurposed for slumber."),
    character("9", "I was blind till you gave me Vision"),
    character("9", "I was languid till you gave me Verve"),
    character("9", "I was simple till you gave me Wit"),
    character("9", "I was peevish till you gave me Cheer"),
    character("9", "I was in vain till you gave me Humility"),
    character("9", "I was cruel till you gave me Benevolence"),
    character("9", "I was gawkish till you gave me Nimbleness"),
    character("9", "I was false till you gave me Probity"),
    character("9", "I was dim till you gave me Wiles"),
    character("9", "I was Me till you gave me You"),
    "I see Kier in you",
    kier(
        """
        And I shall whisper to ye dutiful through the ages.
        In your noblest thoughts and epiphanies shall be my voice.
        You are my mouth, and through ye, I will whisper on when I am 10 centuries demised.
        """.trimIndent()
    ),
    kier(
        """
        Tame in me the tempers four that I may serve thee evermore.
        Place in me the values nine that I may feel thy touch divine.
        """.trimIndent()
    ),
    kier(
        """
        Endow in each swing of your ax or swipe of your pen the sum of your affections,
        that through me they may be purified and returned.
        No higher purpose may be found than this. Nor any... higher love.
        """.trimIndent()
    ),
    kier("The light of discovery shines truer upon a virgin meadow than a beaten path."),
    kier("Be ever merry."),
    tyya(
        """
       But surely beer and juleps cannot fill the void left by love.
       Indeed only wine can achieve this, but it is famously costly,
       which is why sadness is among the most recurrent issues facing the poor.
    """.trimIndent()
    ),
    tyya(
        "They cannot crucify you if your hand is in a fist."
    ),
    tyya(
        """
       A society with festering workers cannot flourish,
       just as a man with rotting toes cannot skip
    """.trimIndent()
    ),
    kier("Revel now in the fruit of your labors"),
    tyya(
        """
        What separates man from machine
        is that machines cannot think for themselves.
        Also they are made of metal, whereas man is made of skin.
    """.trimIndent()
    ),
    character("Irving", "You are sparing with the facial encouragements."),
    character(
        "Milchick",
        "Hey, I know this has been a tough quarter. I’m gonna see about rustling you up some special perks. That sound good?"
    ),
    character("Milchick", "Some of you may be quietly yearning to learn more."),
    character("Milchick", "Marshmallows are for team players."),
    character("Milchick", "There will be no formal valediction, catered or otherwise"),
    character("Milchick", "The Music Dance Experience is officially cancelled."),
    character("Milchick", "Devour feculence"),
    character(
        "Milchick",
        """
            Welcome. I'm agog at how well I can tell you're already fitting in.
            The office feels whole. Now, let's get this party started.
        """.trimIndent()
    ),
    "Praise Kier".bold(),
    character("Drummond", "Your attendance and urinalysis are both in the excellent range"),
    character("Drummond", "Anti-deflections will be heard after the lunch break."),
    kier("And all in Lumon’s care shall revel in the bounty of the incentives spur"),
    kier(
        """
       I have identified four components, which I call tempers, from which are derived every human soul.
       Woe. Frolic. Dread. Malice.
       Each man's character is defined by the precise ratio that resides in him.
       I walked into the cave of my own mind,
       and there I tamed them.
       Should you tame the tempers as I did mine, then the world shall become
       but your appendage.
       It is this great and consecrated power that
       I hope to pass on to all of you, my children.
       """
            .trimIndent()
    ),
    character("Dr. Mauer", "The efficacy test will begin shortly."),
    character("Drummond", "The medical team says your tempers will rebalance quickly."),
    character(
        "Drummond",
        "Mark Scout's completion of Cold Harbor will be remembered as one of the greatest moments in the history of this planet."
    ),
    character(
        "Drummond",
        "🐐 Has it verve?",
    ),
    character(
        "Drummond",
        "🐐 Has it wiles?",
    ),
    character(
        "Drummond",
        "🐐 We commit this animal to Kier, and his eternal war against pain.",
    ),
    character(
        "Drummond",
        """
            I think it's time to go back to the basics, Seth.
            To remember these severed workers' greater purpose, and to treat them as what they really are.
            """.trimIndent()
    ),
    character(
        "Drummond",
        """
            Welcome. Today, I will be conducting your monthly performance review.
            This review can take anywhere from two to six hours, depending on the number of atonements and approbations required.
            If the review is to take longer than four hours, there will be a break for lunch, with the order taken in advance.
        """.trimIndent()
    ),
    character("Harmony", "I welcome your contrition."),
    character(
        "Natalie",
        "The Board is jubilant at your ascendance. It wants you to feel appreciated and asks if you do."
    ),
    character(
        "Natalie",
        """
            I'm here tonight to tell you that we are on the verge of a revolution.
            A kind and empathetic revolution that puts the human being at the center of industry.
        """.trimIndent()
    ),
    character(
        "Natalie",
        """
            The Board austerely desires for you to feel connected to Lumon's history.
            To that end, please accept from the Board these inclusively re-canonicalized paintings
            intended to help you see yourself in Kier, our founder.
        """.trimIndent()
    )
)

object LumonColors {
    const val Membrane: Int = 0xbeb780
    const val Green: Int = 0x7da17e

}

@Service
@Profile("severance")
class SeverancePromptProvider : PromptProvider {
    override fun getPrompt() = AttributedString(
        LumonQuotes.random().color(LumonColors.Membrane) + "\nLumon> ".color(LumonColors.Membrane),
//        AttributedStyle.DEFAULT.foregroundRgb(LumonMembrane)
    )
}
