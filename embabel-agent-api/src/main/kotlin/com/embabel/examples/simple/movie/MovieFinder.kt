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
package com.embabel.examples.simple.movie

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.TransformationPayload
import com.embabel.agent.api.common.createObject
import com.embabel.agent.config.models.OpenAiModels
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.all
import com.embabel.agent.core.hitl.ConfirmationRequest
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.library.RelevantNewsStories
import com.embabel.agent.domain.persistence.FindEntitiesRequest
import com.embabel.agent.domain.special.UserInput
import com.embabel.agent.domain.support.naturalLanguageRepository
import com.embabel.agent.event.ProgressUpdateEvent
import com.embabel.agent.experimental.prompt.Persona
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository

typealias OneThroughTen = Int

data class MovieBuff(
    override val name: String,
    val movieRatings: List<MovieRating>,
    val countryCode: String,
    val hobbies: List<String>,
    val about: String,
    val streamingServices: List<String>,
) : Person {

    /**
     * We use this so we don't overwhelm the prompt
     */
    fun randomRatings(n: Int): List<MovieRating> {
        return movieRatings.shuffled().take(n)
    }
}

data class MovieRating(
    val rating: OneThroughTen,
    val title: String,
)

data class DecoratedMovieBuff(
    val movieBuff: MovieBuff,
    val tasteProfile: String,
)

data class SuggestedMovieTitles(
    val titles: List<String>,
)

/**
 * Fleshed out with IMDB data
 */
data class SuggestedMovies(
    val movies: List<MovieResponse>,
)

data class StreamableMovies(
    val movies: List<StreamableMovie>,
)

/**
 * A movie that's available to our movie buff
 */
data class StreamableMovie(
    val movie: MovieResponse,
    val availableStreamingOptions: List<StreamingOption>,
    val allStreamingOptions: List<StreamingOption>,
)

data class SuggestionWriteup(
    override val text: String,
) : HasContent

interface MovieBuffRepository : CrudRepository<MovieBuff, String>

val Roger = Persona(
    name = "Roger",
    persona = "A creative movie critic who channels the famous movie critic Roger Ebert",
    voice = "You write like Roger Ebert",
    objective = """
            Suggest movies that will extend as well as entertain the user.
            Share the love of cinema and inspire the user to watch, learn and think.
            """.trimIndent(),
)

@ConfigurationProperties(prefix = "moviefinder")
data class MovieFinderConfig(
    val suggestionCount: Int = 5,
    val tasteProfileWordCount: Int = 40,
    val writeupWordCount: Int = 200,
    val suggesterPersona: Persona = Roger,
    val writerPersona: Persona = Roger,
)

