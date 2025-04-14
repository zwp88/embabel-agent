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

import com.embabel.agent.annotation.AchievesGoal
import com.embabel.agent.annotation.Action
import com.embabel.agent.annotation.support.PromptRunner
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.Person
import com.embabel.agent.domain.special.UserInput
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service


typealias OneThroughTen = Int

data class MovieBuff(
    override val name: String,
    val movieRatings: List<MovieRating>,
    val countryCode: String,
    val hobbies: List<String>,
) : Person

data class MovieRating(
    val rating: OneThroughTen,
    val movie: Movie,
)

data class Movie(
    val title: String,
    val imdb: String,
)

data class SuggestedMovieTitles(
    val movies: List<String>,
    val tasteProfile: String,
)

/**
 * Fleshed out with IMDB data
 */
data class SuggestedMovies(
    val movies: List<MovieResponse>,
)

data class Recommendation(
    override val text: String,
) : HasContent

interface MovieBuffRepository : CrudRepository<MovieBuff, String>

@Service
class InMemoryMovieBuffRepository : MovieBuffRepository,
    InMemoryRepository<MovieBuff>(
        idGetter = { it.name },
        idWither = { it, id -> it.copy(name = id) },
    )

fun populateMovieBuffRepository(
    movieBuffRepository: MovieBuffRepository,
) {
    movieBuffRepository.save(
        MovieBuff(
            name = "Rod",
            movieRatings = listOf(
                MovieRating(
                    movie = Movie(
                        title = "The Godfather",
                        imdb = "tt0068646",
                    ),
                    rating = 10,
                )
            ),
            hobbies = listOf("Travel", "Skiing", "Chess", "Hiking", "Reading"),
            countryCode = "AU",
        )
    )

}

//@Agent(
//    description = "Find movies a person hasn't seen and may find interesting"
//)
class MovieFinder(
    private val omdbClient: OmdbClient,
    private val movieBuffRepository: MovieBuffRepository,
    private val suggestionCount: Int = 5,
) {
    init {
        // TODO this is only for example purposes
        populateMovieBuffRepository(movieBuffRepository)
    }

    @Action(description = "Retrieve a MovieBuff based on the user input")
    fun findMovieBuff(userInput: UserInput): MovieBuff {
//        return movieBuffRepository.findById(userInput.content).orElse(null)
        val buff = movieBuffRepository.findAll().first()
        return buff
    }

    // TODO horoscope or other things to consider
    // Or things in the news?

    @Action
    fun suggestMovieTitles(
        userInput: UserInput,
        movieBuff: MovieBuff
    ): SuggestedMovieTitles =
        PromptRunner().createObject(
            """
            Suggest $suggestionCount movies titles that ${movieBuff.name} has not seen, but may find interesting.

            Consider the specific request: '$${userInput.content}'

            Use the information about their preferences from below:
            Consider their hobbies:
            ${movieBuff.hobbies.joinToString("\n- ")}
            They have rated the following movies out of 10:
            ${
                movieBuff.movieRatings.joinToString("\n") {
                    "${it.movie.title}: ${it.rating}"
                }
            }

            Also return their tasteProfile as you understand it,
            in 25 words of less.
            """
        )

    @Action
    fun lookUpMovies(suggestedMovieTitles: SuggestedMovieTitles): SuggestedMovies {
        val movies = suggestedMovieTitles.movies.mapNotNull { title ->
            try {
                omdbClient.getMovieByTitle(title)
            } catch (e: Exception) {
                null
            }
        }
        return SuggestedMovies(movies)
    }


    @Action
    @AchievesGoal(description = "Recommend movies for a movie buff using what we know about them")
    fun writeUpSuggestions(
        movieBuff: MovieBuff,
        suggestedMovies: SuggestedMovieTitles,
        suggestedMoviesWithDetails: SuggestedMovies,
    ): Recommendation =
        PromptRunner().createObject(
            """
            Write up a movie recommendation for ${movieBuff.name}
            based on the following information:
            Their hobbies are ${movieBuff.hobbies.joinToString(", ")}
            Their movie taste profile is ${suggestedMovies.tasteProfile}

            The recommendations are:
            ${
                suggestedMoviesWithDetails.movies.joinToString("\n") {
                    "" + it
                }
            }
            """
        )
}
