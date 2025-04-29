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
package com.embabel.agent.domain.library

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Interface when an object has a single important text component.
 */
interface HasContent {

    val text: String
}

@JsonClassDescription("Internet resource")
data class InternetResource(
    @get:JsonPropertyDescription("url of the resource")
    val url: String,
    @get: JsonPropertyDescription("concise summary of the resource")
    val summary: String,
)

interface InternetResources {

    @get:JsonPropertyDescription("internet resources")
    val links: List<InternetResource>
}


interface Person {

    val name: String

}

data class PersonImpl(override val name: String) : Person


data class RelevantNewsStories(
    val items: List<NewsStory>
)

data class NewsStory(
    val url: String,
    val title: String,
    val summary: String,
)
