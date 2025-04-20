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
package com.embabel.examples.simple.horoscope

import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.dsl.promptTransformer
import com.embabel.agent.api.dsl.transformer
import com.embabel.agent.domain.special.UserInput
import org.springframework.beans.factory.annotation.Value

fun starFinderAgent(
    horoscopeService: HoroscopeService,
    @Value("\${star-news-finder.story.count:5}")
    storyCount: Int,
) = agent(
    name = "StarFinder",
    description = "Find news based on a person's star sign",
) {

    action {
        promptTransformer<UserInput, StarPerson>(
            name = "extractPerson",
        )
        { "Create a person from this user input, extracting their name and star sign: $it" }
    }

    action {
        transformer<StarPerson, Horoscope>(
            name = "retrieveHoroscope",
        ) { Horoscope(horoscopeService.dailyHoroscope(it.input.sign)) }
    }

//    action {
//        MultiTransformer<Horoscope>(
//            name = "retrieveHoroscope",
//        ) { Horoscope(horoscopeService.dailyHoroscope(it.input.sign)) }
//    }

    goal(
        name = "writeup",
        description = "Write an amusing writeup for the target person based on their horoscope and current news stories",
        satisfiedBy = Writeup::class,
    )

}
