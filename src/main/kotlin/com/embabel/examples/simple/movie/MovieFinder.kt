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

data class MovieBuff(
    override val name: String,
    val moviesSeen: List<String>,
    val countryCode: String,
) : Person

data class SuggestedMovie(
    val title: String,
    val imdb: String,
)

data class SuggestedMovies(
    val movies: List<SuggestedMovie>,
)

data class Recommendation(
    override val text: String,
) : HasContent


// TODO add agent...presently causing failures
//@Agent(
//    description = "Find movies a person hasn't seen and may find interesting"
//)

// TODO include streaming service based on country
class MovieFinder {

    @Action(description = "Retrieve a MovieBuff based on the user input")
    fun findMovieBuff(userInput: UserInput): MovieBuff {
        // Try to find a movie buff from the input
        // TODO what if we can't find it?
        // Do we have an abort?
        TODO()
    }

    // TODO horoscope or other things to consider
    // Or things in the news?

    @Action
    fun suggestMovies(movieBuff: MovieBuff): SuggestedMovies =
        PromptRunner().createObject(
            """
            Suggest movies that ${movieBuff.name} has not seen, but may find interesting.
            They have seen the following movies: ${movieBuff.moviesSeen}
            """
        )

    @Action
    @AchievesGoal(description = "Recommend movies for a movie buff using what we know about them")
    fun writeUpSuggestions(suggestedMovies: SuggestedMovies): Recommendation {
        TODO()
    }
}
