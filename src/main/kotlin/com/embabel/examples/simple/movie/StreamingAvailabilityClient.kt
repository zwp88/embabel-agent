package com.embabel.examples.simple.movie

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Client for the Streaming Availability API
 * See https://docs.movieofthenight.com/
 */
@Service
@ConditionalOnProperty("X_RAPIDAPI_KEY")
class StreamingAvailabilityClient(
    apiKey: String = System.getenv("X_RAPIDAPI_KEY"),
    private val baseUrl: String = "https://streaming-availability.p.rapidapi.com/"
) {

    val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Accept", "application/json")
        .defaultHeader("X-RapidAPI-Key", apiKey)
        .build()

    fun getShowStreamingIn(imdb: String, country: String): List<StreamingOption> {
        return getShow(imdb)
            .streamingOptions[country]?.toList() ?: emptyList()
    }

    fun getShow(imdb: String): ShowResponse {
        return restClient.get()
            .uri("shows/${imdb}")
            .retrieve()
            .toEntity(ShowResponse::class.java)
            .body!!
    }

    fun getCountries(): CountriesResponse {
        val responseType = object : ParameterizedTypeReference<Map<String, Country>>() {}

        return restClient.get()
            .uri("countries")
            .retrieve()
            .toEntity(responseType)
            .body!!
    }

    fun distinctStreamingServices(): List<StreamingService> {
        return getCountries().values
            .flatMap { it.services }
            .distinctBy { it.id }
    }

}

/**
 * Option for streaming a particular show
 * @param type subscription etc
 */
data class ServiceOption(
    val id: String,
    val name: String,
    val homePage: String,
    val url: String? = null,
    val link: String? = null,
    val type: String? = null,
    val quality: String? = null,
)

data class StreamingService(
    val id: String,
    val name: String,
    val homePage: String,
    val imageSet: Map<String, String> = emptyMap(),
)

data class Country(
    val countryCode: String,
    val services: List<StreamingService>,
)

typealias CountriesResponse = Map<String, Country>

data class StreamingOption(
    val service: ServiceOption,
    val type: String,
    val link: String,
    val quality: String? = null,
    val expiresSoon: Boolean = false,
)

data class ShowResponse(
    val streamingOptions: Map<String, Array<StreamingOption>>
)