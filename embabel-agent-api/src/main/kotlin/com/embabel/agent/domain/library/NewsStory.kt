package com.embabel.agent.domain.library

data class RelevantNewsStories(
    val items: List<NewsStory>
)

data class NewsStory(
    val url: String,
    val title: String,
    val summary: String,
)