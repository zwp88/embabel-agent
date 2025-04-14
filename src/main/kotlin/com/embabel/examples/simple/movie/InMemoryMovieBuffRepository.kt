package com.embabel.examples.simple.movie

import com.embabel.common.util.RandomFromFileMessageGenerator
import org.springframework.stereotype.Service

@Service
class InMemoryMovieBuffRepository : MovieBuffRepository,
    InMemoryRepository<MovieBuff>(
        idGetter = { it.name },
        idWither = { it, id -> it.copy(name = id) },
    )

fun populateMovieBuffRepository(
    movieBuffRepository: MovieBuffRepository,
) {

    val ratings = parseRatings(
        RandomFromFileMessageGenerator("examples/movie/rod_ratings.tsv")
            .messages
    )


    movieBuffRepository.save(
        MovieBuff(
            name = "Rod",
            movieRatings = ratings,
            hobbies = listOf("Travel", "Skiing", "Chess", "Hiking", "Reading"),
            countryCode = "AU",
            about = """
                Rod is an Australian man who has a PhD in Musicology and
                has a career as a software engineer, author and tech entrepreneur.
                He is widely traveled and has lived in California and the UK
                before returning to Sydney.
            """.trimIndent()
        )
    )

}


fun parseRatings(inputs: List<String>): List<MovieRating> {
    return inputs
        .map { line ->
            // Split on multiple spaces and filter out empty strings
            val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            // Last element is the rating, everything else is the title
            val rating = parts.last().toInt()
            val title = parts.dropLast(1).joinToString(" ")

            MovieRating(title = title, rating = rating)
        }
}