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
package com.embabel.agent.config.models

import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.OptionsConverter
import com.embabel.common.ai.model.PricingModel
import com.embabel.common.util.ExcludeFromJacocoGeneratedReport
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile(value = ["docker", "!test"])
@ExcludeFromJacocoGeneratedReport(reason = "Docker compose environment can't be unit tested")
class DockerOpenAiCompatibleModels(
    private val configurableBeanFactory: ConfigurableBeanFactory,
) {
    private val logger = LoggerFactory.getLogger(DockerOpenAiCompatibleModels::class.java)

    init {
        logger.info("Docker profile active: Looking for Docker-managed local models")
    }

    internal data class Model(val name: String, val url: String)

    internal fun findModelsFromEnvironment(env: Map<String, String>): List<Model> {
        return listOf()
    }

    @PostConstruct
    fun registerModelsFromEnvironment() {
        val models = findModelsFromEnvironment(System.getenv())
        models.forEach { model ->
            val llm = Llm(
                name = model.name,
                model = chatModelOf(model),
                optionsConverter = optionsConverter,
                knowledgeCutoffDate = LocalDate.of(2024, 8, 6),
                pricingModel = PricingModel.ALL_YOU_CAN_EAT,
            )
            logger.info(
                "Registering Docker-managed model {} with endpoint {}",
                model.name,
                model.url,
            )
            configurableBeanFactory.registerSingleton(model.name, llm)
        }
    }

    private fun chatModelOf(model: Model): ChatModel {
        return OpenAiChatModel.builder()
            .openAiApi(OpenAiApi.Builder().baseUrl(model.url).build())
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(model.name)
                    .build()
            ).build()
    }

    private val optionsConverter: OptionsConverter = { options ->
        OpenAiChatOptions.builder()
            .temperature(options.temperature)
            .build()
    }

}
