package com.embabel.examples.dogfood

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

interface HoroscopeService {

    fun dailyHoroscope(sign: String): String
}

@Service
class RestClientHoroscopeService : HoroscopeService {

    private val restClient = RestClient.builder()
        .baseUrl("https://horoscope-app-api.vercel.app")
        .build()

     override fun dailyHoroscope(sign: String): String {
        val response = restClient.get()
            .uri("/api/v1/get-horoscope/daily?sign={sign}", sign.lowercase())
            .retrieve()
            .body(HoroscopeResponse::class.java)

        return response?.data?.horoscope_data
            ?: "Unable to retrieve horoscope for $sign today."
    }
}

private data class HoroscopeResponse(
    val success: Boolean,
    val status: Int,
    val data: HoroscopeData?
)

private data class HoroscopeData(
    val date: String,
    val horoscope_data: String
)