package com.embabel.examples.dogfood

import com.embabel.agent.UserInput
import com.embabel.agent.annotation.AchievesGoal
import com.embabel.agent.annotation.Action
import com.embabel.agent.annotation.Agentic
import com.embabel.agent.annotation.support.PromptRunner
import com.embabel.agent.domain.HasContent


data class RelevantNewsStories(
    val items: List<NewsStory>
)

data class NewsStory(
    val url: String,

    val summary: String,
)

data class Subject(
    val name: String,
    val sign: String,
)

data class Horoscope(
    val summary: String,
)

data class FunnyWriteup(
    override val text: String,
) : HasContent

/**
 * Find news based on a person's star sign
 */
@Agentic
class StarNewsFinder(
    private val horoscopeService: HoroscopeService,
    private val storyCount: Int = 5,
) {

    @Action
    fun extractPerson(userInput: UserInput): Subject {
        return PromptRunner().run("Create a person from this user input, extracting their name and star sign: $userInput")
    }

    @Action
    fun retrieveHoroscope(subject: Subject): Horoscope {
        val horoscope = horoscopeService.dailyHoroscope(subject.sign)
        return Horoscope(horoscope)
    }

    @Action
    fun findNewsStories(person: Subject, horoscope: Horoscope): RelevantNewsStories {
        return PromptRunner().run(
            """
            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is: 
                <horoscope>${horoscope.summary}</horoscope>
            Given this, use web tools and generate search queries
            to find $storyCount relevant news stories summarize them in a few sentences.
            Include the URL for each story.
            Do not look for another horoscope reading or return results directly about astrology;
            find stories relevant to the reading above.
                  
            For example:
            - If the horoscope says that they may
            want to work on relationships, you could find news stories about
            novel gifts
            - If the horoscope says that they may want to work on their career,
            find news stories about training courses.
        """.trimIndent()
        )
    }

    @AchievesGoal(
        description = "Write an amusing writeup for the target person based on their horoscope and current news stories",
    )
    @Action
    fun writeup(
        person: Subject,
        relevantNewsStories: RelevantNewsStories,
        horoscope: Horoscope,
    ): FunnyWriteup =
        PromptRunner().withTemperature(1.2).run(
            """
            Take the following news stories and write up something
            amusing for the target person.
            
            Begin by summarizing their horoscope in a concise, amusing way, then
            talk about the news. End with a surprising signoff.
            
            ${person.name} is an astrology believer with the sign ${person.sign}.
            Their horoscope for today is: 
                <horoscope>${horoscope.summary}</horoscope>
            Relevant news stories are:
            ${relevantNewsStories.items.joinToString("\n") { "- ${it.url}: ${it.summary}" }}
            
            Format it as Markdown with links.
        """.trimIndent()
        )

}