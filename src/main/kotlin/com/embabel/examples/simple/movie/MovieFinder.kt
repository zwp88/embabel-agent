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

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Condition
import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.api.common.TransformationPayload
import com.embabel.agent.api.common.createObject
import com.embabel.agent.api.common.using
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.all
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.library.RelevantNewsStories
import com.embabel.agent.domain.special.UserInput
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
        // Take random n ratings
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
    val movies: List<String>,
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

data class StreamableMovie(
    val movie: MovieResponse,
    val availableStreamingOptions: List<StreamingOption>,
    val allStreamingOptions: List<StreamingOption>,
) {

    val unavailableStreamingOptions: List<StreamingOption> =
        allStreamingOptions.filter { it !in availableStreamingOptions }
}

data class SuggestionWriteup(
    override val text: String,
) : HasContent

interface MovieBuffRepository : CrudRepository<MovieBuff, String>

@ConfigurationProperties(prefix = "moviefinder")
data class MovieFinderConfig(
    val suggestionCount: Int = 5,
    val tasteProfileWordCount: Int = 40,
    val writeupWordCount: Int = 200,
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

    init {
        // TODO this is only for example purposes
        populateMovieBuffRepository(movieBuffRepository)
    }

    @Action(description = "Retrieve a MovieBuff based on the user input")
    fun findMovieBuff(userInput: UserInput): MovieBuff? {
//        return movieBuffRepository.findById(userInput.content).orElse(null)
        val buff = movieBuffRepository.findAll().first()
        return buff
    }

    @Action(toolGroups = [ToolGroup.WEB])
    fun findNewsStories(
        dmb: DecoratedMovieBuff,
        userInput: UserInput
    ): RelevantNewsStories =
        using(LlmOptions("gpt-4o")).createObject(
            """
            ${dmb.movieBuff.name} is a movie buff.
            Their hobbies are ${dmb.movieBuff.hobbies.joinToString(", ")}
            Their movie taste profile is ${dmb.tasteProfile}
            About them: "${dmb.movieBuff.about}"

            Consider the following specific request: '${userInput.content}'

            Given this, use web tools and generate search queries
            to find 5 relevant news stories that might inspire
            movie choice for them tonight.
        """.trimIndent()
        )

    @Action
    fun analyzeTasteProfile(
        movieBuff: MovieBuff
    ): DecoratedMovieBuff {
        val tasteProfile = using(LlmOptions("gpt-4o-mini")) generateText
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
            """
        return DecoratedMovieBuff(
            movieBuff = movieBuff,
            tasteProfile = tasteProfile,
        )
    }

    @Action(
        post = ["haveEnoughMovies"],
        canRerun = true,
    )
    fun suggestMovies(
        userInput: UserInput,
        dmb: DecoratedMovieBuff,
        relevantNewsStories: RelevantNewsStories,
        payload: TransformationPayload<*, SuggestedMovieTitles>,
    ): StreamableMovies {
        // We do this to break any potential LLM caching
        // as this will be run many times
        val randomPart = listOf(
            "be creative",
            "be inventive",
            "think different",
            "think like Roger Ebert",
            "think like an auteur",
            "think of things that are fun",
            "make it entertaining!"
        )
            .random()
        val suggestedMovieTitles = payload.promptRunner(
            LlmOptions(model = "gpt-4o"),
        ).createObject<SuggestedMovieTitles>(
            """
            Suggest ${config.suggestionCount} movies titles that ${dmb.movieBuff.name} hasn't seen, but may find interesting.

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
            ${allStreamableMovies(payload.processContext).joinToString("\n") { it.movie.Title }}

            Consider also the following news stories for topical inspiration:
            ${relevantNewsStories.items.joinToString("\n") { "- ${it.url}: ${it.summary}" }}
            $randomPart
            """
        )
        val suggestedMovies = lookUpMovies(suggestedMovieTitles)
        return streamableMovies(
            movieBuff = dmb.movieBuff,
            suggestedMovies = suggestedMovies
        )
    }

    private fun lookUpMovies(suggestedMovieTitles: SuggestedMovieTitles): SuggestedMovies {
        logger.info(
            "Resolving suggestedMovieTitles {}",
            suggestedMovieTitles.movies.joinToString(", ")
        )
        val movies = suggestedMovieTitles.movies.mapNotNull { title ->
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
                        it.service.name.lowercase() in movieBuff.streamingServices.map { it.lowercase() }
                    }
                    if (allStreamingOptions.isNotEmpty()) {
                        StreamableMovie(
                            movie = movie,
                            allStreamingOptions = allStreamingOptions,
                            availableStreamingOptions = availableToUser
                        )
                    } else {
                        logger.info("Movie {} not available on any streaming service: filtering it out", movie.Title)
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
        allStreamableMovies(processContext).size > 4

    private fun allStreamableMovies(
        processContext: ProcessContext,
    ): List<StreamableMovie> {
        val streamableMovies = processContext.blackboard
            .all<StreamableMovies>()
            .flatMap { it.movies }
            .distinctBy { it.movie.imdbID }
        logger.info(
            "Found {} streamable movies so far",
            streamableMovies.size
        )
        return streamableMovies
    }

    @Action(pre = ["haveEnoughMovies"])
    @AchievesGoal(description = "Recommend movies for a movie buff using what we know about them")
    fun writeUpSuggestions(
        dmb: DecoratedMovieBuff,
        streamableMovies: StreamableMovies,
    ): SuggestionWriteup =
        using(LlmOptions("gpt-4o")).createObject(
            """
            Write up a recommendation of ${config.suggestionCount} movies in ${config.writeupWordCount}
            for ${dmb.movieBuff.name}
            based on the following information:
            Their hobbies are ${dmb.movieBuff.hobbies.joinToString(", ")}
            Their movie taste profile is ${dmb.tasteProfile}
            A bit about them: "${dmb.movieBuff.about}"

            The movie recommendations are:
            ${
                streamableMovies.movies.joinToString("\n\n") {
                    """
                    ${it.movie.Title} (${it.movie.Year}): ${it.movie.imdbID}
                    Director: ${it.movie.Director}
                    Actors: ${it.movie.Actors}
                    ${it.movie.Plot}
                    Streaming available to ${dmb.movieBuff.name} on ${it.availableStreamingOptions.joinToString(", ") { it.service.name }}
                    Also available on ${it.unavailableStreamingOptions.joinToString(", ") { it.service.name }}
                    but ${dmb.movieBuff.name} doesn't have a subscription
                    """.trimIndent()
                }
            }
            Focus on streamable movies.

            Format in Markdown and include links to the movies on IMDB and the streaming services.
            """
        )
}
