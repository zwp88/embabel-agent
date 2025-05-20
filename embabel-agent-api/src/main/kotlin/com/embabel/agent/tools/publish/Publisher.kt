package com.embabel.agent.tools.publish

data class PublishedLocation(
    val url: String,
)

data class PublicationResponse(
    val resources: List<PublishedLocation>,
)

data class FilePublication(
    val fileName: String,
    val content: String,
)

data class PublicationRequest(
    val publications: List<FilePublication>,
)

interface Publisher {

    /**
     * Publish resources at the given location
     */
    fun publish(request: PublicationRequest): PublicationResponse
}