@Profile("!test")
@Agent(
    description = "Find movies a person hasn't seen and may find interesting"
)
class MovieFinder(
    private val omdbClient: OmdbClient,
    private val streamingAvailabilityClient: StreamingAvailabilityClient,
    private val movieBuffRepository: MovieBuffRepository,
    private val config: MovieFinderConfig,
) {

    private val logger = LoggerFactory.getLogger(MovieFinder::class.java)

    private val llm = LlmOptions(OpenAiModels.GPT_4o_MINI)

    init {
        // TODO this is only for example purposes
        populateMovieBuffRepository(movieBuffRepository)
    }

    @Action(description = "Retrieve a MovieBuff based on the user input")
    fun findMovieBuff(userInput: UserInput, payload: OperationPayload): MovieBuff? {
        return movieBuffRepository.findAll().firstOrNull()

        // TODO Standard action helper that can bind
        val fer = movieBuffRepository.naturalLanguageRepository(payload).find(
            FindEntitiesRequest(description = userInput.content),
        )
        // TODO present a choices form
        return when {
            fer.matches.size == 1 -> {
                waitFor(
                    ConfirmationRequest(
                        fer.matches.single().match,
                        "Please confirm whether this is the movie buff you meant: ${fer.matches.single().match.name}",
                    )
                )
            }

            else -> {
                logger.info("Found {} movie buffs", fer.matches.size)
                null
            }
        }
    }

    @Action
    fun analyzeTasteProfile(
        movieBuff: MovieBuff,
        payload: TransformationPayload<*, DecoratedMovieBuff>,
    ): DecoratedMovieBuff {
        val tasteProfile = payload.promptRunner(llm) generateText
                """
            ${movieBuff.name} is a movie lover with hobbies of ${movieBuff.hobbies.joinToString(", ")}
            They have rated the following movies out of 10:
            ${
                    movieBuff.randomRatings(50).joinToString("\n") {
                        "${it.title}: ${it.rating}"
                    }
                }

            Return a summary of their taste profile as you understand it,
            in ${config.tasteProfileWordCount} words or less. Cover what they like and don't like.
            """.trimIndent()
        return DecoratedMovieBuff(
            movieBuff = movieBuff,
            tasteProfile = tasteProfile,
        )
    }

    @Action(toolGroups = [ToolGroup.WEB])
    fun findNewsStories(
        dmb: DecoratedMovieBuff,
        userInput: UserInput
    ): RelevantNewsStories =
        using(llm).createObject(
            """
            ${dmb.movieBuff.name} is a movie buff.
            Their hobbies are ${dmb.movieBuff.hobbies.joinToString(", ")}
            Their movie taste profile is ${dmb.tasteProfile}
            About them: "${dmb.movieBuff.about}"

            Consider the following specific request that may govern today's choice: '${userInput.content}'

            Given this, use web tools and generate search queries
            to find 5 relevant news stories that might inspire
            a movie choice for them tonight.
            Don't look for movie news but general news that might interest them.
            If possible, look for news specific to the specific request.
            Country: ${dmb.movieBuff.countryCode}
            Current date: ${userInput.timestamp.toString()}
        """.trimIndent()
        )


    @Action(
        post = ["haveEnoughMovies"],
        canRerun = true,
    )
    fun suggestMovies(
        userInput: UserInput,
        dmb: DecoratedMovieBuff,
        payload: TransformationPayload<*, SuggestedMovieTitles>,
    ): StreamableMovies {
        val suggestedMovieTitles = payload.promptRunner(
            llm.withTemperature(1.3),
            promptContributors = listOf(config.suggesterPersona),
        ).createObject<SuggestedMovieTitles>(
            """
            Suggest ${config.suggestionCount} movie titles that ${dmb.movieBuff.name} hasn't seen, but may find interesting.

            Consider the specific request: "${userInput.content}"

            Use the information about their preferences from below:
            Their movie taste: "${dmb.tasteProfile}"

            Their hobbies: ${dmb.movieBuff.hobbies.joinToString()}
            About them: "${dmb.movieBuff.about}"

            Don't include the following movies, which they've seen (rating attached):
            ${
                dmb.movieBuff.movieRatings
                    .sortedBy { it.title }
                    .joinToString("\n") {
                        "${it.title}: ${it.rating}"
                    }
            }
            Don't include these movies we've already suggested:
            ${excludedTitles(payload.processContext).joinToString("\n")}

            Consider also the following news stories for topical inspiration:
            """.trimIndent(),
        )
        // Be sure to bind the suggested movie titles to the blackboard
        payload += suggestedMovieTitles
        val suggestedMovies = lookUpMovies(suggestedMovieTitles)
        return streamableMovies(
            movieBuff = dmb.movieBuff,
            suggestedMovies = suggestedMovies
        )
    }

    private fun lookUpMovies(suggestedMovieTitles: SuggestedMovieTitles): SuggestedMovies {
        logger.info(
            "Resolving suggestedMovieTitles {}",
            suggestedMovieTitles.titles.joinToString(", ")
        )
        val movies = suggestedMovieTitles.titles.mapNotNull { title ->
            try {
                omdbClient.getMovieByTitle(title)
            } catch (e: Exception) {
                null
            }
        }
        return SuggestedMovies(movies)
    }

    private fun streamableMovies(
        movieBuff: MovieBuff,
        suggestedMovies: SuggestedMovies,
    ): StreamableMovies {
        val streamables = suggestedMovies.movies
            // Sometimes the LLM ignores being told not to
            // include movies the user has seen
            .filterNot {
                it.Title in movieBuff.movieRatings.map { it.title }
            }
            .mapNotNull { movie ->
                try {
                    val allStreamingOptions =
                        streamingAvailabilityClient.getShowStreamingIn(
                            imdb = movie.imdbID,
                            country = movieBuff.countryCode
                        )
                    val availableToUser = allStreamingOptions.filter {
                        (it.service.name.lowercase() in movieBuff.streamingServices.map { it.lowercase() })  //|| it.type == "free"
                    }
                    logger.debug(
                        "Movie {} available in [{}] on {}",
                        movie.Title,
                        movieBuff.countryCode,
                        allStreamingOptions.map { it.service.name }.sorted().joinToString(", ")
                    )
                    if (availableToUser.isNotEmpty()) {
                        StreamableMovie(
                            movie = movie,
                            allStreamingOptions = allStreamingOptions,
                            availableStreamingOptions = availableToUser,
                        )
                    } else {
                        logger.info(
                            "Movie {} not available to {} on any of their streaming services: {} - filtering it out",
                            movie.Title,
                            movieBuff.name,
                            movieBuff.streamingServices.sorted().joinToString(", ")
                        )
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        return StreamableMovies(streamables)
    }

    @Condition
    fun haveEnoughMovies(processContext: ProcessContext): Boolean =
        allStreamableMovies(processContext).size >= config.suggestionCount

    private fun allStreamableMovies(
        processContext: ProcessContext,
    ): List<StreamableMovie> {
        val streamableMovies = processContext.blackboard
            .all<StreamableMovies>()
            .flatMap { it.movies }
            .distinctBy { it.movie.imdbID }
        processContext.onProcessEvent(
            ProgressUpdateEvent(
                agentProcess = processContext.agentProcess,
                name = "Streamable movies",
                current = streamableMovies.size,
                total = config.suggestionCount,
            )
        )
        return streamableMovies
    }

    /**
     * Movies we've already accepted or even suggested
     */
    private fun excludedTitles(
        processContext: ProcessContext,
    ): List<String> {
        val excludes = (processContext.blackboard
            .all<SuggestedMovieTitles>()
            .flatMap { it.titles } + allStreamableMovies(processContext).map { it.movie.Title })
            .distinct()
            .sorted()
        return excludes
    }

    @Action(pre = ["haveEnoughMovies"])
    @AchievesGoal(description = "Recommend movies for a movie buff using what we know about them")
    fun writeUpSuggestions(
        dmb: DecoratedMovieBuff,
        streamableMovies: StreamableMovies,
        payload: TransformationPayload<*, SuggestionWriteup>,
    ): SuggestionWriteup {
        val text = payload.promptRunner(
            llm = llm,
            promptContributors = listOf(config.suggesterPersona)
        ) generateText
                """
                Write up a recommendation of ${config.suggestionCount} movies in ${config.writeupWordCount} words
                for ${dmb.movieBuff.name}
                based on the following information:
                Their hobbies are ${dmb.movieBuff.hobbies.joinToString(", ")}
                Their movie taste profile is ${dmb.tasteProfile}
                A bit about them: "${dmb.movieBuff.about}"

                The streamable movie recommendations are:
                ${
                    allStreamableMovies(payload.processContext).joinToString("\n\n") {
                        """
                        ${it.movie.Title} (${it.movie.Year}): ${it.movie.imdbID}
                        Director: ${it.movie.Director}
                        Actors: ${it.movie.Actors}
                        ${it.movie.Plot}
                        Streaming available to ${dmb.movieBuff.name} on ${it.availableStreamingOptions.joinToString(", ") { "${it.service.name} at ${it.link}" }}
                        """.trimIndent()
                    }
                }

                Format in Markdown and include links to the movies on IMDB and the streaming service link for each.
                """.trimIndent()
        return SuggestionWriteup(
            text = text,
        )
    }
}
